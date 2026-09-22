// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include <utility>

#include "base/no_destructor.h"
#include "chrome/android/inweb/adblock/inweb_filter_list_source.h"

namespace inweb::adblock {

namespace {

bool IsValidListId(const std::string& id) {
  if (id.empty() || id.size() > 64) {
    return false;
  }
  const char first = id[0];
  if (!((first >= 'a' && first <= 'z') ||
        (first >= '0' && first <= '9'))) {
    return false;
  }
  for (const char c : id) {
    if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-')) {
      return false;
    }
  }
  return true;
}

bool IsBlank(const std::string& text) {
  for (const char c : text) {
    if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
      return false;
    }
  }
  return true;
}

bool IsHttpUrl(const std::string& url) {
  return url.rfind("https://", 0) == 0 || url.rfind("http://", 0) == 0;
}

}  // namespace

// static
std::optional<FilterListSource> FilterListSource::Create(
    std::string id,
    std::string title,
    std::optional<std::string> download_url,
    bool enabled) {
  if (!IsValidListId(id) || IsBlank(title)) {
    return std::nullopt;
  }
  if (download_url && !IsHttpUrl(*download_url)) {
    return std::nullopt;
  }
  return FilterListSource{std::move(id), std::move(title),
                          std::move(download_url), enabled};
}

// static
const std::vector<FilterListSource>& FilterListSource::Defaults() {
  static const base::NoDestructor<std::vector<FilterListSource>> defaults([] {
    std::vector<FilterListSource> lists;
    if (auto easylist = Create("easylist", "EasyList",
                               std::string("https://easylist.to/easylist/easylist.txt"))) {
      lists.push_back(std::move(*easylist));
    }
    if (auto easyprivacy =
            Create("easyprivacy", "EasyPrivacy",
                   std::string("https://easylist.to/easylist/easyprivacy.txt"))) {
      lists.push_back(std::move(*easyprivacy));
    }
    return lists;
  }());
  return *defaults;
}

// static
FilterListSource FilterListSource::DefaultList() {
  return FilterListSource{"inweb-default", "iNWEB Starter List",
                          /*download_url=*/std::nullopt, /*enabled=*/true};
}

}  // namespace inweb::adblock
