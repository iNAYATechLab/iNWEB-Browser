#include "url/gurl.h"
#include <algorithm>
#include <cctype>
GURL::GURL(const std::string& spec) : spec_(spec) {
  auto pos = spec.find("://");
  if (pos == std::string::npos || pos == 0) return;
  size_t start = pos + 3;
  size_t end = start;
  while (end < spec.size() && spec[end] != '/' && spec[end] != '?' && spec[end] != '#') ++end;
  std::string authority = spec.substr(start, end - start);
  auto at = authority.rfind('@');
  if (at != std::string::npos) authority = authority.substr(at + 1);
  if (!authority.empty() && authority[0] == '[') {
    auto rb = authority.find(']');
    if (rb != std::string::npos && rb + 1 < authority.size() && authority[rb + 1] == ':')
      authority = authority.substr(0, rb + 1);
  } else {
    auto colon = authority.rfind(':');
    if (colon != std::string::npos) authority = authority.substr(0, colon);
  }
  std::transform(authority.begin(), authority.end(), authority.begin(),
                 [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
  host_ = authority;
  valid_ = !host_.empty();
}
