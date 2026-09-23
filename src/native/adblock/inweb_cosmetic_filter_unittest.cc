// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — cosmetic filter unit tests (PHASE4-COSMETIC §1),
// ported 1:1 from the tested Kotlin reference (CosmeticFilterTest.kt):
// parsing (## / #@# / #?# / #$# / #%#, domain includes/excludes),
// engine matching (universal, domain, exclude-wins, exception
// cancellation, grouped CSS), list-parser retention, and the holder's
// policy-gated CosmeticCssFor path.

#include "chrome/android/inweb/adblock/inweb_cosmetic_filter.h"

#include <vector>

#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

namespace {

CosmeticFilterEngine EngineFromText(const std::string& text) {
  return CosmeticFilterEngine(CosmeticFilterParser::Parse(text, "t").rules);
}

}  // namespace

TEST(CosmeticFilterParserTest, ParsesUniversalHidingRule) {
  const CosmeticLineResult r = CosmeticFilterParser::ParseLine("##.ad-banner");
  EXPECT_EQ(CosmeticLineResult::Kind::kHide, r.kind);
  EXPECT_EQ(".ad-banner", r.rule.selector);
  EXPECT_TRUE(r.rule.domains.empty());
  EXPECT_FALSE(r.rule.is_exception);
}

TEST(CosmeticFilterParserTest, ParsesDomainIncludesAndExcludes) {
  const CosmeticLineResult r =
      CosmeticFilterParser::ParseLine("example.com,~sub.example.com##.promo");
  EXPECT_EQ(CosmeticLineResult::Kind::kHide, r.kind);
  EXPECT_EQ(2u, r.rule.domains.size());
  EXPECT_TRUE(r.rule.domains.at("example.com"));
  EXPECT_FALSE(r.rule.domains.at("sub.example.com"));
}

TEST(CosmeticFilterParserTest, ParsesExceptionRule) {
  const CosmeticLineResult r =
      CosmeticFilterParser::ParseLine("example.com#@#.sponsored");
  EXPECT_EQ(CosmeticLineResult::Kind::kException, r.kind);
  EXPECT_TRUE(r.rule.is_exception);
  EXPECT_EQ(".sponsored", r.rule.selector);
}

TEST(CosmeticFilterParserTest, ProceduralRulesAreCountedNotMatched) {
  const ParsedCosmeticList parsed = CosmeticFilterParser::Parse(
      "example.com#?#.item:has-text(Sponsored)\n", "t");
  EXPECT_EQ(1, parsed.procedural_rule_count);
  EXPECT_TRUE(parsed.rules.empty());
}

TEST(CosmeticFilterParserTest, UnsupportedMarkersAreCounted) {
  const ParsedCosmeticList parsed = CosmeticFilterParser::Parse(
      "example.com#$#body { background: #fff; }\n"
      "example.com#%#window.__x = 1;\n",
      "t");
  EXPECT_EQ(2, parsed.unsupported_rule_count);
  EXPECT_TRUE(parsed.rules.empty());
}

TEST(CosmeticFilterParserTest, InvalidRulesAreCounted) {
  const ParsedCosmeticList parsed =
      CosmeticFilterParser::Parse("example.com##\nexample.com,##.x\n", "t");
  EXPECT_EQ(2, parsed.invalid_rule_count);
  EXPECT_TRUE(parsed.rules.empty());
}

TEST(CosmeticFilterParserTest, NetworkLinesAndCommentsAreSkipped) {
  const ParsedCosmeticList parsed = CosmeticFilterParser::Parse(
      "! comment\n[AdBlock Plus 2.0]\n||ads.example.com^\nnormal line\n",
      "t");
  EXPECT_EQ(2, parsed.comment_count);  // '!' line + '[' header line
  EXPECT_TRUE(parsed.rules.empty());
  EXPECT_EQ(0, parsed.invalid_rule_count);
}

TEST(CosmeticFilterEngineTest, UniversalRuleAppliesEverywhere) {
  CosmeticFilterEngine engine = EngineFromText("##.ad\n");
  EXPECT_EQ(".ad { display: none !important; }", engine.HideCssFor("anything.example.net"));
  EXPECT_EQ(".ad { display: none !important; }", engine.HideCssFor("other.org"));
}

TEST(CosmeticFilterEngineTest, DomainRuleAppliesToHostAndSubdomainsOnly) {
  CosmeticFilterEngine engine = EngineFromText("example.com##.promo\n");
  EXPECT_EQ(".promo { display: none !important; }",
            engine.HideCssFor("example.com"));
  EXPECT_EQ(".promo { display: none !important; }",
            engine.HideCssFor("sub.example.com"));
  EXPECT_EQ("", engine.HideCssFor("notexample.com"));
  EXPECT_EQ("", engine.HideCssFor("example.com.evil.net"));
}

TEST(CosmeticFilterEngineTest, ExcludeWinsOverInclude) {
  CosmeticFilterEngine engine =
      EngineFromText("example.com,~sub.example.com##.promo\n");
  EXPECT_EQ("", engine.HideCssFor("sub.example.com"));
  EXPECT_EQ(".promo { display: none !important; }",
            engine.HideCssFor("other.example.com"));
}

TEST(CosmeticFilterEngineTest, ExceptionCancelsSelectorOnMatchingDomains) {
  CosmeticFilterEngine engine = EngineFromText(
      "##.ad\n"
      "trusted.example##.ad\n");
  // The exception line here is a HIDE on trusted.example (not #@#): both
  // collapse into one selector. The real cancellation case is below.
  EXPECT_EQ(".ad { display: none !important; }",
            engine.HideCssFor("trusted.example"));

  CosmeticFilterEngine cancelled = EngineFromText(
      "##.ad\n"
      "trusted.example#@#.ad\n");
  EXPECT_EQ("", cancelled.HideCssFor("trusted.example"));
  EXPECT_EQ(".ad { display: none !important; }",
            cancelled.HideCssFor("other.example"));
}

TEST(CosmeticFilterEngineTest, GroupedCssOrEmptyWhenNothingApplies) {
  CosmeticFilterEngine engine = EngineFromText(
      "##.a\n"
      "example.com##.b\n"
      "other.net##.c\n");
  EXPECT_EQ(".a, .b { display: none !important; }",
            engine.HideCssFor("example.com"));
  // The universal rule applies everywhere — "nothing applies" needs a
  // rule set without universal rules.
  EXPECT_EQ(".a { display: none !important; }",
            engine.HideCssFor("nomatch.invalid"));
  // Host hygiene: trailing dot and case are handled (Kotlin parity) —
  // the lowered, dot-stripped host matches the example.com rule too.
  EXPECT_EQ(".a, .b { display: none !important; }",
            engine.HideCssFor("EXAMPLE.com."));
  CosmeticFilterEngine no_universal = EngineFromText(
      "example.com##.b\nother.net##.c\n");
  EXPECT_EQ("", no_universal.HideCssFor("nomatch.invalid"));
}

TEST(CosmeticFilterListParserTest, ListParserRetainsCosmeticRules) {
  // The network list parser routes cosmetic lines into the retained
  // cosmetic half while keeping its total-marker counter.
  const ParsedFilterList parsed = FilterListParser::Parse(
      "||ads.example.com^\n"
      "##.ad\n"
      "example.com#@#.promo\n"
      "example.com#?#.proc:has-text(x)\n"
      "! comment\n",
      "t");
  EXPECT_EQ(1, static_cast<int>(parsed.network_rules.size()));
  EXPECT_EQ(3, parsed.cosmetic_rule_count);  // ## + #@# + #?#
  ASSERT_EQ(2u, parsed.cosmetic_rules.size());
  EXPECT_EQ(".ad", parsed.cosmetic_rules[0].selector);
  EXPECT_FALSE(parsed.cosmetic_rules[0].is_exception);
  EXPECT_EQ(".promo", parsed.cosmetic_rules[1].selector);
  EXPECT_TRUE(parsed.cosmetic_rules[1].is_exception);
}

TEST(InwebAdblockHolderCosmeticTest, CosmeticCssForHonorsPolicyAndEngine) {
  InwebAdblockEngineHolder holder;
  TrackingProtectionSettings settings(
      /*enabled=*/true, CookiePolicy::kAllowAll,
      /*allowlisted_sites=*/{"trusted.example"});
  std::vector<ParsedFilterList> lists;
  lists.push_back(FilterListParser::Parse(
      "##.ad\n"
      "example.com##.promo\n",
      "cosmetic"));
  holder.SetFilterLists(std::move(lists));
  holder.UpdateSettings(settings);

  // Engine answers per host.
  EXPECT_EQ(".ad, .promo { display: none !important; }",
            holder.CosmeticCssFor("example.com"));
  EXPECT_EQ(".ad { display: none !important; }",
            holder.CosmeticCssFor("other.org"));
  // Allowlisted site: no cosmetics (single shields list).
  EXPECT_EQ("", holder.CosmeticCssFor("trusted.example"));

  // Global toggle off: no cosmetics anywhere.
  holder.UpdateSettings(
      TrackingProtectionSettings(false, CookiePolicy::kAllowAll, {}));
  EXPECT_EQ("", holder.CosmeticCssFor("example.com"));

  // No cosmetic rules at all: empty string, never a crash.
  InwebAdblockEngineHolder empty_holder;
  empty_holder.UpdateSettings(settings);
  EXPECT_EQ("", empty_holder.CosmeticCssFor("example.com"));
}

}  // namespace inweb::adblock
