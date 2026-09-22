// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — tracking-protection engine (implementation).

#include "chrome/android/inweb/adblock/inweb_tracking_protection_engine.h"

#include <string>
#include <utility>
#include <vector>

#include "base/strings/string_util.h"
#include "base/synchronization/lock.h"
#include "chrome/android/inweb/adblock/inweb_domain_classifier.h"
#include "chrome/android/inweb/adblock/inweb_rule_matcher.h"

namespace inweb::adblock {

// --- TrackingProtectionSettings ---

TrackingProtectionSettings::TrackingProtectionSettings() = default;

TrackingProtectionSettings::TrackingProtectionSettings(
    bool enabled,
    CookiePolicy cookie_policy,
    std::set<std::string> allowlisted_sites)
    : enabled_(enabled),
      cookie_policy_(cookie_policy),
      allowlisted_sites_(std::move(allowlisted_sites)) {}

bool TrackingProtectionSettings::IsSiteAllowlisted(
    const std::string& host) const {
  std::string h(base::TrimString(host, ".", base::TRIM_ALL));
  h = base::ToLowerASCII(h);
  if (h.empty()) {
    return false;
  }
  for (const std::string& site : allowlisted_sites_) {
    if (h == site || base::EndsWith(h, "." + site) ||
        DomainClassifier::RegistrableDomain(h) == site) {
      return true;
    }
  }
  return false;
}

TrackingProtectionSettings TrackingProtectionSettings::WithAllowlistedSite(
    const std::string& site) const {
  std::set<std::string> sites = allowlisted_sites_;
  sites.insert(base::ToLowerASCII(base::TrimString(site, " ", base::TRIM_ALL)));
  return TrackingProtectionSettings(enabled_, cookie_policy_,
                                    std::move(sites));
}

TrackingProtectionSettings TrackingProtectionSettings::WithoutAllowlistedSite(
    const std::string& site) const {
  std::set<std::string> sites = allowlisted_sites_;
  sites.erase(base::ToLowerASCII(base::TrimString(site, " ", base::TRIM_ALL)));
  return TrackingProtectionSettings(enabled_, cookie_policy_,
                                    std::move(sites));
}

// --- EngineStatistics ---

int EngineStatistics::block_count() const {
  base::AutoLock lock(lock_);
  return block_count_;
}

int EngineStatistics::allow_count() const {
  base::AutoLock lock(lock_);
  return allow_count_;
}

int EngineStatistics::pass_count() const {
  base::AutoLock lock(lock_);
  return pass_count_;
}

std::map<std::string, int> EngineStatistics::blocked_by_domain() const {
  base::AutoLock lock(lock_);
  return blocked_by_domain_;
}

void EngineStatistics::RecordBlock(const std::string& request_host) const {
  base::AutoLock lock(lock_);
  ++block_count_;
  if (!request_host.empty()) {
    const std::string domain =
        DomainClassifier::RegistrableDomain(request_host);
    ++blocked_by_domain_[domain];
  }
}

void EngineStatistics::RecordAllow() const {
  base::AutoLock lock(lock_);
  ++allow_count_;
}

void EngineStatistics::RecordPass() const {
  base::AutoLock lock(lock_);
  ++pass_count_;
}

// --- TrackingProtectionEngine ---

TrackingProtectionEngine::TrackingProtectionEngine(
    std::vector<ParsedFilterList> parsed_lists,
    TrackingProtectionSettings settings)
    : settings_(std::move(settings)) {
  size_t total = 0;
  for (const ParsedFilterList& list : parsed_lists) {
    total += list.network_rules.size();
  }
  rules_.reserve(total);
  for (ParsedFilterList& list : parsed_lists) {
    for (NetworkFilterRule& rule : list.network_rules) {
      rules_.push_back(std::move(rule));
    }
  }
}

FilterDecision TrackingProtectionEngine::Decide(
    const RequestContext& request) const {
  if (!settings_.enabled()) {
    return FilterDecision{.action = FilterAction::kPass};
  }

  const std::string document_host = RuleMatcher::HostOf(request.document_url);
  if (!document_host.empty() && settings_.IsSiteAllowlisted(document_host)) {
    FilterDecision decision;
    decision.action = FilterAction::kAllow;
    decision.allowlisted_site = document_host;
    return decision;
  }

  const NetworkFilterRule* block_match = nullptr;
  for (const NetworkFilterRule& rule : rules_) {
    if (!RuleMatcher::Matches(rule, request)) {
      continue;
    }
    if (rule.is_exception) {
      statistics_.RecordAllow();
      return FilterDecision{.action = FilterAction::kAllow, .matched_rule = &rule};
    }
    if (block_match == nullptr) {
      block_match = &rule;
    }
  }

  if (block_match == nullptr) {
    statistics_.RecordPass();
    return FilterDecision{.action = FilterAction::kPass};
  }
  statistics_.RecordBlock(RuleMatcher::HostOf(request.request_url));
  return FilterDecision{.action = FilterAction::kBlock,
                        .matched_rule = block_match};
}

EngineStatisticsSnapshot EngineStatistics::Snapshot() const {
  base::AutoLock lock(lock_);
  EngineStatisticsSnapshot snapshot;
  snapshot.block_count = block_count_;
  snapshot.allow_count = allow_count_;
  snapshot.pass_count = pass_count_;
  snapshot.blocked_by_domain = blocked_by_domain_;
  return snapshot;
}

void TrackingProtectionEngine::UpdateSettings(
    TrackingProtectionSettings settings) {
  settings_ = std::move(settings);
}

}  // namespace inweb::adblock
