// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — FilterListParser unit tests.
// Ported 1:1 from the Kotlin reference (FilterListParserTest.kt).

#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

TEST(InwebFilterListParserTest, CommentsAndHeaderAreCountedNotParsed) {
  const ParsedFilterList list =
      FilterListParser::Parse("[Adblock Plus 2.0]\n! a comment\n! another comment\n",
                              "test");
  EXPECT_EQ(0u, list.network_rules.size());
  EXPECT_EQ(3, list.comment_count);
}

TEST(InwebFilterListParserTest, CosmeticRulesAreRecognizedAndCounted) {
  const ParsedFilterList list = FilterListParser::Parse(
      "example.com##.ad-banner\n##.sponsor\n||ads.example.com^\n");
  EXPECT_EQ(2, list.cosmetic_rule_count);
  EXPECT_EQ(1u, list.network_rules.size());
}

TEST(InwebFilterListParserTest, ParsesDomainAnchoredRule) {
  const ParsedFilterList list = FilterListParser::Parse("||ads.example.com^");
  ASSERT_EQ(1u, list.network_rules.size());
  const NetworkFilterRule& rule = list.network_rules[0];
  EXPECT_TRUE(rule.domain_anchor);
  EXPECT_FALSE(rule.anchor_start);
  EXPECT_FALSE(rule.anchor_end);
  EXPECT_FALSE(rule.is_exception);
  EXPECT_EQ("ads.example.com^", rule.pattern);
  EXPECT_EQ("||ads.example.com^", rule.raw);
}

TEST(InwebFilterListParserTest, ParsesExceptionRule) {
  const ParsedFilterList list =
      FilterListParser::Parse("@@||example.com^");
  ASSERT_EQ(1u, list.network_rules.size());
  EXPECT_TRUE(list.network_rules[0].is_exception);
  EXPECT_EQ("example.com^", list.network_rules[0].pattern);
}

TEST(InwebFilterListParserTest, ParsesLeftAndRightAnchors) {
  const ParsedFilterList left =
      FilterListParser::Parse("|https://ads.example.com/x");
  ASSERT_EQ(1u, left.network_rules.size());
  EXPECT_TRUE(left.network_rules[0].anchor_start);
  EXPECT_FALSE(left.network_rules[0].domain_anchor);

  const ParsedFilterList right = FilterListParser::Parse("/banner*.gif|");
  ASSERT_EQ(1u, right.network_rules.size());
  EXPECT_TRUE(right.network_rules[0].anchor_end);
  EXPECT_FALSE(right.network_rules[0].anchor_start);
  EXPECT_FALSE(right.network_rules[0].domain_anchor);
}

TEST(InwebFilterListParserTest, ParsesTypeAndPartyOptions) {
  const ParsedFilterList list =
      FilterListParser::Parse("||ads.example.com^$script,third-party");
  ASSERT_EQ(1u, list.network_rules.size());
  const FilterOptions& options = list.network_rules[0].options;
  EXPECT_EQ(1u, options.types.size());
  EXPECT_NE(0u, options.types.count(ResourceType::kScript));
  ASSERT_TRUE(options.third_party.has_value());
  EXPECT_TRUE(*options.third_party);
}

TEST(InwebFilterListParserTest, ParsesDomainIncludeAndExcludeOptions) {
  const ParsedFilterList list = FilterListParser::Parse(
      "||ads.example.com^$domain=news.example|~meta.news.example");
  ASSERT_EQ(1u, list.network_rules.size());
  const auto& domains = list.network_rules[0].options.domains;
  EXPECT_EQ(2u, domains.size());
  EXPECT_NE(0u, domains.count("news.example"));
  EXPECT_TRUE(domains.at("news.example"));
  EXPECT_NE(0u, domains.count("meta.news.example"));
  EXPECT_FALSE(domains.at("meta.news.example"));
}

TEST(InwebFilterListParserTest, NegatedTypeOptions) {
  const ParsedFilterList list =
      FilterListParser::Parse("||example.com^$~script,~image");
  ASSERT_EQ(1u, list.network_rules.size());
  const FilterOptions& options = list.network_rules[0].options;
  EXPECT_EQ(2u, options.negated_types.size());
  EXPECT_NE(0u, options.negated_types.count(ResourceType::kScript));
  EXPECT_NE(0u, options.negated_types.count(ResourceType::kImage));
  EXPECT_TRUE(options.types.empty());
}

TEST(InwebFilterListParserTest, UnknownOptionMarksRuleUnsupported) {
  const ParsedFilterList list =
      FilterListParser::Parse("||example.com^$csp=script-src 'none'");
  EXPECT_EQ(0u, list.network_rules.size());
  EXPECT_EQ(1, list.unsupported_rule_count);
}

TEST(InwebFilterListParserTest, MalformedRulesAreCountedInvalid) {
  const ParsedFilterList list = FilterListParser::Parse("||\n@@\n@@$script\n");
  EXPECT_EQ(0u, list.network_rules.size());
  EXPECT_EQ(3, list.invalid_rule_count);
}

}  // namespace inweb::adblock
