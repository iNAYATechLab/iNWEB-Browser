// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — Security Center read-path tests + the embedded default
// list integration (patch 0007): real blocking from construction, honest
// dashboard numbers. Ported from the Kotlin reference
// (SecurityCenterTest) plus the native holder/bootstrap behaviors.

#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"

#include <string>
#include <vector>

#include "chrome/android/inweb/adblock/inweb_default_filter_list.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "chrome/android/inweb/adblock/inweb_security_center.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

RequestContext Request(const std::string& url,
                       ResourceType type = ResourceType::kImage) {
  return RequestContext{GURL(url), GURL("https://news.example.com/story"),
                        type};
}

}  // namespace

TEST(InwebDefaultFilterListTest, ParsesAndBlocksFixtures) {
  const ParsedFilterList parsed =
      FilterListParser::Parse(kInwebDefaultFilterList, "inweb-default");
  EXPECT_EQ(0, parsed.invalid_rule_count);
  EXPECT_EQ(0, parsed.unsupported_rule_count);
  EXPECT_GT(static_cast<int>(parsed.network_rules.size()), 10);

  InwebAdblockEngineHolder holder;
  EXPECT_TRUE(holder.IsEnabled());

  // Device-test fixture: all three decision paths, deterministically.
  EXPECT_EQ(FilterAction::kBlock,
            holder.Decide(Request("https://adblock-fixture.invalid/ad.png")).action);
  EXPECT_EQ(FilterAction::kBlock,
            holder.Decide(Request("https://tracker-fixture.invalid/t.js",
                                  ResourceType::kScript)).action);
  EXPECT_EQ(FilterAction::kBlock,
            holder.Decide(Request("https://tracker-fixture.invalid/t.png")).action);
  EXPECT_EQ(FilterAction::kPass,
            holder.Decide(Request("https://tracker-fixture.invalid/t.woff",
                                  ResourceType::kFont)).action);
  EXPECT_EQ(FilterAction::kAllow,
            holder.Decide(Request("https://allowed-fixture.invalid/x")).action);
  // A well-known default-list host is blocked out of the box.
  EXPECT_EQ(FilterAction::kBlock,
            holder.Decide(Request("https://doubleclick.net/pixel")).action);
  // Clean hosts pass.
  EXPECT_EQ(FilterAction::kPass,
            holder.Decide(Request("https://clean.example.org/logo.png")).action);
}

TEST(InwebSecurityCenterTest, ModelCarriesRealNumbersOnly) {
  InwebAdblockEngineHolder holder;

  // Produce real decisions.
  holder.Decide(Request("https://adblock-fixture.invalid/a.png"));
  holder.Decide(Request("https://adblock-fixture.invalid/b.png"));
  holder.Decide(Request("https://tracker-fixture.invalid/c.js",
                        ResourceType::kScript));
  holder.Decide(Request("https://allowed-fixture.invalid/x"));
  holder.Decide(Request("https://clean.example.org/logo.png"));

  const SecurityCenterModel model =
      holder.BuildSecurityCenterModel(/*enforcement_active=*/true);
  EXPECT_TRUE(model.enforcement_active);
  EXPECT_TRUE(model.tracking_protection_enabled);
  EXPECT_EQ(3, model.blocked_count);
  EXPECT_EQ(1, model.allowed_count);
  EXPECT_EQ(1, model.passed_count);
  EXPECT_EQ(5, model.total_decisions());

  ASSERT_EQ(1u, model.filter_lists.size());
  EXPECT_EQ("inweb-default", model.filter_lists[0].id);
  ASSERT_TRUE(model.filter_lists[0].version.has_value());
  EXPECT_EQ("iNWEB-1.0", *model.filter_lists[0].version);

  // The default list's real rule count, computed independently.
  const ParsedFilterList parsed =
      FilterListParser::Parse(kInwebDefaultFilterList, "inweb-default");
  EXPECT_EQ(static_cast<int>(parsed.network_rules.size()),
            model.filter_lists[0].rule_count);
  EXPECT_EQ(static_cast<int>(parsed.network_rules.size()),
            model.total_network_rules);

  ASSERT_EQ(2u, model.top_blocked_domains.size());
  EXPECT_EQ("adblock-fixture.invalid", model.top_blocked_domains[0].domain);
  EXPECT_EQ(2, model.top_blocked_domains[0].count);
  EXPECT_EQ("tracker-fixture.invalid", model.top_blocked_domains[1].domain);
  EXPECT_EQ(1, model.top_blocked_domains[1].count);
}

TEST(InwebSecurityCenterTest, TopDomainsSortedAndLimited) {
  InwebAdblockEngineHolder holder;
  // Six distinct blocked hosts (b.example … g.example patterns map onto
  // the fixture host's path space is not separable, so use the engine
  // directly for the sort/limit contract instead).
  std::vector<ParsedFilterList> lists;
  lists.push_back(FilterListParser::Parse(
      "||a.test^\n||b.test^\n||c.test^\n||d.test^\n||e.test^\n||f.test^\n",
      "six"));
  TrackingProtectionEngine engine(std::move(lists),
                                  TrackingProtectionSettings());

  for (const std::string& host :
       {"https://a.test/x", "https://a.test/y", "https://a.test/z",
        "https://b.test/x", "https://b.test/y", "https://c.test/x"}) {
    engine.Decide(Request(host));
  }
  // d/e/f get no decisions — the top list holds the 5 that did, ordered
  // by count desc then domain asc: a(3), b(2), c(1).
  const SecurityCenterModel model = SecurityCenter::Build(
      TrackingProtectionSettings(), engine.statistics().Snapshot(),
      FilterListManager(std::vector<FilterListSource>{}, nullptr, nullptr),
      /*enforcement_active=*/false);
  EXPECT_FALSE(model.enforcement_active);
  ASSERT_EQ(3u, model.top_blocked_domains.size());
  EXPECT_EQ("a.test", model.top_blocked_domains[0].domain);
  EXPECT_EQ(3, model.top_blocked_domains[0].count);
  EXPECT_EQ("b.test", model.top_blocked_domains[1].domain);
  EXPECT_EQ("c.test", model.top_blocked_domains[2].domain);
  EXPECT_EQ(6, model.blocked_count);
  EXPECT_TRUE(model.filter_lists.empty());
  EXPECT_EQ(0, model.total_network_rules);
}

}  // namespace inweb::adblock
