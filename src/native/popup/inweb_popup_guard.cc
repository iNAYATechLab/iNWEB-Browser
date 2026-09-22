// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_popup_guard.h"

#include <utility>

#include "base/functional/bind.h"
#include "base/no_destructor.h"
#include "chrome/android/inweb/adblock/inweb_adblock_engine_holder.h"
#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_rule_matcher.h"

namespace inweb::popup {

InwebPopupGuard::InwebPopupGuard(Decider decider,
                                 AllowlistChecker allowlist_checker)
    : decider_(std::move(decider)),
      allowlist_checker_(std::move(allowlist_checker)) {}

InwebPopupGuard::~InwebPopupGuard() = default;

PopupDecision InwebPopupGuard::ShouldAllowPopup(const GURL& target_url,
                                                const GURL& opener_url,
                                                bool user_gesture) const {
  adblock::RequestContext context{target_url, opener_url,
                                  adblock::ResourceType::kPopup};
  const adblock::FilterDecision decision = decider_.Run(context);

  if (decision.action == adblock::FilterAction::kAllow) {
    // Site allowlist or an `@@` exception explicitly permits this popup.
    return PopupDecision::kAllow;
  }

  if (decision.action == adblock::FilterAction::kBlock) {
    engine_blocked_.fetch_add(1);
    blocked_popups_.fetch_add(1);
    return PopupDecision::kBlockedByEngine;
  }

  // kPass: no rule spoke. With a gesture the window opens; without one
  // the default popup policy applies, except for allowlisted sites.
  if (user_gesture) {
    return PopupDecision::kAllow;
  }
  if (allowlist_checker_.Run(opener_url)) {
    return PopupDecision::kAllow;
  }
  blocked_popups_.fetch_add(1);
  return PopupDecision::kBlockedNoGesture;
}

InwebPopupGuard* GetPopupGuard() {
  static base::NoDestructor<InwebPopupGuard> guard(
      base::BindRepeating([](const adblock::RequestContext& context) {
        return adblock::InwebAdblockEngineHolder::GetInstance()->Decide(
            context);
      }),
      base::BindRepeating([](const GURL& opener_url) {
        const std::string host = adblock::RuleMatcher::HostOf(opener_url);
        if (host.empty()) {
          return false;
        }
        return adblock::InwebAdblockEngineHolder::GetInstance()
            ->IsSiteAllowlisted(host);
      }));
  return guard.get();
}

}  // namespace inweb::popup
