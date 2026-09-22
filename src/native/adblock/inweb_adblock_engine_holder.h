// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_ENGINE_HOLDER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_ENGINE_HOLDER_H_

#include <memory>
#include <vector>

#include "base/no_destructor.h"
#include "base/synchronization/lock.h"
#include "base/thread_annotations.h"
#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_manager.h"
#include "chrome/android/inweb/adblock/inweb_security_center.h"
#include "chrome/android/inweb/adblock/inweb_tracking_protection_engine.h"

namespace inweb::adblock {

// Process-wide owner of the tracking-protection engine (v1: one engine for
// the whole browser; ADR-040 native port). Thread-safe: Decide() may be
// called from any sequence (the URL-loader throttle runs it on the thread
// pool); rule-list and settings updates arrive from the provisioning paths
// (patch 0007).
class InwebAdblockEngineHolder {
 public:
  static InwebAdblockEngineHolder* GetInstance();

  // True when filtering is enabled. Lets throttle registration skip all
  // per-request work while tracking protection is off.
  bool IsEnabled() const;

  // Thread-safe; callable from any sequence. Holds the holder lock for the
  // duration of the (v1 full-scan) decision — see ADR-013/ADR-040 notes.
  FilterDecision Decide(const RequestContext& request) const;

  // Replaces the rule lists, keeping current settings.
  void SetFilterLists(std::vector<ParsedFilterList> lists);

  // Replaces settings, keeping current rule lists.
  void UpdateSettings(TrackingProtectionSettings settings);

  // Security Center read path (§24): builds the dashboard model from the
  // real settings, the real engine statistics, and the real list state.
  SecurityCenterModel BuildSecurityCenterModel(bool enforcement_active) const;

 private:
  friend class base::NoDestructor<InwebAdblockEngineHolder>;

 public:
  // Public for unit tests (direct instances avoid polluting the process
  // singleton). Production code must go through GetInstance().
  InwebAdblockEngineHolder();
  ~InwebAdblockEngineHolder();

  mutable base::Lock lock_;
  TrackingProtectionSettings settings_ GUARDED_BY(lock_);
  std::unique_ptr<MemoryFilterListCache> cache_ GUARDED_BY(lock_);
  std::unique_ptr<FilterListManager> manager_ GUARDED_BY(lock_);
  std::unique_ptr<TrackingProtectionEngine> engine_ GUARDED_BY(lock_);
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_ENGINE_HOLDER_H_
