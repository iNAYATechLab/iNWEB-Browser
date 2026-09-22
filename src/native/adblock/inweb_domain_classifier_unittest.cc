// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — DomainClassifier unit tests.
// Ported from the Kotlin reference (DomainClassifierTest.kt). Runs against
// Chromium's full Public Suffix List (the Kotlin table's suffixes —
// co.uk, com.bd, co.jp — are real PSL entries, so expectations match).

#include "chrome/android/inweb/adblock/inweb_domain_classifier.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::adblock {

TEST(InwebDomainClassifierTest, SimpleDomainsReduceToRegistrableDomain) {
  EXPECT_EQ("example.com", DomainClassifier::RegistrableDomain("example.com"));
  EXPECT_EQ("example.com",
            DomainClassifier::RegistrableDomain("a.example.com"));
  EXPECT_EQ("example.com",
            DomainClassifier::RegistrableDomain("a.b.c.example.com"));
}

TEST(InwebDomainClassifierTest, MultiPartSuffixesNeedThreeLabels) {
  EXPECT_EQ("bbc.co.uk", DomainClassifier::RegistrableDomain("www.bbc.co.uk"));
  EXPECT_EQ("bbc.co.uk",
            DomainClassifier::RegistrableDomain("news.bbc.co.uk"));
  EXPECT_EQ("example.com.bd",
            DomainClassifier::RegistrableDomain("www.example.com.bd"));
  EXPECT_EQ("shop.co.jp",
            DomainClassifier::RegistrableDomain("www.shop.co.jp"));
}

TEST(InwebDomainClassifierTest, IpAddressHostsStayWhole) {
  EXPECT_EQ("192.168.0.1",
            DomainClassifier::RegistrableDomain("192.168.0.1"));
}

TEST(InwebDomainClassifierTest, SingleLabelHostsStayWhole) {
  EXPECT_EQ("localhost", DomainClassifier::RegistrableDomain("localhost"));
}

TEST(InwebDomainClassifierTest, ThirdPartyClassification) {
  // Same registrable domain → first-party.
  EXPECT_FALSE(DomainClassifier::IsThirdParty("news.example.com",
                                              "cdn.example.com"));
  // Different registrable domains → third-party.
  EXPECT_TRUE(
      DomainClassifier::IsThirdParty("news.example.com", "ads.other.net"));
  // Multi-part suffix: different sites under co.uk are third-party.
  EXPECT_TRUE(DomainClassifier::IsThirdParty("bbc.co.uk", "itv.co.uk"));
  // Same multi-part-suffix site.
  EXPECT_FALSE(
      DomainClassifier::IsThirdParty("www.bbc.co.uk", "news.bbc.co.uk"));
}

}  // namespace inweb::adblock
