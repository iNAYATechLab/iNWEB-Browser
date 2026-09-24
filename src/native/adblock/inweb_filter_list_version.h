// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_VERSION_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_VERSION_H_

#include <optional>
#include <string>

namespace inweb::adblock {

// Version information extracted from EasyList-family header comments.
struct FilterListVersionInfo {
  FilterListVersionInfo();
  ~FilterListVersionInfo();
  FilterListVersionInfo(const FilterListVersionInfo&);
  FilterListVersionInfo& operator=(const FilterListVersionInfo&);
  FilterListVersionInfo(FilterListVersionInfo&&);
  FilterListVersionInfo& operator=(FilterListVersionInfo&&);

  std::optional<std::string> version;
  std::optional<std::string> last_modified;
};

// Extracts `! Version:` and `! Last modified:` header comments from a
// filter list (the EasyList-family convention). Matching is
// case-insensitive; the first occurrence wins. Absent or empty headers
// yield nullopt — never a made-up value. Ported from the Kotlin reference
// (lists/FilterListVersion.kt).
FilterListVersionInfo ParseFilterListVersion(const std::string& text);

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_VERSION_H_
