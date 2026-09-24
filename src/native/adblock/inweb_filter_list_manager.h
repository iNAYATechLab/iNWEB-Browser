// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_MANAGER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_MANAGER_H_

#include <map>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_fetcher.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"
#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_update_policy.h"

namespace inweb::adblock {

// Per-source outcome of a startup load or refresh pass. Ported from the
// Kotlin reference (lists/FilterListManager.kt).
enum class ListUpdateKind {
  kUpdated,          // Downloaded a new body — parsed and cached.
  kNotModified,      // Server confirmed the cached copy is current (304).
  kFailed,           // Download failed; |served_from_cache| says whether
                     // the engine kept content.
  kSkipped,          // Not attempted — disabled, or refresh not due.
  kLoadedFromCache,  // Startup served the verified cached copy.
};

struct ListUpdateStatus {
  ListUpdateStatus();
  ~ListUpdateStatus();
  ListUpdateStatus(const ListUpdateStatus&);
  ListUpdateStatus& operator=(const ListUpdateStatus&);
  ListUpdateStatus(ListUpdateStatus&&);
  ListUpdateStatus& operator=(ListUpdateStatus&&);

  ListUpdateKind kind = ListUpdateKind::kSkipped;
  std::optional<std::string> version;  // kUpdated / kNotModified
  int rule_count = 0;                  // kUpdated
  std::string reason;                  // kFailed / kSkipped
  bool served_from_cache = false;      // kFailed
};

// Orchestrates the filter-list lifecycle: startup (cache first, download
// only what is missing), conditional refresh (ETag / Last-Modified → 304),
// and graceful degradation — a failed refresh always keeps the cached
// copy. Ported from the Kotlin reference; performs no background
// scheduling itself (UpdatePolicy is the decision, the host layer runs
// the timer).
//
// Content integrity (G-07): every downloaded body is pinned with its
// SHA-256 in the cache metadata and re-verified on every cache load; a
// mismatching copy is NEVER served — the manager re-downloads (when
// allowed) or reports an honest failure with nothing loaded.
class FilterListManager {
 public:
  // |fetcher| may be null: v1 runs with the embedded default list and no
  // subscription transport (ADR-041); a null fetcher turns any required
  // download into an honest kFailed without touching the network.
  FilterListManager(std::vector<FilterListSource> sources,
                    FilterListFetcher* fetcher,
                    FilterListCache* cache,
                    UpdatePolicy policy = UpdatePolicy());
  ~FilterListManager();
  FilterListManager(const FilterListManager&) = delete;
  FilterListManager& operator=(const FilterListManager&) = delete;
  FilterListManager(FilterListManager&&) = delete;
  FilterListManager& operator=(FilterListManager&&) = delete;

  // Initial load: serve every enabled source from cache; download only
  // the ones with no cached copy (when UpdatePolicy::fetch_on_startup
  // allows).
  std::map<std::string, ListUpdateStatus> Startup(int64_t now_ms);

  // Refresh pass for all enabled sources whose content is due.
  // Conditional validators make unchanged lists a cheap 304; failures
  // never evict the cached copy.
  std::map<std::string, ListUpdateStatus> Refresh(int64_t now_ms);

  // Moves the parsed lists out — the one-shot handoff that builds a
  // TrackingProtectionEngine. ParsedFilterList is move-only (compiled
  // regexes); after this call the manager keeps bodies, metadata and rule
  // counts for reporting, but no longer holds parsed rules.
  std::vector<ParsedFilterList> TakeParsedLists();

  // Per-list reporting facts for the Security Center (§24). Rule counts
  // are the real counts from the parse that produced each list (stable
  // across TakeParsedLists()).
  struct FilterListReport {
    std::string id;
    int rule_count = 0;
    std::optional<std::string> version;
    std::optional<int64_t> last_checked_at_ms;
  };
  std::vector<FilterListReport> list_reports() const;

  int total_network_rules() const;

  std::optional<FilterListMetadata> metadata_of(
      const std::string& source_id) const;

  std::map<std::string, ListUpdateStatus> last_report() const {
    return last_status_;
  }

 private:
  struct ManagedList {
    FilterListSource source;
    std::string body;
    FilterListMetadata metadata;
    // Parsed rules; moved out by TakeParsedLists(). |rule_count| keeps
    // the real count for reporting afterwards.
    ParsedFilterList parsed;
    int rule_count = 0;
  };

  void StoreAndRecord(const FilterListSource& source,
                      const FetchResult& result,
                      int64_t now_ms);
  void Put(const FilterListSource& source,
           const std::string& body,
           const FilterListMetadata& metadata);
  std::optional<FilterListMetadata> LoadFromCache(
      const FilterListSource& source);
  FetchResult FetchOrNull(const FilterListSource& source,
                          const std::optional<FilterListMetadata>& metadata);
  bool VerifiedCachedCopy(const std::string& body,
                          const FilterListMetadata& metadata) const;
  void Record(const std::string& source_id, ListUpdateStatus status);

  std::vector<FilterListSource> sources_;
  FilterListFetcher* fetcher_ = nullptr;
  FilterListCache* cache_ = nullptr;
  UpdatePolicy policy_;
  std::map<std::string, ManagedList> lists_;
  std::map<std::string, ListUpdateStatus> last_status_;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_MANAGER_H_
