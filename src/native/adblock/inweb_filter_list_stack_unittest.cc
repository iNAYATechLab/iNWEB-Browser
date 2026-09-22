// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter-list stack unit tests (source model, version
// headers, checksum, update policy). Ported from the Kotlin reference
// (FilterListSource/FilterListVersion/FilterListChecksum/UpdatePolicy
// tests).

#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_version.h"
#include "chrome/android/inweb/adblock/inweb_update_policy.h"

#include <string>

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

TEST(InwebFilterListSourceTest, ValidatesIdTitleAndUrl) {
  EXPECT_TRUE(FilterListSource::Create("easylist", "EasyList",
                                       std::string("https://example.com/l")));
  EXPECT_FALSE(FilterListSource::Create("EasyList", "EasyList",
                                        std::string("https://e.com/l")));
  EXPECT_FALSE(FilterListSource::Create("", "T", std::string("https://e.com/l")));
  EXPECT_FALSE(FilterListSource::Create("has_underscore", "T",
                                        std::string("https://e.com/l")));
  EXPECT_FALSE(FilterListSource::Create("a", " ", std::string("https://e.com/l")));
  EXPECT_FALSE(FilterListSource::Create("a", "T", std::string("ftp://e.com/l")));
  // Embedded lists carry no download URL — allowed (ADR-041 deviation).
  EXPECT_TRUE(FilterListSource::Create("inweb-default", "iNWEB Starter List",
                                       std::nullopt));
}

TEST(InwebFilterListSourceTest, DefaultsAndDefaultList) {
  const std::vector<FilterListSource>& defaults = FilterListSource::Defaults();
  ASSERT_EQ(2u, defaults.size());
  EXPECT_EQ("easylist", defaults[0].id);
  EXPECT_EQ("easyprivacy", defaults[1].id);
  EXPECT_TRUE(defaults[0].enabled);

  const FilterListSource builtin = FilterListSource::DefaultList();
  EXPECT_EQ("inweb-default", builtin.id);
  EXPECT_FALSE(builtin.download_url.has_value());
}

TEST(InwebFilterListVersionTest, ParsesEasyListFamilyHeaders) {
  const FilterListVersionInfo info = ParseFilterListVersion(
      "[Adblock Plus 2.0]\n! Version: 1.2.3\n! Last modified: 22 Sep 2026\n! "
      "x\n||ads.example.com^\n");
  ASSERT_TRUE(info.version.has_value());
  EXPECT_EQ("1.2.3", *info.version);
  ASSERT_TRUE(info.last_modified.has_value());
  EXPECT_EQ("22 Sep 2026", *info.last_modified);
}

TEST(InwebFilterListVersionTest, CaseInsensitiveAndFirstWins) {
  const FilterListVersionInfo info =
      ParseFilterListVersion("! version: A\n! Version: B\n");
  ASSERT_TRUE(info.version.has_value());
  EXPECT_EQ("A", *info.version);
  EXPECT_FALSE(ParseFilterListVersion("! comment only\n").version.has_value());
  EXPECT_FALSE(ParseFilterListVersion("! Version:  \n").version.has_value());
}

TEST(InwebFilterListChecksumTest, KnownSha256Vector) {
  // sha256("abc") — the standard test vector.
  EXPECT_EQ("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256Hex("abc"));
  EXPECT_EQ("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256Hex(""));
}

TEST(InwebUpdatePolicyTest, RefreshDueDecisions) {
  UpdatePolicy policy;
  EXPECT_FALSE(UpdatePolicy{/*enabled=*/false}.IsRefreshDue(std::nullopt, 100));

  EXPECT_TRUE(policy.IsRefreshDue(std::nullopt, 0));
  FilterListMetadata fresh;
  fresh.downloaded_at_ms = 1000;
  EXPECT_FALSE(policy.IsRefreshDue(fresh, 1000 + policy.refresh_interval_ms - 1));
  EXPECT_TRUE(policy.IsRefreshDue(fresh, 1000 + policy.refresh_interval_ms));
}

}  // namespace inweb::adblock
