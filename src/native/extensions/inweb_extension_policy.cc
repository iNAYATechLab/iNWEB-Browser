// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/extensions/inweb_extension_policy.h"

namespace inweb::extensions {

ExtensionPolicyDecision::ExtensionPolicyDecision() = default;
ExtensionPolicyDecision::~ExtensionPolicyDecision() = default;
ExtensionPolicyDecision::ExtensionPolicyDecision(
    const ExtensionPolicyDecision&) = default;
ExtensionPolicyDecision& ExtensionPolicyDecision::operator=(
    const ExtensionPolicyDecision&) = default;
ExtensionPolicyDecision::ExtensionPolicyDecision(
    ExtensionPolicyDecision&&) noexcept = default;
ExtensionPolicyDecision& ExtensionPolicyDecision::operator=(
    ExtensionPolicyDecision&&) noexcept = default;

namespace {

bool IsValidManifest(const ExtensionManifest& manifest) {
  return !manifest.parse_error && !manifest.name.empty() &&
         CompareVersions(manifest.version, manifest.version).has_value() &&
         (manifest.manifest_version == 2 || manifest.manifest_version == 3);
}

}  // namespace

ExtensionPolicyDecision EvaluateExtensionPolicy(
    const ExtensionManifest& incoming,
    const std::optional<ExtensionManifest>& installed,
    const std::set<std::string>& reviewed_permissions) {
  ExtensionPolicyDecision decision;
  decision.show_mv2_deprecation_warning = incoming.manifest_version == 2;

  if (!IsValidManifest(incoming)) {
    decision.block_reason = PolicyBlockReason::kInvalidManifest;
    return decision;
  }

  if (installed) {
    const std::optional<int> order =
        CompareVersions(installed->version, incoming.version);
    if (!order || *order >= 0) {
      decision.block_reason = PolicyBlockReason::kVersionNotNewer;
      return decision;
    }
  }

  decision.may_stage = true;
  decision.block_reason = PolicyBlockReason::kNone;
  for (const std::string& permission : incoming.permissions) {
    if (!reviewed_permissions.contains(permission)) {
      decision.added_permissions.insert(permission);
    }
  }
  // Every first install needs an explicit package review, even when the
  // manifest requests no warning-producing permissions. Updates require a
  // new review only when they expand the accepted permission set.
  decision.permission_review_required =
      !installed.has_value() || !decision.added_permissions.empty();
  decision.may_enable = !decision.permission_review_required;
  return decision;
}

}  // namespace inweb::extensions
