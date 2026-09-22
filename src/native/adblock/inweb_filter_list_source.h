// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_SOURCE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_SOURCE_H_

#include <optional>
#include <string>
#include <vector>

namespace inweb::adblock {

// One subscribable filter-list source (EasyList-family). Ported from the
// Kotlin reference (lists/FilterListSource.kt).
//
// `id` is the stable key used for cache files and status reports; it is
// restricted to [a-z0-9-] so it can never escape the cache directory.
//
// Deviation from the Kotlin reference (ADR-041): `download_url` is optional —
// the embedded default list (inweb_default_filter_list.h) has no upstream
// URL. Validation is applied only when a URL is present.
struct FilterListSource {
  std::string id;
  std::string title;
  std::optional<std::string> download_url;
  bool enabled = true;

  // Returns a validated source, or nullopt when id/title/url constraints
  // fail (the Kotlin `require` blocks). Id must match
  // [a-z0-9][a-z0-9-]{0,63}; the title must be non-blank; a present
  // download URL must be http(s).
  static std::optional<FilterListSource> Create(
      std::string id,
      std::string title,
      std::optional<std::string> download_url,
      bool enabled = true);

  // Canonical, subscription-free community lists (§10 defaults). Used when
  // subscription downloads are wired (deferred from patch 0007, ADR-041).
  static const std::vector<FilterListSource>& Defaults();

  // The embedded starter list's identity (no download URL).
  static FilterListSource DefaultList();
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_SOURCE_H_
