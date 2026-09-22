// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — FilterListManager unit tests: startup cache-first
// behavior, G-07 integrity pinning, conditional refresh, graceful
// degradation. Ported from the Kotlin reference
// (FilterListManagerTest) with a scriptable fake fetcher.

#include "chrome/android/inweb/adblock/inweb_filter_list_manager.h"

#include <deque>
#include <optional>
#include <string>
#include <vector>

#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

class FakeFetcher : public FilterListFetcher {
 public:
  struct Call {
    std::string url;
    std::optional<std::string> etag;
    std::optional<std::string> last_modified;
  };

  FetchResult Fetch(const std::string& url,
                    const std::optional<std::string>& etag,
                    const std::optional<std::string>& last_modified) override {
    calls.push_back(Call{url, etag, last_modified});
    if (script.empty()) {
      return FetchResult::Failure("script exhausted");
    }
    const FetchResult result = std::move(script.front());
    script.pop_front();
    return result;
  }

  std::vector<Call> calls;
  std::deque<FetchResult> script;
};

FilterListSource Source(bool enabled = true) {
  return *FilterListSource::Create("easylist", "EasyList",
                                   std::string("https://example.com/l.txt"),
                                   enabled);
}

constexpr char kBodyV1[] = "[Adblock Plus 2.0]\n! Version: 1\n||ads.example.com^\n";
constexpr char kBodyV2[] = "[Adblock Plus 2.0]\n! Version: 2\n||ads.example.com^\n||tracker.net^$script\n";

FilterListMetadata MetadataFor(const std::string& body, int64_t at) {
  FilterListMetadata metadata;
  metadata.source_id = "easylist";
  metadata.downloaded_at_ms = at;
  metadata.etag = "\"v1\"";
  metadata.version = "1";
  metadata.rule_count = 1;
  metadata.content_sha256 = Sha256Hex(body);
  return metadata;
}

}  // namespace

TEST(InwebFilterListManagerTest, StartupFetchesWhenNoCache) {
  FakeFetcher fetcher;
  fetcher.script.push_back(FetchResult::Success(
      kBodyV1, std::string("\"v1\""), std::string("Mon, 21 Sep 2026")));
  MemoryFilterListCache cache;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);

  const auto status = manager.Startup(1000);
  ASSERT_EQ(1u, status.count("easylist"));
  EXPECT_EQ(ListUpdateKind::kUpdated, status.at("easylist").kind);
  EXPECT_EQ(1, status.at("easylist").rule_count);
  ASSERT_TRUE(status.at("easylist").version.has_value());
  EXPECT_EQ("1", *status.at("easylist").version);
  ASSERT_EQ(1u, fetcher.calls.size());
  EXPECT_EQ("https://example.com/l.txt", fetcher.calls[0].url);
  EXPECT_FALSE(fetcher.calls[0].etag.has_value());

  EXPECT_EQ(1, manager.total_network_rules());
  const std::optional<FilterListMetadata> metadata =
      manager.metadata_of("easylist");
  ASSERT_TRUE(metadata.has_value());
  EXPECT_EQ(1000, metadata->downloaded_at_ms);
  ASSERT_TRUE(metadata->content_sha256.has_value());
  EXPECT_EQ(Sha256Hex(kBodyV1), *metadata->content_sha256);
  // Cached for the next start.
  EXPECT_EQ(kBodyV1, cache.LoadBody(Source()).value_or(""));
}

TEST(InwebFilterListManagerTest, StartupServesVerifiedCacheWithoutNetwork) {
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, MetadataFor(kBodyV1, 500));
  FakeFetcher fetcher;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);

  const auto status = manager.Startup(1000);
  EXPECT_EQ(ListUpdateKind::kLoadedFromCache, status.at("easylist").kind);
  EXPECT_TRUE(fetcher.calls.empty());
  EXPECT_EQ(1, manager.total_network_rules());
}

TEST(InwebFilterListManagerTest, TamperedCacheIsNeverServed) {
  // G-07: pinned checksum does not match the body.
  FilterListMetadata tampered = MetadataFor(kBodyV1, 500);
  tampered.content_sha256 = Sha256Hex("a different body");
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, tampered);
  FakeFetcher fetcher;
  fetcher.script.push_back(FetchResult::Success(kBodyV2, std::nullopt, std::nullopt));
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);

  const auto status = manager.Startup(1000);
  EXPECT_EQ(ListUpdateKind::kUpdated, status.at("easylist").kind);
  EXPECT_EQ(2, manager.total_network_rules());
  EXPECT_EQ(kBodyV2, cache.LoadBody(Source()).value_or(""));
}

TEST(InwebFilterListManagerTest, TamperedCacheWithNoFetchFailsHonestly) {
  FilterListMetadata tampered = MetadataFor(kBodyV1, 500);
  tampered.content_sha256 = Sha256Hex("a different body");
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, tampered);
  UpdatePolicy policy;
  policy.fetch_on_startup = false;
  FilterListManager manager(std::vector<FilterListSource>{Source()},
                            /*fetcher=*/nullptr, &cache, policy);

  const auto status = manager.Startup(1000);
  EXPECT_EQ(ListUpdateKind::kFailed, status.at("easylist").kind);
  EXPECT_FALSE(status.at("easylist").served_from_cache);
  EXPECT_EQ(0, manager.total_network_rules());
}

TEST(InwebFilterListManagerTest, NullFetcherFailsHonestlyOnStartup) {
  MemoryFilterListCache cache;
  FilterListManager manager(std::vector<FilterListSource>{Source()},
                            /*fetcher=*/nullptr, &cache);
  const auto status = manager.Startup(1000);
  EXPECT_EQ(ListUpdateKind::kFailed, status.at("easylist").kind);
  EXPECT_FALSE(status.at("easylist").served_from_cache);
  EXPECT_EQ(0, manager.total_network_rules());
}

TEST(InwebFilterListManagerTest, RefreshNotModifiedUpdatesTimestamp) {
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, MetadataFor(kBodyV1, 500));
  FakeFetcher fetcher;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);
  manager.Startup(1000);

  fetcher.script.push_back(FetchResult::NotModified(std::string("\"v2\""), std::nullopt));
  const auto status = manager.Refresh(90'000'000);
  EXPECT_EQ(ListUpdateKind::kNotModified, status.at("easylist").kind);
  ASSERT_EQ(1u, fetcher.calls.size());
  // Conditional validators were sent.
  ASSERT_TRUE(fetcher.calls[0].etag.has_value());
  EXPECT_EQ("\"v1\"", *fetcher.calls[0].etag);
  const std::optional<FilterListMetadata> metadata =
      manager.metadata_of("easylist");
  ASSERT_TRUE(metadata.has_value());
  EXPECT_EQ(90'000'000, metadata->downloaded_at_ms);
  ASSERT_TRUE(metadata->etag.has_value());
  EXPECT_EQ("\"v2\"", *metadata->etag);
  EXPECT_EQ(1, manager.total_network_rules());
}

TEST(InwebFilterListManagerTest, RefreshFailureKeepsCachedContent) {
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, MetadataFor(kBodyV1, 500));
  FakeFetcher fetcher;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);
  manager.Startup(1000);

  fetcher.script.push_back(FetchResult::Failure("HTTP 500"));
  const auto status = manager.Refresh(90'000'000);
  ASSERT_EQ(1u, status.count("easylist"));
  EXPECT_EQ(ListUpdateKind::kFailed, status.at("easylist").kind);
  EXPECT_TRUE(status.at("easylist").served_from_cache);
  EXPECT_EQ(1, manager.total_network_rules());
}

TEST(InwebFilterListManagerTest, RefreshSkippedWhenNotDueOrDisabled) {
  MemoryFilterListCache cache;
  cache.Store(Source(), kBodyV1, MetadataFor(kBodyV1, 90'000'000));
  FakeFetcher fetcher;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);
  manager.Startup(90'000'000);

  const auto status = manager.Refresh(90'000'000 + 1000);
  EXPECT_EQ(ListUpdateKind::kSkipped, status.at("easylist").kind);
  EXPECT_EQ("not due", status.at("easylist").reason);
  EXPECT_TRUE(fetcher.calls.empty());
}

TEST(InwebFilterListManagerTest, DisabledSourceIsSkipped) {
  MemoryFilterListCache cache;
  FakeFetcher fetcher;
  FilterListManager manager(std::vector<FilterListSource>{Source(false)},
                            &fetcher, &cache);
  const auto status = manager.Startup(1000);
  EXPECT_EQ(ListUpdateKind::kSkipped, status.at("easylist").kind);
  EXPECT_EQ("disabled", status.at("easylist").reason);
  EXPECT_TRUE(fetcher.calls.empty());
}

TEST(InwebFilterListManagerTest, TakeParsedListsHandsOffRulesOnce) {
  FakeFetcher fetcher;
  fetcher.script.push_back(FetchResult::Success(kBodyV2, std::nullopt, std::nullopt));
  MemoryFilterListCache cache;
  FilterListManager manager(std::vector<FilterListSource>{Source()}, &fetcher,
                            &cache);
  manager.Startup(1000);

  std::vector<ParsedFilterList> lists = manager.TakeParsedLists();
  ASSERT_EQ(1u, lists.size());
  EXPECT_EQ(2u, lists[0].network_rules.size());
  EXPECT_EQ("easylist", lists[0].list_id);

  // Reporting facts survive the handoff.
  EXPECT_EQ(2, manager.total_network_rules());
  const auto reports = manager.list_reports();
  ASSERT_EQ(1u, reports.size());
  EXPECT_EQ("easylist", reports[0].id);
  EXPECT_EQ(2, reports[0].rule_count);
  ASSERT_TRUE(reports[0].version.has_value());
  EXPECT_EQ("2", *reports[0].version);
  EXPECT_EQ(1000, reports[0].last_checked_at_ms.value_or(-1));
}

}  // namespace inweb::adblock
