// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"

#include <memory>
#include <utility>

namespace inweb::adblock {

InwebAdblockEngineHolder* InwebAdblockEngineHolder::GetInstance() {
  static base::NoDestructor<InwebAdblockEngineHolder> instance;
  return instance.get();
}

InwebAdblockEngineHolder::InwebAdblockEngineHolder()
    : settings_(TrackingProtectionSettings()),
      engine_(std::make_unique<TrackingProtectionEngine>(
          std::vector<ParsedFilterList>(),
          TrackingProtectionSettings())) {}

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

void InwebAdblockEngineHolder::UpdateSettings(
    TrackingProtectionSettings settings) {
  base::AutoLock lock(lock_);
  settings_ = settings;
  engine_->UpdateSettings(std::move(settings));
}

}  // namespace inweb::adblock
