// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_POLICY_H_
#define CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_POLICY_H_

#include <optional>
#include <set>
#include <string>

#include "chrome/android/inweb/extensions/inweb_extension_installer.h"

namespace inweb::extensions {

// An install/update can be staged only after package inspection succeeds.
// Staging never implies enablement: first installs and permission-expanding
// updates require an explicit warning review before the runtime may enable.
enum class PolicyBlockReason {
  kNone,
  kInvalidManifest,
  kVersionNotNewer,
};

struct ExtensionPolicyDecision {
  ExtensionPolicyDecision();
  ~ExtensionPolicyDecision();
  ExtensionPolicyDecision(const ExtensionPolicyDecision&);
  ExtensionPolicyDecision& operator=(const ExtensionPolicyDecision&);
  ExtensionPolicyDecision(ExtensionPolicyDecision&&) noexcept;
  ExtensionPolicyDecision& operator=(ExtensionPolicyDecision&&) noexcept;

  bool may_stage = false;
  bool may_enable = false;
  bool permission_review_required = true;
  bool show_mv2_deprecation_warning = false;
  PolicyBlockReason block_reason = PolicyBlockReason::kInvalidManifest;
  std::set<std::string> added_permissions;
};

// Evaluates the iNWEB sideload/update policy without installing anything.
//
// |installed| is absent for a first install. |reviewed_permissions| is the
// exact set previously accepted by the user. The decision is deterministic
// and contains no runtime capability claim.
ExtensionPolicyDecision EvaluateExtensionPolicy(
    const ExtensionManifest& incoming,
    const std::optional<ExtensionManifest>& installed,
    const std::set<std::string>& reviewed_permissions);

}  // namespace inweb::extensions

#endif  // CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_POLICY_H_
