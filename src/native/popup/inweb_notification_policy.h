// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_H_

#include <atomic>

namespace inweb::popup {

// Abusive-notification-quieting policy, PHASE4-POPUP-PROTECTION §2C.
//
// Policy: notification permission prompts are QUIETED (no modal — the
// request surfaces as a quiet chip the user can expand) on sites
// without meaningful engagement, are explained by the quiet chip
// itself, and are NEVER auto-granted: a grant requires an explicit
// user action on the expanded prompt.
//
// v1 honest scope (stricter default, conservative superset): every
// notification prompt in the ASK state is quieted. No engagement
// signal is wired yet, so no site is treated as "meaningfully
// engaged" — the engagement-exemption slot below is the documented
// extension point for the settings/engagement patch, and until it
// exists this policy never loosens the default. Chromium's own
// quiet-messaging infrastructure carries the UI
// (PermissionRequestManager quiet chip + the Android quiet prompt
// model); this class owns only the decision table and the real §24
// counter. Repeated-deny auto-blocking is Chromium's existing
// PermissionDecisionAutoBlocker and is left untouched.
//
// Honest boundary (§2C/§6): iNWEB makes NO Safe-Browsing-class
// reputation claim — quieting is policy-based only.
enum class NotificationPromptDecision {
  kNormalPrompt,
  kQuietPrompt,
};

// Facts about a permission request reaching the prompt stage, as seen
// by the policy. Kept free of content/ types so the core builds and
// unit-tests on the host harness.
struct NotificationPromptFacts {
  // True when the request is the Web Notifications permission
  // (RequestType::kNotifications / ContentSettingsType::NOTIFICATIONS).
  bool is_notifications_request = false;
  // Extension point (documented, unwired in v1): true would mean the
  // site has meaningful engagement and may get the normal prompt.
  bool has_meaningful_engagement = false;
};

// Notification-prompt policy core. Pure table + real counter; the thin
// permissions/ wrapper is inweb_notification_policy_ui_selector.cc.
class InwebNotificationPolicy {
 public:
  InwebNotificationPolicy() = default;
  ~InwebNotificationPolicy() = default;

  InwebNotificationPolicy(const InwebNotificationPolicy&) = delete;
  InwebNotificationPolicy& operator=(const InwebNotificationPolicy&) = delete;

  NotificationPromptDecision ShouldQuietPrompt(
      const NotificationPromptFacts& facts) const;

  // Real counter for the Security Center (§24). Never fabricated:
  // zero until a real notification prompt is quieted.
  int quieted_prompt_count() const { return quieted_prompts_.load(); }

 private:
  mutable std::atomic<int> quieted_prompts_{0};
};

// Process-wide production instance.
InwebNotificationPolicy* GetNotificationPolicy();

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_H_
