// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_POPUP_GUARD_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_POPUP_GUARD_H_

#include <atomic>
#include <string>

#include "base/functional/callback.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "url/gurl.h"

namespace inweb::popup {

// Popup-blocking decision, PHASE4-POPUP-PROTECTION §2A.
//
// Policy: a new window/tab creation without a user activation on the
// initiating frame is a popup and is blocked by default (browser popup
// policy, Chrome-parity) — UNLESS the opener site is allowlisted, an
// `@@` exception matches, or the user explicitly re-opens it from the
// blocked-popup UI (that path is a UI patch; the guard only exposes the
// counters). With a user gesture the window opens — unless the engine
// blocks it (a `$popup` / host rule), so tracker popups are still caught.
//
// When tracking protection is disabled the engine reports kPass for
// everything; the gestureless default policy still applies (popups are a
// browser policy, not an ad-block feature), with the allowlist honored —
// the guard consults the allowlist through a dedicated callback.
enum class PopupDecision {
  kAllow,
  kBlockedNoGesture,   // default policy: popup without user activation
  kBlockedByEngine,    // engine BLOCK decision ($popup / host rule)
};

// Window-creation guard. The decision core is pure and unit-tested with
// scriptable callbacks; the production instance consults the native
// ad-block engine holder (patch 0005–0007) with ResourceType::kPopup.
class InwebPopupGuard {
 public:
  using Decider =
      base::RepeatingCallback<adblock::FilterDecision(const adblock::RequestContext&)>;
  using AllowlistChecker = base::RepeatingCallback<bool(const GURL& opener_url)>;

  InwebPopupGuard(Decider decider, AllowlistChecker allowlist_checker);
  ~InwebPopupGuard();

  InwebPopupGuard(const InwebPopupGuard&) = delete;
  InwebPopupGuard& operator=(const InwebPopupGuard&) = delete;

  // |opener_url|: the initiating frame's last committed URL.
  PopupDecision ShouldAllowPopup(const GURL& target_url,
                                 const GURL& opener_url,
                                 bool user_gesture) const;

  // Real counters for the Security Center (§24) and the blocked-popup UI
  // chip signal. Never fabricated: zero until real popups are blocked.
  int blocked_popup_count() const { return blocked_popups_.load(); }
  int engine_blocked_count() const { return engine_blocked_.load(); }

 private:
  mutable std::atomic<int> blocked_popups_{0};
  mutable std::atomic<int> engine_blocked_{0};
  Decider decider_;
  AllowlistChecker allowlist_checker_;
};

// Process-wide production instance: decider and allowlist checker bound
// to the ad-block engine holder. Returns nullptr-safe raw pointer; the
// instance is lazily created on first use and never destroyed.
InwebPopupGuard* GetPopupGuard();

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_POPUP_GUARD_H_
