// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — notification policy unit tests (PHASE4-POPUP §2C
// policy table): notification prompts quieted by default, never
// auto-granted (the quiet UI requires explicit user action), other
// permissions untouched, real §24 counter.

#include "chrome/android/inweb/popup/inweb_notification_policy.h"

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::popup {

namespace {

NotificationPromptFacts NotificationRequest() {
  NotificationPromptFacts facts;
  facts.is_notifications_request = true;
  return facts;
}

}  // namespace

TEST(InwebNotificationPolicyTest, NotificationPromptsAreQuietedByDefault) {
  InwebNotificationPolicy policy;
  EXPECT_EQ(policy.ShouldQuietPrompt(NotificationRequest()),
            NotificationPromptDecision::kQuietPrompt);
  EXPECT_EQ(policy.quieted_prompt_count(), 1);
}

TEST(InwebNotificationPolicyTest, OtherPermissionsAreNotOursToQuiet) {
  InwebNotificationPolicy policy;
  NotificationPromptFacts facts;  // geolocation, camera, mic, ...
  facts.is_notifications_request = false;
  EXPECT_EQ(policy.ShouldQuietPrompt(facts),
            NotificationPromptDecision::kNormalPrompt);
  EXPECT_EQ(policy.quieted_prompt_count(), 0);
}

TEST(InwebNotificationPolicyTest, EngagementExemptionSlotIsWiredButUnset) {
  // The engagement exemption is the documented extension point; until
  // the settings/engagement patch sets it from a real signal, the
  // default stays strict (no fabricated engagement).
  InwebNotificationPolicy policy;
  NotificationPromptFacts facts = NotificationRequest();
  facts.has_meaningful_engagement = true;
  EXPECT_EQ(policy.ShouldQuietPrompt(facts),
            NotificationPromptDecision::kNormalPrompt);
  EXPECT_EQ(policy.quieted_prompt_count(), 0);
}

TEST(InwebNotificationPolicyTest, CounterCountsOnlyRealQuiets) {
  InwebNotificationPolicy policy;
  NotificationPromptFacts other;
  EXPECT_EQ(policy.ShouldQuietPrompt(other), NotificationPromptDecision::kNormalPrompt);
  EXPECT_EQ(policy.ShouldQuietPrompt(NotificationRequest()),
            NotificationPromptDecision::kQuietPrompt);
  EXPECT_EQ(policy.ShouldQuietPrompt(NotificationRequest()),
            NotificationPromptDecision::kQuietPrompt);
  EXPECT_EQ(policy.quieted_prompt_count(), 2);
}

TEST(InwebNotificationPolicyTest, ProductionInstanceIsUsable) {
  // The process-wide instance answers with the same policy; the real
  // counter lives there and starts at zero (§24 — no fabricated
  // numbers). Repeated calls with the same instance accumulate.
  InwebNotificationPolicy* policy = GetNotificationPolicy();
  const int before = policy->quieted_prompt_count();
  EXPECT_EQ(policy->ShouldQuietPrompt(NotificationRequest()),
            NotificationPromptDecision::kQuietPrompt);
  EXPECT_EQ(policy->quieted_prompt_count(), before + 1);
  EXPECT_EQ(policy, GetNotificationPolicy());  // stable singleton
}

}  // namespace inweb::popup
