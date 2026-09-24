// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_filter_list_version.h"

#include <strings.h>

#include <string_view>

#include "base/strings/string_split.h"
#include "base/strings/string_util.h"

namespace inweb::adblock {

namespace {

bool CaseInsensitiveStartsWith(std::string_view text,
                               std::string_view prefix) {
  if (text.size() < prefix.size()) {
    return false;
  }
  return ::strncasecmp(text.data(), prefix.data(), prefix.size()) == 0;
}

// Kotlin parity: value after the first ':', trimmed; empty -> nullopt.
std::optional<std::string> ValueAfterColon(std::string_view comment) {
  const size_t colon = comment.find(':');
  if (colon == std::string_view::npos) {
    return std::nullopt;
  }
  const std::string value(
      base::TrimString(comment.substr(colon + 1), base::kWhitespaceASCII,
                       base::TRIM_ALL));
  if (value.empty()) {
    return std::nullopt;
  }
  return value;
}

}  // namespace

FilterListVersionInfo ParseFilterListVersion(const std::string& text) {
  FilterListVersionInfo info;
  for (const std::string& line :
       base::SplitString(text, "\n", base::TRIM_WHITESPACE,
                         base::SPLIT_WANT_NONEMPTY)) {
    if (line.empty() || line[0] != '!') {
      continue;
    }
    const std::string_view comment(line);
    const std::string_view body = comment.substr(1);
    const size_t first_non_space = body.find_first_not_of(" \t");
    const std::string_view trimmed =
        first_non_space == std::string_view::npos
            ? std::string_view()
            : body.substr(first_non_space);
    if (!info.version &&
        CaseInsensitiveStartsWith(trimmed, "Version:")) {
      info.version = ValueAfterColon(trimmed);
    } else if (!info.last_modified &&
               CaseInsensitiveStartsWith(trimmed, "Last modified:")) {
      info.last_modified = ValueAfterColon(trimmed);
    }
    if (info.version && info.last_modified) {
      break;
    }
  }
  return info;
}


// Out-of-line ctor/dtor definitions (chromium-style fallout fix).
FilterListVersionInfo::FilterListVersionInfo() = default;
FilterListVersionInfo::~FilterListVersionInfo() = default;


FilterListVersionInfo::FilterListVersionInfo(FilterListVersionInfo&&) = default;
FilterListVersionInfo& FilterListVersionInfo::operator=(FilterListVersionInfo&&) = default;


FilterListVersionInfo::FilterListVersionInfo(const FilterListVersionInfo&) = default;
FilterListVersionInfo& FilterListVersionInfo::operator=(const FilterListVersionInfo&) = default;

}  // namespace inweb::adblock
