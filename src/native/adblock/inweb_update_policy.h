// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_UPDATE_POLICY_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_UPDATE_POLICY_H_

#include <optional>

#include "chrome/android/inweb/adblock/inweb_filter_list_cache.h"

namespace inweb::adblock {

// Update-scheduling *decision* model. The actual timer lives in the host
// layer and calls FilterListManager::Refresh when this policy says a list
// is due — the policy itself never runs background work. Ported from the
// Kotlin reference (lists/UpdatePolicy.kt).
struct UpdatePolicy {
  bool enabled = true;
  // Download a list on startup when no cached copy exists.
  bool fetch_on_startup = true;
  // Minimum age of the last confirmed download before a refresh is due.
  int64_t refresh_interval_ms = 24LL * 60 * 60 * 1000;

  bool IsRefreshDue(const std::optional<FilterListMetadata>& metadata,
                    int64_t now_ms) const {
    if (!enabled) {
      return false;
    }
    if (!metadata) {
      return true;
    }
    return now_ms - metadata->downloaded_at_ms >= refresh_interval_ms;
  }
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_UPDATE_POLICY_H_
