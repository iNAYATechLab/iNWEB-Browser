// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"

#include <cstdio>
#include <utility>

#include "base/files/file_enumerator.h"
#include "base/files/file_util.h"
#include "base/strings/string_number_conversions.h"
#include "base/strings/string_split.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"

namespace inweb::adblock {

namespace {

constexpr char kMetaHeader[] = "iNWEB-FILTERLIST-META v=1";

void AppendField(std::string* out, const char* key, const std::string& value) {
  out->append(key);
  out->push_back('\t');
  out->append(value);
  out->push_back('\n');
}

void AppendField(std::string* out, const char* key,
                 const std::optional<std::string>& value) {
  if (value) {
    AppendField(out, key, *value);
  }
}

}  // namespace

std::string FilterListMetadata::Serialize() const {
  std::string out;
  out.append(kMetaHeader).push_back('\n');
  AppendField(&out, "sourceId", source_id);
  AppendField(&out, "downloadedAt", base::NumberToString(downloaded_at_ms));
  AppendField(&out, "etag", etag);
  AppendField(&out, "lastModified", last_modified);
  AppendField(&out, "version", version);
  AppendField(&out, "ruleCount", base::NumberToString(rule_count));
  AppendField(&out, "contentSha256", content_sha256);
  return out;
}

// static
std::optional<FilterListMetadata> FilterListMetadata::Deserialize(
    const std::string& text) {
  const std::vector<std::string> lines =
      base::SplitString(text, "\n", base::KEEP_WHITESPACE,
                        base::SPLIT_WANT_ALL);
  if (lines.empty() || lines[0] != kMetaHeader) {
    return std::nullopt;
  }
  std::map<std::string, std::string> fields;
  for (size_t i = 1; i < lines.size(); ++i) {
    const std::string& line = lines[i];
    if (line.empty()) {
      continue;
    }
    const size_t separator = line.find('\t');
    if (separator == std::string::npos || separator == 0) {
      return std::nullopt;
    }
    fields[line.substr(0, separator)] = line.substr(separator + 1);
  }
  FilterListMetadata metadata;
  const auto source_id = fields.find("sourceId");
  if (source_id == fields.end() || source_id->second.empty()) {
    return std::nullopt;
  }
  metadata.source_id = source_id->second;
  const auto downloaded_at = fields.find("downloadedAt");
  int64_t downloaded_at_ms = 0;
  if (downloaded_at == fields.end() ||
      !base::StringToInt64(downloaded_at->second, &downloaded_at_ms)) {
    return std::nullopt;
  }
  metadata.downloaded_at_ms = downloaded_at_ms;
  const auto read_optional = [&fields](const char* key,
                                       std::optional<std::string>* out) {
    const auto it = fields.find(key);
    if (it != fields.end()) {
      *out = it->second;
    }
  };
  read_optional("etag", &metadata.etag);
  read_optional("lastModified", &metadata.last_modified);
  read_optional("version", &metadata.version);
  read_optional("contentSha256", &metadata.content_sha256);
  const auto rule_count = fields.find("ruleCount");
  int rule_count_value = 0;
  if (rule_count != fields.end()) {
    if (!base::StringToInt(rule_count->second, &rule_count_value)) {
      rule_count_value = 0;
    }
  }
  metadata.rule_count = rule_count_value;
  return metadata;
}

void MemoryFilterListCache::Store(const FilterListSource& source,
                                  const std::string& body,
                                  const FilterListMetadata& metadata) {
  bodies_[source.id] = body;
  metadata_[source.id] = metadata;
}

std::optional<std::string> MemoryFilterListCache::LoadBody(
    const FilterListSource& source) {
  const auto it = bodies_.find(source.id);
  if (it == bodies_.end()) {
    return std::nullopt;
  }
  return it->second;
}

std::optional<FilterListMetadata> MemoryFilterListCache::LoadMetadata(
    const FilterListSource& source) {
  const auto it = metadata_.find(source.id);
  if (it == metadata_.end()) {
    return std::nullopt;
  }
  return it->second;
}

void MemoryFilterListCache::Remove(const FilterListSource& source) {
  bodies_.erase(source.id);
  metadata_.erase(source.id);
}

void MemoryFilterListCache::Clear() {
  bodies_.clear();
  metadata_.clear();
}

FileFilterListCache::FileFilterListCache(base::FilePath directory)
    : directory_(std::move(directory)) {
  base::CreateDirectory(directory_);
}

base::FilePath FileFilterListCache::BodyPath(const std::string& id) const {
  return directory_.Append(id + ".txt");
}

base::FilePath FileFilterListCache::MetaPath(const std::string& id) const {
  return directory_.Append(id + ".meta");
}

void FileFilterListCache::Store(const FilterListSource& source,
                                const std::string& body,
                                const FilterListMetadata& metadata) {
  WriteAtomically(BodyPath(source.id), body);
  WriteAtomically(MetaPath(source.id), metadata.Serialize());
}

std::optional<std::string> FileFilterListCache::LoadBody(
    const FilterListSource& source) {
  std::string body;
  if (!base::ReadFileToString(BodyPath(source.id), &body)) {
    return std::nullopt;
  }
  return body;
}

std::optional<FilterListMetadata> FileFilterListCache::LoadMetadata(
    const FilterListSource& source) {
  std::string text;
  if (!base::ReadFileToString(MetaPath(source.id), &text)) {
    return std::nullopt;
  }
  return FilterListMetadata::Deserialize(text);
}

void FileFilterListCache::Remove(const FilterListSource& source) {
  base::DeleteFile(BodyPath(source.id));
  base::DeleteFile(MetaPath(source.id));
}

void FileFilterListCache::Clear() {
  base::FileEnumerator entries(directory_, /*recursive=*/false,
                                base::FileEnumerator::FILES);
  for (base::FilePath entry = entries.Next(); !entry.empty();
       entry = entries.Next()) {
    base::DeleteFile(entry);
  }
}

// static
void FileFilterListCache::WriteAtomically(const base::FilePath& target,
                                          const std::string& content) {
  const base::FilePath temp =
      target.InsertBeforeExtension(FILE_PATH_LITERAL(".tmp"));
  if (!base::WriteFile(temp, content)) {
    return;
  }
  if (base::PathExists(target)) {
    base::DeleteFile(target);
  }
  base::ReplaceFile(temp, target, /*error=*/nullptr);
}


// Out-of-line ctor/dtor definitions (chromium-style fallout fix).
FilterListMetadata::FilterListMetadata() = default;
FilterListMetadata::FilterListMetadata(std::string source_id,
                                       int64_t downloaded_at_ms)
    : source_id(std::move(source_id)), downloaded_at_ms(downloaded_at_ms) {}
FilterListMetadata::~FilterListMetadata() = default;

MemoryFilterListCache::MemoryFilterListCache() = default;
MemoryFilterListCache::~MemoryFilterListCache() = default;


FilterListMetadata::FilterListMetadata(FilterListMetadata&&) = default;
FilterListMetadata& FilterListMetadata::operator=(FilterListMetadata&&) = default;


FilterListMetadata::FilterListMetadata(const FilterListMetadata&) = default;
FilterListMetadata& FilterListMetadata::operator=(const FilterListMetadata&) = default;

}  // namespace inweb::adblock
