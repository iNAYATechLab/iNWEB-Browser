// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_filter_list_checksum.h"

#include <string>

#include "crypto/sha2.h"

namespace inweb::adblock {

std::string Sha256Hex(const std::string& body) {
  const std::string digest = crypto::SHA256HashString(body);
  static const char kHex[] = "0123456789abcdef";
  std::string hex;
  hex.reserve(digest.size() * 2);
  for (const unsigned char byte : digest) {
    hex.push_back(kHex[byte >> 4]);
    hex.push_back(kHex[byte & 0x0f]);
  }
  return hex;
}

}  // namespace inweb::adblock
