// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — RuleMatcher unit tests.
// Ported 1:1 from the Kotlin reference (RuleMatcherTest.kt).

#include "chrome/android/inweb/adblock/inweb_rule_matcher.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

RequestContext Request(const std::string& url,
                       const std::string& document = "https://news.example.com/story",
                       ResourceType type = ResourceType::kImage) {
  return RequestContext{GURL(url), GURL(document), type};
}

bool Matches(const std::string& line, const RequestContext& request) {
  ParsedFilterList list = FilterListParser::Parse(line);
  EXPECT_EQ(1u, list.network_rules.size());
  return RuleMatcher::Matches(list.network_rules[0], request);
}

}  // namespace

TEST(InwebRuleMatcherTest, DomainAnchorMatchesHostAndSubdomains) {
  EXPECT_TRUE(Matches("||ads.example.com^",
                      Request("https://ads.example.com/pixel.gif")));
  EXPECT_TRUE(Matches("||ads.example.com^",
                      Request("https://cdn.ads.example.com/pixel.gif")));
  // End-of-URL separator.
  EXPECT_TRUE(
      Matches("||ads.example.com^", Request("https://ads.example.com")));
}

TEST(InwebRuleMatcherTest, DomainAnchorRejectsSimilarHosts) {
  EXPECT_FALSE(Matches("||ads.example.com^",
                       Request("https://notads.example.com/x")));
  EXPECT_FALSE(Matches("||ads.example.com^",
                       Request("https://ads.example.com.evil.net/x")));
  EXPECT_FALSE(Matches("||ads.example.com^",
                       Request("https://ads.example.company.com/x")));
}

TEST(InwebRuleMatcherTest, SeparatorRequiresBoundaryAfterPattern) {
  EXPECT_TRUE(
      Matches("||example.com^", Request("https://example.com:8080/x")));
  EXPECT_TRUE(Matches("||example.com^", Request("https://example.com?utm=1")));
  EXPECT_FALSE(
      Matches("||example.com^", Request("https://example.community.com/x")));
}

TEST(InwebRuleMatcherTest, WildcardMatchesAnyCharacters) {
  EXPECT_TRUE(Matches("||cdn.example.com/ads/*.js",
                      Request("https://cdn.example.com/ads/foo.js")));
  EXPECT_TRUE(Matches("||cdn.example.com/ads/*.js",
                      Request("https://cdn.example.com/ads/a/b/c.js")));
  EXPECT_FALSE(Matches("||cdn.example.com/ads/*.js",
                       Request("https://cdn.example.com/ads/foo.css")));
}

TEST(InwebRuleMatcherTest, LeftAnchorAnchorsToUrlStart) {
  EXPECT_TRUE(Matches("|https://example.com",
                      Request("https://example.com/page")));
  EXPECT_FALSE(Matches("|https://example.com",
                       Request("https://evil.com/?u=https://example.com")));
}

TEST(InwebRuleMatcherTest, RightAnchorAnchorsToUrlEnd) {
  EXPECT_TRUE(
      Matches("/banner.gif|", Request("https://x.example.com/banner.gif")));
  EXPECT_FALSE(Matches("/banner.gif|",
                       Request("https://x.example.com/banner.gif?v=2")));
}

TEST(InwebRuleMatcherTest, PlainSubstringMatchesAnywhere) {
  EXPECT_TRUE(Matches("tracker.js",
                      Request("https://static.evil.net/libs/tracker.js?x=1")));
  EXPECT_FALSE(
      Matches("tracker.js", Request("https://static.evil.net/libs/other.js")));
}

TEST(InwebRuleMatcherTest, TypeOptionConstrainsMatching) {
  const std::string line = "||ads.example.com^$script";
  EXPECT_TRUE(Matches(line, Request("https://ads.example.com/x",
                                    /*document=*/"", ResourceType::kScript)));
  EXPECT_FALSE(Matches(line, Request("https://ads.example.com/x")));
}

TEST(InwebRuleMatcherTest, NegatedTypeOption) {
  const std::string line = "||ads.example.com^$~script";
  EXPECT_FALSE(Matches(line, Request("https://ads.example.com/x",
                                     /*document=*/"", ResourceType::kScript)));
  EXPECT_TRUE(Matches(line, Request("https://ads.example.com/x")));
}

TEST(InwebRuleMatcherTest, ThirdPartyOption) {
  const std::string line = "||media.net^$third-party";
  // Same registrable domain → first-party → no match.
  EXPECT_FALSE(Matches(line, Request("https://cdn.media.net/t.gif",
                                     "https://www.media.net/page")));
  // Different registrable domain → third-party → match.
  EXPECT_TRUE(Matches(line, Request("https://cdn.media.net/t.gif",
                                    "https://news.example.com/story")));
}

TEST(InwebRuleMatcherTest, DomainOptionIncludesAndExcludes) {
  const std::string line = "||ads.example^$domain=www.news.example|~meta.news.example";
  EXPECT_TRUE(Matches(line, Request("https://ads.example/x",
                                    "https://www.news.example/story")));
  EXPECT_FALSE(Matches(line, Request("https://ads.example/x",
                                     "https://meta.news.example/story")));
  EXPECT_FALSE(Matches(line, Request("https://ads.example/x",
                                     "https://blog.other.org/story")));
}

TEST(InwebRuleMatcherTest, DomainOptionMatchesDocumentSubdomains) {
  const std::string line = "||ads.example^$domain=news.example.com";
  EXPECT_TRUE(Matches(line, Request("https://ads.example/x",
                                    "https://sub.news.example.com/story")));
}

TEST(InwebRuleMatcherTest, DomainOptionWithoutIncludesAppliesEverywhereButExcluded) {
  const std::string line = "||ads.example^$domain=~news.example.com";
  EXPECT_TRUE(Matches(line, Request("https://ads.example/x",
                                    "https://blog.other.org/story")));
  EXPECT_FALSE(Matches(line, Request("https://ads.example/x",
                                     "https://sub.news.example.com/story")));
}

}  // namespace inweb::adblock
