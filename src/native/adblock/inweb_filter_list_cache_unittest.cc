// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter-list cache unit tests: metadata wire format and
// the file-backed cache. Ported from the Kotlin reference
// (FileFilterListCacheTest / InMemoryFilterListCacheTest).

#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"

#include <optional>
#include <string>

#include "base/files/scoped_temp_dir.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

FilterListSource TestSource() {
  return *FilterListSource::Create("test-list", "Test List",
                                   std::string("https://example.com/l.txt"));
}

FilterListMetadata SampleMetadata() {
  FilterListMetadata metadata;
  metadata.source_id = "test-list";
  metadata.downloaded_at_ms = 1234567890;
  metadata.etag = "\"v1\"";
  metadata.last_modified = "Mon, 21 Sep 2026 10:00:00 GMT";
  metadata.version = "1.2.3";
  metadata.rule_count = 42;
  metadata.content_sha256 = std::string(64, 'a');
  return metadata;
}

}  // namespace

TEST(InwebFilterListMetadataTest, SerializeDeserializeRoundTrip) {
  const FilterListMetadata original = SampleMetadata();
  const std::optional<FilterListMetadata> parsed =
      FilterListMetadata::Deserialize(original.Serialize());
  ASSERT_TRUE(parsed.has_value());
  EXPECT_EQ(original.source_id, parsed->source_id);
  EXPECT_EQ(original.downloaded_at_ms, parsed->downloaded_at_ms);
  EXPECT_EQ(original.etag, parsed->etag);
  EXPECT_EQ(original.last_modified, parsed->last_modified);
  EXPECT_EQ(original.version, parsed->version);
  EXPECT_EQ(original.rule_count, parsed->rule_count);
  EXPECT_EQ(original.content_sha256, parsed->content_sha256);
}

TEST(InwebFilterListMetadataTest, MinimalFieldsRoundTrip) {
  FilterListMetadata minimal;
  minimal.source_id = "x";
  minimal.downloaded_at_ms = 7;
  const std::optional<FilterListMetadata> parsed =
      FilterListMetadata::Deserialize(minimal.Serialize());
  ASSERT_TRUE(parsed.has_value());
  EXPECT_FALSE(parsed->etag.has_value());
  EXPECT_EQ(0, parsed->rule_count);
}

TEST(InwebFilterListMetadataTest, CorruptionYieldsNullopt) {
  EXPECT_FALSE(FilterListMetadata::Deserialize("random text\n"));
  EXPECT_FALSE(FilterListMetadata::Deserialize(""));
  EXPECT_FALSE(FilterListMetadata::Deserialize(
      "iNWEB-FILTERLIST-META v=1\nsourceId\t\n"));
  EXPECT_FALSE(FilterListMetadata::Deserialize(
      "iNWEB-FILTERLIST-META v=1\nsourceId\tid\n"));
  EXPECT_FALSE(FilterListMetadata::Deserialize(
      "iNWEB-FILTERLIST-META v=1\nsourceId\tid\ndownloadedAt\tnot-a-number\n"));
}

TEST(InwebFileFilterListCacheTest, StoreLoadRemoveRoundTrip) {
  base::ScopedTempDir dir;
  ASSERT_TRUE(dir.CreateUniqueTempDir());
  FileFilterListCache cache(dir.GetPath());

  const FilterListSource source = TestSource();
  const std::string body = "! v\n||ads.example.com^\n";
  cache.Store(source, body, SampleMetadata());

  const std::optional<std::string> loaded_body = cache.LoadBody(source);
  ASSERT_TRUE(loaded_body.has_value());
  EXPECT_EQ(body, *loaded_body);

  const std::optional<FilterListMetadata> loaded = cache.LoadMetadata(source);
  ASSERT_TRUE(loaded.has_value());
  EXPECT_EQ(1234567890, loaded->downloaded_at_ms);
  EXPECT_EQ(42, loaded->rule_count);

  // Corrupt metadata is reported as missing; the body stays usable.
  const std::optional<FilterListMetadata> missing =
      FilterListMetadata::Deserialize("corrupt");
  EXPECT_FALSE(missing.has_value());

  cache.Remove(source);
  EXPECT_FALSE(cache.LoadBody(source).has_value());
  EXPECT_FALSE(cache.LoadMetadata(source).has_value());
}

TEST(InwebMemoryFilterListCacheTest, StoreAndClear) {
  MemoryFilterListCache cache;
  const FilterListSource source = TestSource();
  cache.Store(source, "body", SampleMetadata());
  EXPECT_EQ("body", cache.LoadBody(source).value_or(""));
  cache.Clear();
  EXPECT_FALSE(cache.LoadBody(source).has_value());
  EXPECT_FALSE(cache.LoadMetadata(source).has_value());
}

}  // namespace inweb::adblock
