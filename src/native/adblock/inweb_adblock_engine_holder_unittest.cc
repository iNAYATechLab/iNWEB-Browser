// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — engine holder unit tests: provisioning entry points and
// the disabled/empty-list behavior that patch 0006 ships with (0007 wires
// real list loading). Uses direct instances — the process-wide
// GetInstance() stays untouched by tests.

#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"

#include <vector>

#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

RequestContext Request(const std::string& url) {
  return RequestContext{GURL(url), GURL("https://news.example.com/story"),
                        ResourceType::kImage};
}

}  // namespace

TEST(InwebAdblockEngineHolderTest, EmptyByDefaultPassesEverything) {
  InwebAdblockEngineHolder holder;
  EXPECT_TRUE(holder.IsEnabled());
  const FilterDecision decision =
      holder.Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kPass, decision.action);
  EXPECT_EQ(nullptr, decision.matched_rule);
}

TEST(InwebAdblockEngineHolderTest, SetFilterListsActivatesBlocking) {
  InwebAdblockEngineHolder holder;
  std::vector<ParsedFilterList> lists;
  lists.push_back(FilterListParser::Parse("||ads.example.com^\n", "test"));
  holder.SetFilterLists(std::move(lists));

  const FilterDecision blocked =
      holder.Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kBlock, blocked.action);

  const FilterDecision clean =
      holder.Decide(Request("https://clean.example.org/logo.png"));
  EXPECT_EQ(FilterAction::kPass, clean.action);
}

TEST(InwebAdblockEngineHolderTest, UpdateSettingsDisablesFiltering) {
  InwebAdblockEngineHolder holder;
  std::vector<ParsedFilterList> lists;
  lists.push_back(FilterListParser::Parse("||ads.example.com^\n", "test"));
  holder.SetFilterLists(std::move(lists));

  TrackingProtectionSettings off(/*enabled=*/false,
                                 CookiePolicy::kBlockThirdParty,
                                 /*allowlisted_sites=*/{});
  holder.UpdateSettings(off);
  EXPECT_FALSE(holder.IsEnabled());

  const FilterDecision decision =
      holder.Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kPass, decision.action);
}

TEST(InwebAdblockEngineHolderTest, SettingsSurviveListReplacement) {
  InwebAdblockEngineHolder holder;
  TrackingProtectionSettings settings =
      TrackingProtectionSettings().WithAllowlistedSite("news.example.com");
  holder.UpdateSettings(settings);

  std::vector<ParsedFilterList> lists;
  lists.push_back(FilterListParser::Parse("||ads.example.com^\n", "test"));
  holder.SetFilterLists(std::move(lists));

  // The allowlisted document host still bypasses the fresh rules.
  const FilterDecision decision =
      holder.Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kAllow, decision.action);
  EXPECT_EQ("news.example.com", decision.allowlisted_site);
}

}  // namespace inweb::adblock
