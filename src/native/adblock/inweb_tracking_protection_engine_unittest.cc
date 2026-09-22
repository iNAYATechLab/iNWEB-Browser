// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — TrackingProtectionEngine unit tests.
// Ported 1:1 from the Kotlin reference (TrackingProtectionEngineTest.kt).

#include "chrome/android/inweb/adblock/inweb_tracking_protection_engine.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

ParsedFilterList TestList() {
  return FilterListParser::Parse(
      "||ads.example.com^\n||tracker.net^$script,third-party\n@@||good.example.com^\n",
      "test-list");
}

TrackingProtectionEngine Engine(TrackingProtectionSettings settings =
                                   TrackingProtectionSettings()) {
  std::vector<ParsedFilterList> lists;
  lists.push_back(TestList());
  return TrackingProtectionEngine(std::move(lists), std::move(settings));
}

RequestContext Request(const std::string& url,
                       const std::string& document = "https://news.example.com/story",
                       ResourceType type = ResourceType::kImage) {
  return RequestContext{GURL(url), GURL(document), type};
}

}  // namespace

TEST(InwebTrackingProtectionEngineTest, BlocksMatchingRequestWithRule) {
  TrackingProtectionEngine engine = Engine();
  const FilterDecision decision =
      engine.Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kBlock, decision.action);
  ASSERT_NE(nullptr, decision.matched_rule);
  EXPECT_EQ("||ads.example.com^", decision.matched_rule->raw);
}

TEST(InwebTrackingProtectionEngineTest, PassesUnmatchedRequest) {
  const FilterDecision decision =
      Engine().Decide(Request("https://cdn.example.org/logo.png"));
  EXPECT_EQ(FilterAction::kPass, decision.action);
  EXPECT_EQ(nullptr, decision.matched_rule);
}

TEST(InwebTrackingProtectionEngineTest, ExceptionRuleOverridesBlocking) {
  TrackingProtectionEngine engine = Engine();
  const FilterDecision decision =
      engine.Decide(Request("https://good.example.com/api"));
  EXPECT_EQ(FilterAction::kAllow, decision.action);
  ASSERT_NE(nullptr, decision.matched_rule);
  EXPECT_EQ("@@||good.example.com^", decision.matched_rule->raw);
}

TEST(InwebTrackingProtectionEngineTest, TypeConstrainedRuleOnlyBlocksItsType) {
  const FilterDecision decision = Engine().Decide(
      Request("https://tracker.net/lib.js", /*document=*/"", ResourceType::kScript));
  EXPECT_EQ(FilterAction::kBlock, decision.action);

  const FilterDecision image_decision =
      Engine().Decide(Request("https://tracker.net/pixel.gif"));
  EXPECT_EQ(FilterAction::kPass, image_decision.action);
}

TEST(InwebTrackingProtectionEngineTest,
     PerSiteAllowlistBypassesFilteringForWholePage) {
  const TrackingProtectionSettings settings =
      TrackingProtectionSettings().WithAllowlistedSite("news.example.com");
  const FilterDecision decision =
      Engine(settings).Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kAllow, decision.action);
  EXPECT_EQ("news.example.com", decision.allowlisted_site);
  EXPECT_EQ(nullptr, decision.matched_rule);
}

TEST(InwebTrackingProtectionEngineTest, DisabledEnginePassesEverything) {
  const TrackingProtectionSettings settings(/*enabled=*/false,
                                            CookiePolicy::kBlockThirdParty,
                                            /*allowlisted_sites=*/{});
  const FilterDecision decision =
      Engine(settings).Decide(Request("https://ads.example.com/pixel.gif"));
  EXPECT_EQ(FilterAction::kPass, decision.action);
}

TEST(InwebTrackingProtectionEngineTest, StatisticsCountRealDecisions) {
  TrackingProtectionEngine engine = Engine();
  engine.Decide(Request("https://ads.example.com/pixel.gif"));
  engine.Decide(Request("https://sub.ads.example.com/other.gif"));
  engine.Decide(Request("https://clean.example.org/logo.png"));

  EXPECT_EQ(2, engine.statistics().block_count());
  EXPECT_EQ(1, engine.statistics().pass_count());
  EXPECT_EQ(0, engine.statistics().allow_count());
  const std::map<std::string, int> by_domain =
      engine.statistics().blocked_by_domain();
  ASSERT_EQ(1u, by_domain.size());
  EXPECT_EQ(2, by_domain.at("example.com"));
}

TEST(InwebTrackingProtectionEngineTest, AllowlistSiteHelpersRoundTrip) {
  const TrackingProtectionSettings settings =
      TrackingProtectionSettings().WithAllowlistedSite("News.Example.com");
  EXPECT_EQ(1u, settings.allowlisted_sites().size());
  EXPECT_NE(0u, settings.allowlisted_sites().count("news.example.com"));
  EXPECT_TRUE(settings.IsSiteAllowlisted("news.example.com"));
  EXPECT_TRUE(settings.IsSiteAllowlisted("sub.news.example.com"));
  const TrackingProtectionSettings removed =
      settings.WithoutAllowlistedSite("news.example.com");
  EXPECT_TRUE(removed.allowlisted_sites().empty());
}

}  // namespace inweb::adblock
