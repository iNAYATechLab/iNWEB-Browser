// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — data-saver core unit tests (PHASE7 §4; §19/§20 honesty
// rules). Every assertion here is about a claim we make to the user: what
// gets blocked, and what the counters are allowed to say.

#include "chrome/android/inweb/offline/inweb_data_saver.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::offline {

namespace {

adblock::RequestContext Request(const std::string& request_url,
                                const std::string& document_url,
                                adblock::ResourceType type) {
  adblock::RequestContext context;
  context.request_url = GURL(request_url);
  context.document_url = GURL(document_url);
  context.resource_type = type;
  return context;
}

DataSaverSettings Enabled() {
  DataSaverSettings settings;
  settings.enabled = true;
  return settings;
}

}  // namespace

TEST(InwebDataSaverTest, OffModeClaimsNothing) {
  DataSaverPolicy policy{DataSaverSettings()};
  DataSaverDecision decision =
      policy.Decide(Request("https://cdn.example.com/a.png",
                            "https://site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/false);
  EXPECT_FALSE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kNone);
}

TEST(InwebDataSaverTest, ThirdPartyImageBlockedByDefault) {
  DataSaverPolicy policy{Enabled()};
  DataSaverDecision decision =
      policy.Decide(Request("https://cdn.example.com/a.png",
                            "https://site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/false);
  EXPECT_TRUE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kThirdPartyImage);
}

TEST(InwebDataSaverTest, SameSiteImageIsLeftAlone) {
  DataSaverPolicy policy{Enabled()};
  DataSaverDecision decision =
      policy.Decide(Request("https://site.test/img/a.png",
                            "https://site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/false);
  EXPECT_FALSE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kNone);
}

TEST(InwebDataSaverTest, SubdomainOfTheSameRegistrableDomainIsFirstParty) {
  DataSaverPolicy policy{Enabled()};
  // a.img.site.test and site.test share the registrable domain site.test.
  DataSaverDecision decision =
      policy.Decide(Request("https://a.img.site.test/a.png",
                            "https://www.site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/false);
  EXPECT_FALSE(decision.block);
}

TEST(InwebDataSaverTest, ThirdPartyFontRequiresOptIn) {
  DataSaverPolicy policy{Enabled()};
  adblock::RequestContext font = Request("https://fonts.example.com/a.woff2",
                                         "https://site.test/page",
                                         adblock::ResourceType::kFont);
  EXPECT_FALSE(policy.Decide(font, /*is_speculative=*/false).block);

  DataSaverSettings aggressive = Enabled();
  aggressive.block_third_party_fonts = true;
  policy.UpdateSettings(aggressive);
  DataSaverDecision decision = policy.Decide(font, /*is_speculative=*/false);
  EXPECT_TRUE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kThirdPartyFont);
}

TEST(InwebDataSaverTest, MediaIsNeverBlockedByDefault) {
  DataSaverPolicy policy{Enabled()};
  adblock::RequestContext media = Request("https://cdn.example.com/v.mp4",
                                          "https://site.test/page",
                                          adblock::ResourceType::kMedia);
  EXPECT_FALSE(policy.Decide(media, /*is_speculative=*/false).block);

  DataSaverSettings with_media = Enabled();
  with_media.block_media = true;
  policy.UpdateSettings(with_media);
  DataSaverDecision decision = policy.Decide(media, /*is_speculative=*/false);
  EXPECT_TRUE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kHeavyweightMedia);
}

TEST(InwebDataSaverTest, SpeculativeRequestsAreSuppressedWhileOn) {
  DataSaverPolicy policy{Enabled()};
  DataSaverDecision decision =
      policy.Decide(Request("https://cdn.example.com/a.png",
                            "https://site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/true);
  EXPECT_TRUE(decision.block);
  EXPECT_EQ(decision.reason, DataSaverBlockReason::kSpeculative);

  // With speculative reduction off the request is judged on its own
  // merits: this one is a third-party image, so it is still blocked — but
  // as an image rule, never as a speculative suppression.
  DataSaverSettings no_speculative = Enabled();
  no_speculative.reduce_speculative = false;
  policy.UpdateSettings(no_speculative);
  DataSaverDecision as_image =
      policy.Decide(Request("https://cdn.example.com/a.png",
                            "https://site.test/page",
                            adblock::ResourceType::kImage),
                    /*is_speculative=*/true);
  EXPECT_TRUE(as_image.block);
  EXPECT_EQ(as_image.reason, DataSaverBlockReason::kThirdPartyImage);

  // A speculative request for something data saver never blocks stays
  // untouched once speculative reduction is off.
  DataSaverDecision as_script =
      policy.Decide(Request("https://cdn.example.com/a.js",
                            "https://site.test/page",
                            adblock::ResourceType::kScript),
                    /*is_speculative=*/true);
  EXPECT_FALSE(as_script.block);
  EXPECT_EQ(as_script.reason, DataSaverBlockReason::kNone);
}

TEST(InwebDataSaverTest, DocumentsScriptsAndXhrAreNeverDataSaverBlocks) {
  DataSaverSettings aggressive = Enabled();
  aggressive.block_third_party_fonts = true;
  aggressive.block_media = true;
  DataSaverPolicy policy(aggressive);

  for (adblock::ResourceType type : {adblock::ResourceType::kDocument,
                                     adblock::ResourceType::kScript,
                                     adblock::ResourceType::kXhr,
                                     adblock::ResourceType::kStylesheet}) {
    DataSaverDecision decision =
        policy.Decide(Request("https://cdn.example.com/x",
                              "https://site.test/page", type),
                      /*is_speculative=*/false);
    EXPECT_FALSE(decision.block);
    EXPECT_EQ(decision.reason, DataSaverBlockReason::kNone);
  }
}

TEST(InwebDataSaverTest, CountersReportRealAvoidedRequestsOnly) {
  DataSaverStatistics stats;
  EXPECT_EQ(stats.requests_avoided(), 0);
  EXPECT_EQ(stats.estimated_bytes_saved(), 0);

  stats.RecordAvoided(/*size_known=*/true, 12000);
  stats.RecordAvoided(/*size_known=*/true, 8000);
  EXPECT_EQ(stats.requests_avoided(), 2);
  EXPECT_EQ(stats.estimated_bytes_saved(), 20000);

  stats.Reset();
  EXPECT_EQ(stats.requests_avoided(), 0);
  EXPECT_EQ(stats.estimated_bytes_saved(), 0);
}

TEST(InwebDataSaverTest, UnknownSizesAreCountedButNeverEstimated) {
  DataSaverStatistics stats;
  // The estimate must stay at zero: we never saw a content-length, so we
  // have nothing honest to add (PHASE7 §4.4).
  stats.RecordAvoided(/*size_known=*/false, 0);
  stats.RecordAvoided(/*size_known=*/false, 0);
  EXPECT_EQ(stats.requests_avoided(), 2);
  EXPECT_EQ(stats.requests_with_unknown_size(), 2);
  EXPECT_EQ(stats.estimated_bytes_saved(), 0);

  stats.RecordAvoided(/*size_known=*/true, 500);
  EXPECT_EQ(stats.requests_avoided(), 3);
  EXPECT_EQ(stats.requests_with_unknown_size(), 2);
  EXPECT_EQ(stats.estimated_bytes_saved(), 500);
}

TEST(InwebDataSaverTest, SpeculativeSuppressionsAreAccountedSeparately) {
  DataSaverStatistics stats;
  stats.RecordSpeculativeSuppressed();
  stats.RecordSpeculativeSuppressed();
  // A suppressed prefetch was never a request, so it is not an "avoided
  // request" — inflating that number would be a false claim.
  EXPECT_EQ(stats.speculative_suppressed(), 2);
  EXPECT_EQ(stats.requests_avoided(), 0);
}

TEST(InwebDataSaverTest, NegativeOrZeroLengthsAddNothingToTheEstimate) {
  DataSaverStatistics stats;
  stats.RecordAvoided(/*size_known=*/true, 0);
  EXPECT_EQ(stats.requests_avoided(), 1);
  EXPECT_EQ(stats.estimated_bytes_saved(), 0);
  EXPECT_EQ(stats.requests_with_unknown_size(), 1);
}

}  // namespace inweb::offline
