// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/popup/inweb_notification_policy_ui_selector.h"

#include <utility>

#include "chrome/android/inweb/popup/inweb_notification_policy.h"
#include "components/content_settings/core/common/content_settings_types.h"
#include "components/permissions/permission_request.h"

namespace inweb::popup {

InwebNotificationPolicyUiSelector::InwebNotificationPolicyUiSelector() =
    default;

InwebNotificationPolicyUiSelector::~InwebNotificationPolicyUiSelector() =
    default;

void InwebNotificationPolicyUiSelector::SelectUiToUse(
    content::WebContents* web_contents,
    permissions::PermissionRequest* request,
    DecisionMadeCallback callback) {
  NotificationPromptFacts facts;
  facts.is_notifications_request =
      request->GetContentSettingsType() ==
      ContentSettingsType::NOTIFICATIONS;

  if (GetNotificationPolicy()->ShouldQuietPrompt(facts) ==
      NotificationPromptDecision::kQuietPrompt) {
    std::move(callback).Run(Decision::UseQuietUi(
        QuietUiReason::kEnabledInPrefs, Decision::ShowNoWarning()));
    return;
  }
  std::move(callback).Run(Decision::UseNormalUiAndShowNoWarning());
}

void InwebNotificationPolicyUiSelector::Cancel() {}

bool InwebNotificationPolicyUiSelector::IsPermissionRequestSupported(
    permissions::RequestType request_type) {
  return request_type == permissions::RequestType::kNotifications;
}

}  // namespace inweb::popup
