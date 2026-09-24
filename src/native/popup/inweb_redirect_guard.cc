// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_redirect_guard.h"

#include "base/functional/bind.h"
#include "base/no_destructor.h"
#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "chrome/android/inweb/adblock/inweb_rule_matcher.h"

namespace inweb::popup {

namespace {

// Extracts the registrable host of |url| through the same matcher the
// engine uses (so allowlist semantics match the rest of the product).
std::string HostOf(const GURL& url) {
  return adblock::RuleMatcher::HostOf(url);
}

}  // namespace

InwebRedirectGuard::InwebRedirectGuard(Decider decider,
                                       AllowlistChecker allowlist_checker)
    : decider_(std::move(decider)),
      allowlist_checker_(std::move(allowlist_checker)) {}

InwebRedirectGuard::~InwebRedirectGuard() = default;

RedirectDecision InwebRedirectGuard::ShouldAllowNavigation(
    const NavigationFacts& facts) const {
  // Only renderer-initiated top-level navigations are ours to judge.
  // Browser-initiated navigations (omnibox, bookmarks, UI buttons) carry
  // explicit user intent by construction.
  if (!facts.in_main_frame || !facts.renderer_initiated) {
    return RedirectDecision::kProceed;
  }
  // Document-preserving transitions never reach a URL loader; the
  // header contract says throttles may not even run for them.
  if (facts.is_same_document || facts.is_page_activation) {
    return RedirectDecision::kProceed;
  }
  // Genuine user activation on the initiating frame: never touched
  // (§2B, web-compatibility).
  if (facts.has_user_gesture || facts.started_with_transient_activation) {
    return RedirectDecision::kProceed;
  }
  // No initiator attribution: cannot attribute the navigation to a
  // subframe — do not block what we cannot see.
  if (!facts.has_initiator) {
    return RedirectDecision::kProceed;
  }
  // Frame-context rule: an ad frame or a cross-origin frame steering
  // the top-level page is a takeover attempt.
  const bool takeover =
      facts.started_by_ad || facts.cross_origin_initiator;
  if (!takeover) {
    return RedirectDecision::kProceed;
  }
  // Exemption 1: the site the user is on is allowlisted (single shields
  // list, PHASE4-POPUP §1 — SSO frames live on allowlisted sites).
  if (allowlist_checker_ && allowlist_checker_.Run(facts.current_page_url)) {
    return RedirectDecision::kProceed;
  }
  // Exemption 2 / layering: consult the engine for the target document.
  if (decider_) {
    const adblock::RequestContext context{
        facts.target_url, facts.current_page_url,
        adblock::ResourceType::kDocument};
    const adblock::FilterDecision decision = decider_.Run(context);
    // `@@` exception on the target (identity-provider redirect targets):
    // allowed.
    if (decision.action == adblock::FilterAction::kAllow) {
      return RedirectDecision::kProceed;
    }
    // The engine already blocks this document by rule: the URL-loader
    // throttle (0006) owns that decision and its counters — stay out.
    if (decision.action == adblock::FilterAction::kBlock) {
      return RedirectDecision::kProceed;
    }
    // kPass: no engine opinion; the frame-context rule decides.
  }
  blocked_redirects_.fetch_add(1, std::memory_order_relaxed);
  return RedirectDecision::kBlockedTabTakeover;
}

InwebRedirectGuard* GetRedirectGuard() {
  static base::NoDestructor<InwebRedirectGuard> guard(
      base::BindRepeating([](const adblock::RequestContext& context) {
        return adblock::InwebAdblockEngineHolder::GetInstance()->Decide(
            context);
      }),
      base::BindRepeating([](const GURL& page_url) {
        const std::string host = HostOf(page_url);
        if (host.empty()) {
          return false;
        }
        return adblock::InwebAdblockEngineHolder::GetInstance()
            ->IsSiteAllowlisted(host);
      }));
  return guard.get();
}


// Out-of-line ctor definition (chromium-style fallout fix).
NavigationFacts::NavigationFacts() = default;

}  // namespace inweb::popup
