// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_filter_list_manager.h"

#include <utility>

#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_version.h"

namespace inweb::adblock {

namespace {

ListUpdateStatus Skipped(const std::string& reason) {
  ListUpdateStatus status;
  status.kind = ListUpdateKind::kSkipped;
  status.reason = reason;
  return status;
}

ListUpdateStatus Failed(const std::string& reason, bool served_from_cache) {
  ListUpdateStatus status;
  status.kind = ListUpdateKind::kFailed;
  status.reason = reason;
  status.served_from_cache = served_from_cache;
  return status;
}

ListUpdateStatus LoadedFromCache() {
  ListUpdateStatus status;
  status.kind = ListUpdateKind::kLoadedFromCache;
  return status;
}

}  // namespace

FilterListManager::FilterListManager(std::vector<FilterListSource> sources,
                                     FilterListFetcher* fetcher,
                                     FilterListCache* cache,
                                     UpdatePolicy policy)
    : sources_(std::move(sources)),
      fetcher_(fetcher),
      cache_(cache),
      policy_(policy) {}

FetchResult FilterListManager::FetchOrNull(
    const FilterListSource& source,
    const std::optional<FilterListMetadata>& metadata) {
  if (fetcher_ == nullptr || !source.download_url) {
    return FetchResult::Failure("no fetcher configured");
  }
  return fetcher_->Fetch(*source.download_url, metadata ? metadata->etag
                                                        : std::nullopt,
                         metadata ? metadata->last_modified : std::nullopt);
}

std::map<std::string, ListUpdateStatus> FilterListManager::Startup(
    int64_t now_ms) {
  last_status_.clear();
  for (const FilterListSource& source : sources_) {
    if (!source.enabled) {
      Record(source.id, Skipped("disabled"));
      continue;
    }
    std::optional<std::string> cached_body = cache_->LoadBody(source);
    if (cached_body) {
      FilterListMetadata metadata = cache_->LoadMetadata(source)
                                         .value_or(FilterListMetadata{
                                             source.id, /*downloaded_at_ms=*/0});
      metadata.source_id = source.id;
      if (VerifiedCachedCopy(*cached_body, metadata)) {
        Put(source, *cached_body, metadata);
        Record(source.id, LoadedFromCache());
      } else if (policy_.fetch_on_startup) {
        // G-07: the cached copy failed integrity verification — it is
        // never served; download a fresh one instead.
        const FetchResult result = FetchOrNull(source, std::nullopt);
        if (result.kind == FetchResult::Kind::kSuccess) {
          StoreAndRecord(source, result, now_ms);
        } else if (result.kind == FetchResult::Kind::kNotModified) {
          Record(source.id, Failed("not-modified without a verifiable "
                                   "cached copy",
                                   /*served_from_cache=*/false));
        } else {
          Record(source.id,
                 Failed("cached copy failed content verification; " +
                            result.reason,
                        /*served_from_cache=*/false));
        }
      } else {
        Record(source.id,
               Failed("cached copy failed content checksum verification",
                      /*served_from_cache=*/false));
      }
    } else if (policy_.fetch_on_startup) {
      const FetchResult result = FetchOrNull(source, std::nullopt);
      if (result.kind == FetchResult::Kind::kSuccess) {
        StoreAndRecord(source, result, now_ms);
      } else if (result.kind == FetchResult::Kind::kNotModified) {
        Record(source.id, Failed("not-modified without a cached copy",
                                 /*served_from_cache=*/false));
      } else {
        Record(source.id, Failed(result.reason, /*served_from_cache=*/false));
      }
    } else {
      Record(source.id, Skipped("no cached copy; startup fetch disabled"));
    }
  }
  return last_status_;
}

std::map<std::string, ListUpdateStatus> FilterListManager::Refresh(
    int64_t now_ms) {
  last_status_.clear();
  for (const FilterListSource& source : sources_) {
    if (!source.enabled) {
      Record(source.id, Skipped("disabled"));
      continue;
    }
    std::optional<FilterListMetadata> current_metadata;
    if (const auto it = lists_.find(source.id); it != lists_.end()) {
      current_metadata = it->second.metadata;
    } else {
      current_metadata = LoadFromCache(source);
    }
    if (!policy_.IsRefreshDue(current_metadata, now_ms)) {
      Record(source.id, Skipped("not due"));
      continue;
    }
    const FetchResult result = FetchOrNull(source, current_metadata);
    if (result.kind == FetchResult::Kind::kSuccess) {
      StoreAndRecord(source, result, now_ms);
    } else if (result.kind == FetchResult::Kind::kNotModified) {
      const auto it = lists_.find(source.id);
      if (it == lists_.end()) {
        Record(source.id, Failed("not-modified but no cached copy",
                                 /*served_from_cache=*/false));
      } else {
        ManagedList& cached = it->second;
        cached.metadata.downloaded_at_ms = now_ms;
        if (result.etag) {
          cached.metadata.etag = result.etag;
        }
        if (result.last_modified) {
          cached.metadata.last_modified = result.last_modified;
        }
        cache_->Store(cached.source, cached.body, cached.metadata);
        ListUpdateStatus status;
        status.kind = ListUpdateKind::kNotModified;
        status.version = cached.metadata.version;
        Record(source.id, std::move(status));
      }
    } else {
      const bool served = lists_.count(source.id) > 0 ||
                          cache_->LoadBody(source).has_value();
      Record(source.id, Failed(result.reason, served));
    }
  }
  return last_status_;
}

std::vector<ParsedFilterList> FilterListManager::TakeParsedLists() {
  std::vector<ParsedFilterList> lists;
  lists.reserve(lists_.size());
  for (auto& [id, managed] : lists_) {
    lists.push_back(std::move(managed.parsed));
  }
  return lists;
}

std::vector<FilterListManager::FilterListReport>
FilterListManager::list_reports() const {
  std::vector<FilterListReport> reports;
  reports.reserve(lists_.size());
  for (const auto& [id, managed] : lists_) {
    FilterListReport report;
    report.id = managed.source.id;
    report.rule_count = managed.rule_count;
    report.version = managed.metadata.version;
    report.last_checked_at_ms = managed.metadata.downloaded_at_ms;
    reports.push_back(std::move(report));
  }
  return reports;
}

int FilterListManager::total_network_rules() const {
  int total = 0;
  for (const auto& [id, managed] : lists_) {
    total += managed.rule_count;
  }
  return total;
}

std::optional<FilterListMetadata> FilterListManager::metadata_of(
    const std::string& source_id) const {
  const auto it = lists_.find(source_id);
  if (it == lists_.end()) {
    return std::nullopt;
  }
  return it->second.metadata;
}

void FilterListManager::StoreAndRecord(const FilterListSource& source,
                                       const FetchResult& result,
                                       int64_t now_ms) {
  ParsedFilterList parsed = FilterListParser::Parse(result.body, source.id);
  FilterListMetadata metadata;
  metadata.source_id = source.id;
  metadata.downloaded_at_ms = now_ms;
  metadata.etag = result.etag;
  metadata.last_modified = result.last_modified;
  metadata.version = ParseFilterListVersion(result.body).version;
  metadata.rule_count = static_cast<int>(parsed.network_rules.size());
  metadata.content_sha256 = Sha256Hex(result.body);
  cache_->Store(source, result.body, metadata);
  Put(source, result.body, metadata);
  ListUpdateStatus status;
  status.kind = ListUpdateKind::kUpdated;
  status.version = metadata.version;
  status.rule_count = metadata.rule_count;
  Record(source.id, std::move(status));
}

void FilterListManager::Put(const FilterListSource& source,
                            const std::string& body,
                            const FilterListMetadata& metadata) {
  ManagedList managed;
  managed.source = source;
  managed.body = body;
  managed.metadata = metadata;
  managed.parsed = FilterListParser::Parse(body, source.id);
  managed.rule_count = static_cast<int>(managed.parsed.network_rules.size());
  lists_[source.id] = std::move(managed);
}

std::optional<FilterListMetadata> FilterListManager::LoadFromCache(
    const FilterListSource& source) {
  std::optional<std::string> body = cache_->LoadBody(source);
  if (!body) {
    return std::nullopt;
  }
  FilterListMetadata metadata = cache_->LoadMetadata(source)
                                     .value_or(FilterListMetadata{
                                         source.id, /*downloaded_at_ms=*/0});
  metadata.source_id = source.id;
  if (!VerifiedCachedCopy(*body, metadata)) {
    return std::nullopt;
  }
  Put(source, *body, metadata);
  return metadata;
}

bool FilterListManager::VerifiedCachedCopy(
    const std::string& body,
    const FilterListMetadata& metadata) const {
  return !metadata.content_sha256 ||
         Sha256Hex(body) == *metadata.content_sha256;
}

void FilterListManager::Record(const std::string& source_id,
                               ListUpdateStatus status) {
  last_status_[source_id] = std::move(status);
}

}  // namespace inweb::adblock
