// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_default_filter_list.h"

namespace inweb::adblock {

const char kInwebDefaultFilterList[] =
    R"INWEB_LIST([Adblock Plus 2.0]
! iNWEB Browser — embedded starter tracking-protection list.
! Version: iNWEB-1.0
! Last modified: 2026-09-22
! Conservative, host-anchored rules for well-known advertising and
! tracking hosts (see header for the curation policy).
||doubleclick.net^
||googlesyndication.com^
||googleadservices.com^
||google-analytics.com^
||adservice.google.com^
||adnxs.com^
||criteo.com^
||criteo.net^
||taboola.com^
||outbrain.com^
||scorecardresearch.com^
||quantserve.com^
||quantcount.com^
||moatads.com^
||amazon-adsystem.com^
||adsrvr.org^
||doubleverify.com^
||adsafeprotected.com^
||graph.facebook.com^
||analytics.tiktok.com^
||ads-twitter.com^
||static.ads-twitter.com^
||ads.linkedin.com^
||bat.bing.com^
||clarity.ms^
||hotjar.com^
||mouseflow.com^
||fullstory.com^
||mixpanel.com^
||amplitude.com^
||heapanalytics.com^
||kissmetrics.com^
||chartbeat.com^
||parsely.com^
||matomo.cloud^
||adcolony.com^
||applovin.com^
||chartboost.com^
||inmobi.com^
||vungle.com^
!
! Device-test fixture (.invalid TLD — RFC 2606, never routable):
! one plain block, one type-constrained block, one exception.
||adblock-fixture.invalid^
||tracker-fixture.invalid^$script,image
@@||allowed-fixture.invalid^
)INWEB_LIST";

}  // namespace inweb::adblock
