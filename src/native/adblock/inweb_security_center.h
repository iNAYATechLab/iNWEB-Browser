// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_SECURITY_CENTER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_SECURITY_CENTER_H_

#include <optional>
#include <string>
#include <vector>

#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"
#include "chrome/android/inweb/adblock/inweb_tracking_protection_engine.h"

namespace inweb::adblock {

class FilterListManager;

// Status of one filter list, for the Security Center (§24).
struct FilterListStatus {
  std::string id;
  int rule_count = 0;
  std::optional<std::string> version;
  std::optional<int64_t> last_checked_at_ms;
};

// A domain with its real blocked-request count, for the Security Center.
struct BlockedDomainCount {
  std::string domain;
  int count = 0;
};

// The Security Center dashboard contract (MASTER-SPEC §24). Every number
// comes from real state: real decisions made by TrackingProtectionEngine,
// real parsed filter lists, real policy inputs. Nothing is estimated or
// fabricated. Ported from the Kotlin reference (SecurityCenter.kt).
struct SecurityCenterModel {
  bool enforcement_active = false;
  bool tracking_protection_enabled = false;
  CookiePolicy cookie_policy = CookiePolicy::kBlockThirdParty;
  int allowlisted_site_count = 0;
  std::vector<FilterListStatus> filter_lists;
  int total_network_rules = 0;
  int blocked_count = 0;
  int allowed_count = 0;
  int passed_count = 0;
  std::vector<BlockedDomainCount> top_blocked_domains;

  int total_decisions() const {
    return blocked_count + allowed_count + passed_count;
  }
};

// Builds the SecurityCenterModel from real engine statistics, real policy
// inputs, and the real filter-list state. The dashboard UI renders this
// model; it never reads raw internals itself. Ported from the Kotlin
// reference.
class SecurityCenter {
 public:
  static constexpr size_t kTopDomainsLimit = 5;

  static SecurityCenterModel Build(
      const TrackingProtectionSettings& settings,
      const EngineStatisticsSnapshot& statistics,
      const FilterListManager& list_manager,
      bool enforcement_active);
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_SECURITY_CENTER_H_
