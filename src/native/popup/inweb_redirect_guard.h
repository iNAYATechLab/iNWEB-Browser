// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_GUARD_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_GUARD_H_

#include <atomic>

#include "base/functional/callback.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "url/gurl.h"

namespace inweb::popup {

// Tab-takeover / tab-under decision, PHASE4-POPUP-PROTECTION §2B.
//
// The abuse pattern: a hidden or ad subframe navigates the top-level page
// without user activation, hijacking the tab. The engine (0005–0007)
// cannot see frame context — it re-evaluates redirects by host in the
// URL-loader throttle (0006, WillRedirectRequest) — so this guard adds
// the frame-context rule.
//
// Policy: a renderer-initiated top-level navigation is a tab takeover and
// is blocked when it has NO user activation (neither a gesture nor a
// transient activation) AND the navigation was started by an ad frame or
// by a frame from a different origin than the page the user is reading.
// Exemptions before blocking (SSO/login flows and friends): the site the
// user is on is allowlisted, or an `@@` exception matches the target.
// Same-tab, same-origin, gestureless script redirects are never touched
// (web-compatibility, §2B); navigations with a genuine user gesture are
// never touched either.
//
// When the engine already decides BLOCK for the target document the guard
// stays out of the way (kProceed): the URL-loader throttle (0006) owns
// host-based blocking and its counters — this guard only owns the
// frame-context rule. When tracking protection is disabled the engine
// reports kPass for everything; the frame-context policy still applies,
// with the allowlist honored (tab-takeover protection is a browser
// policy, not an ad-block feature — same philosophy as the popup guard).
enum class RedirectDecision {
  kProceed,
  kBlockedTabTakeover,
};

// Facts extracted from content::NavigationHandle by the thin throttle
// wrapper (inweb_redirect_throttle.cc). Kept out of the core so the core
// builds and unit-tests on the host harness without content/ headers.
struct NavigationFacts {
  NavigationFacts();

  // Navigation target context.
  bool in_main_frame = false;    // top-level navigation only.
  bool renderer_initiated = false;  // excludes omnibox/bookmarks/UI.
  bool is_same_document = false;    // never blocked (no loader anyway).
  bool is_page_activation = false;  // BFCache/prerender activation.
  // Activation state at navigation start.
  bool has_user_gesture = false;
  bool started_with_transient_activation = false;
  // Initiator attribution.
  bool started_by_ad = false;         // Chromium ad-frame tagging.
  bool has_initiator = false;         // GetInitiatorOrigin() present.
  bool cross_origin_initiator = false;  // initiator origin differs from
                                        // the current main frame's.
  // URLs. |current_page_url| is the main frame's last committed URL (the
  // page the user is reading when the takeover attempt happens).
  GURL target_url;
  GURL current_page_url;
};

// Tab-takeover guard. The decision core is pure and unit-tested with
// scriptable callbacks; the production instance consults the native
// ad-block engine holder (patch 0005–0007) with ResourceType::kDocument
// for the `@@` exception path.
class InwebRedirectGuard {
 public:
  using Decider =
      base::RepeatingCallback<adblock::FilterDecision(const adblock::RequestContext&)>;
  using AllowlistChecker = base::RepeatingCallback<bool(const GURL& page_url)>;

  InwebRedirectGuard(Decider decider, AllowlistChecker allowlist_checker);
  ~InwebRedirectGuard();

  InwebRedirectGuard(const InwebRedirectGuard&) = delete;
  InwebRedirectGuard& operator=(const InwebRedirectGuard&) = delete;

  RedirectDecision ShouldAllowNavigation(const NavigationFacts& facts) const;

  // Real counter for the Security Center (§24) and the blocked-redirect
  // signal. Never fabricated: zero until a real takeover is blocked.
  int blocked_redirect_count() const { return blocked_redirects_.load(); }

 private:
  mutable std::atomic<int> blocked_redirects_{0};
  Decider decider_;
  AllowlistChecker allowlist_checker_;
};

// Process-wide production instance: decider and allowlist checker bound
// to the ad-block engine holder. Lazily created on first use and never
// destroyed.
InwebRedirectGuard* GetRedirectGuard();

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_GUARD_H_
