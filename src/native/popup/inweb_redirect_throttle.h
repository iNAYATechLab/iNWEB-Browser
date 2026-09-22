// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_THROTTLE_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_THROTTLE_H_

#include "content/public/browser/navigation_throttle.h"
#include "content/public/browser/navigation_throttle_registry.h"

namespace content {
class NavigationHandle;
}  // namespace content

namespace inweb::popup {

// Thin content/ wrapper around InwebRedirectGuard (PHASE4-POPUP §2B):
// extracts navigation facts from the NavigationHandle at
// WillStartRequest time and turns a takeover verdict into
// BLOCK_REQUEST with a "redirect blocked" error page. The decision core
// (and all policy) lives in inweb_redirect_guard.cc, which builds and
// unit-tests on the host harness without content/ headers.
//
// Redirect re-evaluation by host is intentionally NOT done here — the
// URL-loader throttle (0006) owns it and its counters.
class InwebRedirectThrottle : public content::NavigationThrottle {
 public:
  // Registers a throttle for top-level navigations only (the guard's
  // frame-context rule applies exclusively to the main frame).
  static void MaybeCreate(content::NavigationThrottleRegistry& registry);

  explicit InwebRedirectThrottle(
      content::NavigationThrottleRegistry& registry);
  ~InwebRedirectThrottle() override;

  InwebRedirectThrottle(const InwebRedirectThrottle&) = delete;
  InwebRedirectThrottle& operator=(const InwebRedirectThrottle&) = delete;

  // content::NavigationThrottle:
  ThrottleCheckResult WillStartRequest() override;
  ThrottleCheckResult WillRedirectRequest() override;
  const char* GetNameForLogging() override;
};

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_REDIRECT_THROTTLE_H_
