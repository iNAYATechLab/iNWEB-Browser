// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_INSTALLER_H_
#define CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_INSTALLER_H_

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace inweb::extensions {

// The fields iNWEB reads from a manifest.json before offering install.
// The full manifest is surfaced too (raw), because the management UI
// shows the real permission list — never a summary we invented.
struct ExtensionManifest {
  ExtensionManifest();
  ~ExtensionManifest();
  ExtensionManifest(const ExtensionManifest&);
  ExtensionManifest& operator=(const ExtensionManifest&);
  ExtensionManifest(ExtensionManifest&&);
  ExtensionManifest& operator=(ExtensionManifest&&);

  std::string name;
  std::string version;        // Chrome format: dot-separated integers.
  int manifest_version = 0;   // 2 or 3 supported (PHASE6 MV2/MV3 policy)
  std::vector<std::string> permissions;
  std::optional<std::string> parse_error;  // set when parsing failed
};

// Structural parse of the manifest JSON subset iNWEB needs. Strict,
// small, self-contained (no Chromium JSON dependency — it must build
// on the host harness unchanged). Limits, documented not hidden:
// strings/ints/bools/arrays/objects; unicode escapes; duplicate keys
// keep the last value; no floats needed for these fields (a float
// where an int is expected is a parse error).
ExtensionManifest ParseManifest(const std::string& json);

// Compares Chrome-format versions ("1.2.3.4", 1–4 numeric components,
// missing components are 0). Returns: -1 (a<b), 0 (equal), 1 (a>b).
// nullopt when either string is not a valid version.
std::optional<int> CompareVersions(const std::string& a, const std::string& b);

// Result of inspecting a sideloaded package.
enum class PackageVerdict {
  kValid,
  kNotAnExtension,   // ZIP parsed but manifest missing/unreadable
  kCorrupt,          // container/ZIP corruption
};

struct InspectionResult {
  PackageVerdict verdict = PackageVerdict::kCorrupt;
  std::string error;            // human-readable, shown in the UI as-is
  ExtensionManifest manifest;
  bool is_crx = false;          // came wrapped in a CRX3 container
  // The raw manifest.json bytes (the UI may show the source of truth).
  std::string raw_manifest;
};

// Inspects a sideloaded package: CRX3 container (structural) or plain
// ZIP → CRC-verified manifest.json → manifest parse + required-field
// validation (name, valid version, manifest_version 2 or 3).
InspectionResult InspectPackage(const std::vector<uint8_t>& package);

// Sideload update policy (PHASE6 §4 update model: manual re-sideload
// with version comparison against the installed copy).
enum class UpdateDecision {
  kAcceptFirstInstall,  // nothing installed
  kAcceptNewerVersion,
  kRejectOlderVersion,  // same or older — the UI explains, never silently
};

UpdateDecision DecideUpdate(const std::optional<std::string>& installed,
                            const std::string& incoming);

}  // namespace inweb::extensions

#endif  // CHROME_ANDROID_INWEB_EXTENSIONS_INWEB_EXTENSION_INSTALLER_H_
