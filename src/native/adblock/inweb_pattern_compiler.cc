// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter pattern → RE2 translation (implementation).
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/PatternCompiler.kt).

#include "chrome/android/inweb/adblock/inweb_pattern_compiler.h"

namespace inweb::adblock {

namespace {

constexpr char kSeparator[] = "(?:[^a-zA-Z0-9_.%-]|$)";
constexpr char kDomainAnchorPrefix[] =
    "^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^/?#]*\\.)?";

}  // namespace

// static
std::string PatternCompiler::Translate(const std::string& pattern,
                                       bool domain_anchor,
                                       bool anchor_start,
                                       bool anchor_end) {
  std::string out;
  if (domain_anchor) {
    out += kDomainAnchorPrefix;
  } else if (anchor_start) {
    out += '^';
  }
  for (char ch : pattern) {
    switch (ch) {
      case '*':
        out += ".*";
        break;
      case '^':
        out += kSeparator;
        break;
      default:
        out += RE2::QuoteMeta(std::string(1, ch));
        break;
    }
  }
  if (anchor_end) {
    out += '$';
  }
  return out;
}

// static
std::unique_ptr<RE2> PatternCompiler::Compile(const std::string& pattern,
                                              bool domain_anchor,
                                              bool anchor_start,
                                              bool anchor_end) {
  RE2::Options options;
  options.set_case_sensitive(false);
  auto re = std::make_unique<RE2>(
      Translate(pattern, domain_anchor, anchor_start, anchor_end), options);
  if (!re->ok()) {
    return nullptr;
  }
  return re;
}

}  // namespace inweb::adblock
