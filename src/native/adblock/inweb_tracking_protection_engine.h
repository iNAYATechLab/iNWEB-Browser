// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — tracking-protection / ad-blocking decision engine.
//
// Ported from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/TrackingProtectionEngine.kt).
//
// Decision order:
//  1. engine disabled → PASS (nothing recorded)
//  2. the document's site is user-allowlisted → ALLOW for the whole page
//  3. a matching exception rule (@@) → ALLOW
//  4. a matching blocking rule → BLOCK (first matching rule wins)
//  5. otherwise → PASS
//
// Statistics count real decisions only — never fabricated (§24 honesty
// rule). Decisions run on worker-pool threads (per the throttle design);
// the engine snapshot is immutable after construction and the statistics
// are lock-guarded, so Decide() is safe to call concurrently.
//
// v1 performs the full rule scan (`matchingRulesNaive` semantics in the
// Kotlin reference); the combined matcher is the Phase 5 performance
// deliverable (patch 0012) — PHASE4-ADBLOCK-DESIGN §5.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_TRACKING_PROTECTION_ENGINE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_TRACKING_PROTECTION_ENGINE_H_

#include <map>
#include <set>
#include <string>
#include <vector>

#include "base/synchronization/lock.h"
#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"

namespace inweb::adblock {

// Cookie policy defaults (§10). Enforcement ships with the privacy engine
// patch.
enum class CookiePolicy { kAllowAll, kBlockThirdParty, kBlockAll };

// Tracking-protection settings model (§10). These are the policy inputs
// the engine patches enforce; they are NOT yet user-facing toggles (no UI
// until the enforcing patches exist — §57).
class TrackingProtectionSettings {
 public:
  TrackingProtectionSettings();
  TrackingProtectionSettings(bool enabled,
                             CookiePolicy cookie_policy,
                             std::set<std::string> allowlisted_sites);

  bool enabled() const { return enabled_; }
  CookiePolicy cookie_policy() const { return cookie_policy_; }

  // True when filtering is bypassed for pages on this host.
  bool IsSiteAllowlisted(const std::string& host) const;

  TrackingProtectionSettings WithAllowlistedSite(const std::string& site) const;
  TrackingProtectionSettings WithoutAllowlistedSite(
      const std::string& site) const;

  const std::set<std::string>& allowlisted_sites() const {
    return allowlisted_sites_;
  }

 private:
  bool enabled_ = true;
  CookiePolicy cookie_policy_ = CookiePolicy::kBlockThirdParty;
  std::set<std::string> allowlisted_sites_;
};

// Real decision counters, backing Security Center statistics (§24).
// Thread-safe: decisions run on worker-pool threads.
class EngineStatistics {
 public:
  int block_count() const;
  int allow_count() const;
  int pass_count() const;
  std::map<std::string, int> blocked_by_domain() const;

  void RecordBlock(const std::string& request_host) const;
  void RecordAllow() const;
  void RecordPass() const;

 private:
  mutable base::Lock lock_;
  mutable int block_count_ = 0;
  mutable int allow_count_ = 0;
  mutable int pass_count_ = 0;
  mutable std::map<std::string, int> blocked_by_domain_;
};

// The decision engine. Immutable after construction except statistics.
class TrackingProtectionEngine {
 public:
  TrackingProtectionEngine(std::vector<ParsedFilterList> parsed_lists,
                           TrackingProtectionSettings settings);

  FilterDecision Decide(const RequestContext& request) const;

  // Replaces settings (site allowlist, enabled flag). Not internally
  // synchronized: the caller must serialize against Decide() —
  // InwebAdblockEngineHolder (patch 0006) holds its lock across calls.
  void UpdateSettings(TrackingProtectionSettings settings);

  const EngineStatistics& statistics() const { return statistics_; }
  const TrackingProtectionSettings& settings() const { return settings_; }
  const std::vector<NetworkFilterRule>& rules() const { return rules_; }

 private:
  std::vector<NetworkFilterRule> rules_;
  TrackingProtectionSettings settings_;
  EngineStatistics statistics_;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_TRACKING_PROTECTION_ENGINE_H_
