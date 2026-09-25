// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/extensions/inweb_extension_policy.h"

#include <utility>

#include "testing/gtest/include/gtest/gtest.h"

namespace inweb::extensions {
namespace {

ExtensionManifest Manifest(
    const std::string& version = "1.0",
    int manifest_version = 3,
    std::vector<std::string> permissions = {"storage"}) {
  ExtensionManifest manifest;
  manifest.name = "Test Extension";
  manifest.version = version;
  manifest.manifest_version = manifest_version;
  manifest.permissions = std::move(permissions);
  return manifest;
}

TEST(InwebExtensionPolicyTest, FirstInstallStagesButNeverSilentlyEnables) {
  const ExtensionPolicyDecision decision =
      EvaluateExtensionPolicy(Manifest(), std::nullopt, {});
  EXPECT_TRUE(decision.may_stage);
  EXPECT_FALSE(decision.may_enable);
  EXPECT_TRUE(decision.permission_review_required);
  EXPECT_EQ(PolicyBlockReason::kNone, decision.block_reason);
  EXPECT_EQ(std::set<std::string>({"storage"}), decision.added_permissions);
}

TEST(InwebExtensionPolicyTest, InvalidManifestAndNonNewerUpdateAreBlocked) {
  ExtensionManifest invalid = Manifest();
  invalid.name.clear();
  EXPECT_EQ(PolicyBlockReason::kInvalidManifest,
            EvaluateExtensionPolicy(invalid, std::nullopt, {}).block_reason);

  const ExtensionManifest installed = Manifest("2.0");
  const ExtensionPolicyDecision same =
      EvaluateExtensionPolicy(Manifest("2.0"), installed, {"storage"});
  EXPECT_FALSE(same.may_stage);
  EXPECT_EQ(PolicyBlockReason::kVersionNotNewer, same.block_reason);

  const ExtensionPolicyDecision older =
      EvaluateExtensionPolicy(Manifest("1.9"), installed, {"storage"});
  EXPECT_FALSE(older.may_stage);
  EXPECT_EQ(PolicyBlockReason::kVersionNotNewer, older.block_reason);
}

TEST(InwebExtensionPolicyTest, UpdateWithoutNewPermissionsMayRemainEnabled) {
  const ExtensionManifest installed = Manifest("1.0", 3, {"storage", "tabs"});
  const ExtensionPolicyDecision decision = EvaluateExtensionPolicy(
      Manifest("1.1", 3, {"storage"}), installed, {"storage", "tabs"});
  EXPECT_TRUE(decision.may_stage);
  EXPECT_TRUE(decision.may_enable);
  EXPECT_FALSE(decision.permission_review_required);
  EXPECT_TRUE(decision.added_permissions.empty());
}

TEST(InwebExtensionPolicyTest, AddedPermissionForcesReviewBeforeEnable) {
  const ExtensionManifest installed = Manifest("1.0", 3, {"storage"});
  const ExtensionPolicyDecision decision = EvaluateExtensionPolicy(
      Manifest("2.0", 3, {"storage", "tabs"}), installed, {"storage"});
  EXPECT_TRUE(decision.may_stage);
  EXPECT_FALSE(decision.may_enable);
  EXPECT_TRUE(decision.permission_review_required);
  EXPECT_EQ(std::set<std::string>({"tabs"}), decision.added_permissions);
}

TEST(InwebExtensionPolicyTest, ManifestV2AlwaysCarriesDeprecationWarning) {
  const ExtensionPolicyDecision decision =
      EvaluateExtensionPolicy(Manifest("1.0", 2), std::nullopt, {});
  EXPECT_TRUE(decision.may_stage);
  EXPECT_TRUE(decision.show_mv2_deprecation_warning);
  EXPECT_FALSE(decision.may_enable);
}

}  // namespace
}  // namespace inweb::extensions
