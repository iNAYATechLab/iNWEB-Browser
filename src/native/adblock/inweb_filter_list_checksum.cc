// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"

#include <string>

#include "base/containers/span.h"
#include "base/strings/string_number_conversions.h"
#include "crypto/sha2.h"

namespace inweb::adblock {

std::string Sha256Hex(const std::string& body) {
  const std::string digest = crypto::SHA256HashString(body);
  // base::HexEncodeLower = the lowercase hex sha256 convention; no raw
  // C-array indexing anywhere (unsafe-buffers-clean).
  return base::HexEncodeLower(base::as_byte_span(digest));
}

}  // namespace inweb::adblock
