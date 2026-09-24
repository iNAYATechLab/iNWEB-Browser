// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — parsed network-filter rule model.
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/FilterRule.kt).
//
// Pattern anchors:
//  - `||pattern` — domain anchor: matches at a scheme/host boundary
//  - `|pattern`  — left anchor: matches at the start of the URL
//  - `pattern|`  — right anchor: matches at the end of the URL
// Pattern specials: `*` (any characters), `^` (separator character or
// end of URL).
//
// A rule marked `unsupported` carries options outside the supported
// subset; it is never matched (counted separately by the parser —
// honest scoping).

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_RULE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_RULE_H_

#include <map>
#include <memory>
#include <optional>
#include <set>
#include <string>
#include <vector>

#include "chrome/android/inweb/adblock/inweb_cosmetic_filter.h"

#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "re2/re2.h"

namespace inweb::adblock {

// Option constraints of a rule (the part after `$`).
struct FilterOptions {
  FilterOptions();
  ~FilterOptions();
  FilterOptions(const FilterOptions&);
  FilterOptions& operator=(const FilterOptions&);
  FilterOptions(FilterOptions&&);
  FilterOptions& operator=(FilterOptions&&);

  std::set<ResourceType> types;
  std::set<ResourceType> negated_types;
  // true = third-party requests only; false = first-party only;
  // nullopt = no constraint.
  std::optional<bool> third_party;
  // `domain=` entries: include (true) / exclude (false).
  std::map<std::string, bool> domains;
};

// Parsed network-filter rule (EasyList-family syntax subset). Move-only:
// owns its compiled regex.
struct NetworkFilterRule {
  NetworkFilterRule();
  NetworkFilterRule(NetworkFilterRule&&);
  NetworkFilterRule& operator=(NetworkFilterRule&&);
  ~NetworkFilterRule();
  NetworkFilterRule(const NetworkFilterRule&) = delete;
  NetworkFilterRule& operator=(const NetworkFilterRule&) = delete;

  std::string raw;
  std::string pattern;
  bool is_exception = false;
  bool domain_anchor = false;
  bool anchor_start = false;
  bool anchor_end = false;
  FilterOptions options;
  bool unsupported = false;
  int line = 0;
  // Compiled matcher (translation per InwebPatternCompiler). Compiled
  // once at parse time; RE2 objects are immutable and safe to use from
  // multiple threads afterwards.
  std::unique_ptr<RE2> regex;
};

// Result of parsing one filter list.
struct ParsedFilterList {
  ParsedFilterList();
  ~ParsedFilterList();
  // Move-only: holds move-only NetworkFilterRules.
  ParsedFilterList(const ParsedFilterList&) = delete;
  ParsedFilterList& operator=(const ParsedFilterList&) = delete;
  ParsedFilterList(ParsedFilterList&&);
  ParsedFilterList& operator=(ParsedFilterList&&);

  std::string list_id;
  std::vector<NetworkFilterRule> network_rules;
  // Cosmetic (element-hiding) rules retained for the cosmetic engine
  // (patch 0012); `cosmetic_rule_count` below still counts ALL lines
  // with a cosmetic marker, matchable or not.
  std::vector<CosmeticRule> cosmetic_rules;
  int cosmetic_rule_count = 0;
  int comment_count = 0;
  int invalid_rule_count = 0;
  int unsupported_rule_count = 0;

  int total_lines() const {
    return static_cast<int>(network_rules.size()) + cosmetic_rule_count +
           comment_count + invalid_rule_count + unsupported_rule_count;
  }
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_RULE_H_
