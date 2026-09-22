// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_notification_policy.h"

#include "base/no_destructor.h"

namespace inweb::popup {

NotificationPromptDecision InwebNotificationPolicy::ShouldQuietPrompt(
    const NotificationPromptFacts& facts) const {
  // Only notification prompts are ours to quiet; every other
  // permission keeps Chromium's default prompt behavior.
  if (!facts.is_notifications_request) {
    return NotificationPromptDecision::kNormalPrompt;
  }
  // Engagement exemption (unwired in v1 — conservative superset: all
  // ASK-state notification prompts quieted). The flag exists so the
  // settings/engagement patch can loosen the default deliberately,
  // never silently.
  if (facts.has_meaningful_engagement) {
    return NotificationPromptDecision::kNormalPrompt;
  }
  quieted_prompts_.fetch_add(1, std::memory_order_relaxed);
  return NotificationPromptDecision::kQuietPrompt;
}

InwebNotificationPolicy* GetNotificationPolicy() {
  static base::NoDestructor<InwebNotificationPolicy> policy;
  return policy.get();
}

}  // namespace inweb::popup
