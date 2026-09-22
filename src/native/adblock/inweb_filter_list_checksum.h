// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CHECKSUM_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CHECKSUM_H_

#include <string>

namespace inweb::adblock {

// Lowercase-hex SHA-256 of the body's bytes. Content-integrity pinning for
// cached filter lists (G-07): computed when a body is downloaded and
// re-verified whenever the cached copy is loaded. This is integrity
// pinning, NOT a publisher signature (§57). Platform crypto only —
// crypto::SHA256HashString (ADR-025: no custom cryptography). Ported from
// the Kotlin reference (lists/FilterListChecksum.kt).
std::string Sha256Hex(const std::string& body);

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_CHECKSUM_H_
