// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_redirect_throttle.h"

#include <string>
#include <utility>

#include "chrome/android/inweb/popup/inweb_redirect_guard.h"
#include "content/public/browser/navigation_handle.h"
#include "content/public/browser/render_frame_host.h"
#include "content/public/browser/web_contents.h"
#include "net/base/net_errors.h"
#include "url/gurl.h"
#include "url/origin.h"

namespace inweb::popup {

namespace {

// Static, script-free error page for blocked tab takeovers. Localization
// is deferred to the PHASE10 localization patch; the page intentionally
// avoids script so it renders identically in every error-page mode.
const char kBlockedRedirectErrorPage[] =
    "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
    "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
    "<title>Redirect blocked</title></head>"
    "<body style=\"font-family:Roboto,system-ui,Arial,sans-serif;"
    "background:#fafafa;color:#202124;margin:0;padding:48px 24px;"
    "text-align:center;\">"
    "<div style=\"font-size:56px;line-height:1;\">&#128737;&#65039;</div>"
    "<h1 style=\"font-size:20px;font-weight:500;margin:16px 0 8px;\">"
    "Redirect blocked</h1>"
    "<p style=\"font-size:14px;color:#5f6368;margin:0 0 8px;\">"
    "iNWEB stopped a page from redirecting you without your permission."
    "</p>"
    "<p style=\"font-size:14px;color:#5f6368;margin:0;\">"
    "Use the back button to return to the page you were reading.</p>"
    "</body></html>";

// Extracts the guard's decision facts from a NavigationHandle.
NavigationFacts FactsFromNavigation(content::NavigationHandle* handle) {
  NavigationFacts facts;
  facts.in_main_frame = handle->IsInMainFrame();
  facts.renderer_initiated = handle->IsRendererInitiated();
  facts.is_same_document = handle->IsSameDocument();
  facts.is_page_activation = handle->IsPageActivation();
  facts.has_user_gesture = handle->HasUserGesture();
  facts.started_with_transient_activation =
      handle->StartedWithTransientActivation();
  facts.started_by_ad = handle->StartedByAd();
  facts.target_url = handle->GetURL();

  const std::optional<url::Origin>& initiator = handle->GetInitiatorOrigin();
  facts.has_initiator = initiator.has_value();

  content::WebContents* web_contents = handle->GetWebContents();
  content::RenderFrameHost* main_frame =
      web_contents ? web_contents->GetPrimaryMainFrame() : nullptr;
  if (main_frame) {
    facts.current_page_url = main_frame->GetLastCommittedURL();
    // Frame-context rule: an initiator whose origin differs from the
    // page the user is reading is steering the tab from outside it.
    facts.cross_origin_initiator =
        initiator.has_value() && (*initiator != main_frame->GetLastCommittedOrigin());
  }
  return facts;
}

}  // namespace

void InwebRedirectThrottle::MaybeCreate(
    content::NavigationThrottleRegistry& registry) {
  // The frame-context rule applies exclusively to top-level navigations;
  // subframe loads are the engine's territory (0005–0007).
  if (!registry.GetNavigationHandle().IsInMainFrame()) {
    return;
  }
  registry.AddThrottle(
      std::make_unique<InwebRedirectThrottle>(registry));
}

InwebRedirectThrottle::InwebRedirectThrottle(
    content::NavigationThrottleRegistry& registry)
    : content::NavigationThrottle(registry) {}

InwebRedirectThrottle::~InwebRedirectThrottle() = default;

content::NavigationThrottle::ThrottleCheckResult
InwebRedirectThrottle::WillStartRequest() {
  if (GetRedirectGuard()->ShouldAllowNavigation(
          FactsFromNavigation(navigation_handle())) ==
      RedirectDecision::kProceed) {
    return content::NavigationThrottle::PROCEED;
  }
  return content::NavigationThrottle::ThrottleCheckResult(
      content::NavigationThrottle::BLOCK_REQUEST,
      net::ERR_BLOCKED_BY_CLIENT,
      std::string(kBlockedRedirectErrorPage));
}

content::NavigationThrottle::ThrottleCheckResult
InwebRedirectThrottle::WillRedirectRequest() {
  // Redirect re-evaluation toward engine-blocked hosts is owned by the
  // URL-loader throttle (0006, WillRedirectRequest) — nothing to do
  // here; the frame-context verdict was already rendered at start.
  return content::NavigationThrottle::PROCEED;
}

const char* InwebRedirectThrottle::GetNameForLogging() {
  return "InwebRedirectThrottle";
}

}  // namespace inweb::popup
