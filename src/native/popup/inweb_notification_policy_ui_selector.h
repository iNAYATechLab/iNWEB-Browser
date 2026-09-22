// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_UI_SELECTOR_H_
#define CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_UI_SELECTOR_H_

#include "components/permissions/prediction_service/permission_ui_selector.h"

namespace inweb::popup {

// Thin permissions/ wrapper around InwebNotificationPolicy
// (PHASE4-POPUP §2C): turns the policy verdict into a
// PermissionUiSelector::Decision. Registered first in
// ChromePermissionsClient::CreatePermissionUiSelectors so iNWEB policy
// takes precedence; decisions are synchronous, so Cancel() is a no-op.
// kEnabledInPrefs is the honest QuietUiReason — the quiet messaging
// behavior Chromium already ships, enabled as iNWEB's default policy.
class InwebNotificationPolicyUiSelector
    : public permissions::PermissionUiSelector {
 public:
  InwebNotificationPolicyUiSelector();
  ~InwebNotificationPolicyUiSelector() override;

  InwebNotificationPolicyUiSelector(const InwebNotificationPolicyUiSelector&) =
      delete;
  InwebNotificationPolicyUiSelector& operator=(
      const InwebNotificationPolicyUiSelector&) = delete;

  // permissions::PermissionUiSelector:
  void SelectUiToUse(content::WebContents* web_contents,
                     permissions::PermissionRequest* request,
                     DecisionMadeCallback callback) override;
  void Cancel() override;
  bool IsPermissionRequestSupported(
      permissions::RequestType request_type) override;
};

}  // namespace inweb::popup

#endif  // CHROME_ANDROID_INWEB_POPUP_INWEB_NOTIFICATION_POLICY_UI_SELECTOR_H_
