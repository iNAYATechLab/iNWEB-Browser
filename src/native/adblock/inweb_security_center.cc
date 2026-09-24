// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_security_center.h"

#include <algorithm>
#include <utility>

#include "chrome/android/inweb/adblock/inweb_filter_list_manager.h"

namespace inweb::adblock {

// static
SecurityCenterModel SecurityCenter::Build(
    const TrackingProtectionSettings& settings,
    const EngineStatisticsSnapshot& statistics,
    const FilterListManager& list_manager,
    bool enforcement_active) {
  SecurityCenterModel model;
  model.enforcement_active = enforcement_active;
  model.tracking_protection_enabled = settings.enabled();
  model.cookie_policy = settings.cookie_policy();
  model.allowlisted_site_count =
      static_cast<int>(settings.allowlisted_sites().size());

  for (const auto& report : list_manager.list_reports()) {
    FilterListStatus status;
    status.id = report.id;
    status.rule_count = report.rule_count;
    status.version = report.version;
    status.last_checked_at_ms = report.last_checked_at_ms;
    model.filter_lists.push_back(std::move(status));
  }

  model.total_network_rules = list_manager.total_network_rules();
  model.blocked_count = statistics.block_count;
  model.allowed_count = statistics.allow_count;
  model.passed_count = statistics.pass_count;

  model.top_blocked_domains.reserve(statistics.blocked_by_domain.size());
  for (const auto& [domain, count] : statistics.blocked_by_domain) {
    model.top_blocked_domains.push_back(BlockedDomainCount{domain, count});
  }
  std::sort(model.top_blocked_domains.begin(),
            model.top_blocked_domains.end(),
            [](const BlockedDomainCount& a, const BlockedDomainCount& b) {
              if (a.count != b.count) {
                return a.count > b.count;
              }
              return a.domain < b.domain;
            });
  if (model.top_blocked_domains.size() > kTopDomainsLimit) {
    model.top_blocked_domains.resize(kTopDomainsLimit);
  }
  return model;
}


// Out-of-line ctor/dtor definitions (chromium-style fallout fix).
FilterListStatus::FilterListStatus() = default;
FilterListStatus::~FilterListStatus() = default;

SecurityCenterModel::SecurityCenterModel() = default;
SecurityCenterModel::~SecurityCenterModel() = default;


FilterListStatus::FilterListStatus(FilterListStatus&&) = default;
FilterListStatus& FilterListStatus::operator=(FilterListStatus&&) = default;
SecurityCenterModel::SecurityCenterModel(SecurityCenterModel&&) = default;
SecurityCenterModel& SecurityCenterModel::operator=(SecurityCenterModel&&) = default;


FilterListStatus::FilterListStatus(const FilterListStatus&) = default;
FilterListStatus& FilterListStatus::operator=(const FilterListStatus&) = default;
SecurityCenterModel::SecurityCenterModel(const SecurityCenterModel&) = default;
SecurityCenterModel& SecurityCenterModel::operator=(const SecurityCenterModel&) = default;

}  // namespace inweb::adblock
