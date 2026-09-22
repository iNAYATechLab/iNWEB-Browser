// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "chrome/android/inweb/adblock/inweb_default_filter_list.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_parser.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_version.h"

#include <memory>
#include <utility>
#include <vector>

namespace inweb::adblock {

InwebAdblockEngineHolder* InwebAdblockEngineHolder::GetInstance() {
  static base::NoDestructor<InwebAdblockEngineHolder> instance;
  return instance.get();
}

InwebAdblockEngineHolder::InwebAdblockEngineHolder()
    : settings_(TrackingProtectionSettings()) {
  // Provisioning (patch 0007, ADR-041): the embedded starter list is
  // seeded into an in-memory cache with pinned integrity metadata (G-07)
  // and loaded through the standard FilterListManager startup path — the
  // same verified path cached subscriptions will use once the download
  // transport lands. Blocking is therefore active from the first request,
  // with no network dependency.
  const FilterListSource source = FilterListSource::DefaultList();
  ParsedFilterList parsed =
      FilterListParser::Parse(kInwebDefaultFilterList, source.id);
  FilterListMetadata metadata;
  metadata.source_id = source.id;
  metadata.downloaded_at_ms = 0;
  metadata.version = ParseFilterListVersion(kInwebDefaultFilterList).version;
  metadata.rule_count = static_cast<int>(parsed.network_rules.size());
  metadata.content_sha256 = Sha256Hex(kInwebDefaultFilterList);

  cache_ = std::make_unique<MemoryFilterListCache>();
  cache_->Store(source, kInwebDefaultFilterList, metadata);
  manager_ = std::make_unique<FilterListManager>(
      std::vector<FilterListSource>{source},
      /*fetcher=*/nullptr, cache_.get());
  manager_->Startup(/*now_ms=*/0);

  engine_ = std::make_unique<TrackingProtectionEngine>(
      manager_->TakeParsedLists(), settings_);
}

InwebAdblockEngineHolder::~InwebAdblockEngineHolder() = default;

bool InwebAdblockEngineHolder::IsEnabled() const {
  base::AutoLock lock(lock_);
  return settings_.enabled();
}

FilterDecision InwebAdblockEngineHolder::Decide(
    const RequestContext& request) const {
  base::AutoLock lock(lock_);
  return engine_->Decide(request);
}

void InwebAdblockEngineHolder::SetFilterLists(
    std::vector<ParsedFilterList> lists) {
  base::AutoLock lock(lock_);
  engine_ = std::make_unique<TrackingProtectionEngine>(std::move(lists),
                                                       settings_);
}

SecurityCenterModel InwebAdblockEngineHolder::BuildSecurityCenterModel(
    bool enforcement_active) const {
  base::AutoLock lock(lock_);
  return SecurityCenter::Build(settings_, engine_->statistics().Snapshot(),
                               *manager_, enforcement_active);
}

void InwebAdblockEngineHolder::UpdateSettings(
    TrackingProtectionSettings settings) {
  base::AutoLock lock(lock_);
  settings_ = settings;
  engine_->UpdateSettings(std::move(settings));
}

}  // namespace inweb::adblock
