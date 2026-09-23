// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_FILTER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_FILTER_H_

#include <map>
#include <string>
#include <vector>

#include "url/gurl.h"

namespace inweb::adblock {

// A cosmetic (element-hiding) rule: a CSS selector hidden on a set of
// document domains (MASTER-SPEC §11). Ported 1:1 from the tested Kotlin
// reference (src/core/tracking-protection .../CosmeticFilter.kt).
//
// v1 subset (ADR-021):
//  - `##`   — hide selector on matching domains (empty domain list =
//             everywhere)
//  - `#@#`  — exception: cancel hiding of that selector on matching
//             domains
//  - `#?#`  — procedural (JS-based) rules: recognized and counted, NOT
//             matched
//  - `#$#` / `#%#` — stylesheet-rewrite / scriptlet rules: counted as
//             unsupported, NOT matched
//
// Selectors pass through verbatim — the browser CSS parser is the
// validator at injection time; nothing is invented here.
struct CosmeticRule {
  std::string raw;
  std::string selector;
  // Include (true) / exclude (false) domains; empty = applies
  // everywhere. Map semantics: a later entry for the same domain
  // overwrites an earlier one (Kotlin parity).
  std::map<std::string, bool> domains;
  bool is_exception = false;
};

// One parsed cosmetic line, or why it was not a matchable rule.
struct CosmeticLineResult {
  enum class Kind {
    kNotCosmetic,   // no cosmetic marker on the line
    kHide,          // `##`
    kException,     // `#@#`
    kProcedural,    // `#?#` — counted, never matched
    kUnsupported,   // `#$#` / `#%#` — counted, never matched
    kInvalid,       // empty selector or malformed domain list
  };
  Kind kind = Kind::kNotCosmetic;
  CosmeticRule rule;
};

// Result of parsing the cosmetic half of one filter list (Kotlin
// ParsedCosmeticList parity).
struct ParsedCosmeticList {
  std::string list_id;
  std::vector<CosmeticRule> rules;
  int procedural_rule_count = 0;
  int unsupported_rule_count = 0;
  int invalid_rule_count = 0;
  int comment_count = 0;
};

// Parses the cosmetic rules of an EasyList-family list body. Network
// rules and comments are not this parser's concern (see
// inweb_filter_list_parser.h) — lines without a cosmetic marker are
// simply skipped.
class CosmeticFilterParser {
 public:
  // Parses one trimmed line. Pure, allocation-light.
  static CosmeticLineResult ParseLine(const std::string& line);

  // Full-list parse (test/parity convenience; mirrors Kotlin parse()).
  static ParsedCosmeticList Parse(const std::string& text,
                                  const std::string& list_id);
};

// Computes the stylesheet to inject into a page: every hiding selector
// that applies to the document host, minus selectors cancelled by
// matching exception (`#@#`) rules. Pure function of the parsed lists
// and the host — injection timing is inweb_cosmetic_injector's concern.
class CosmeticFilterEngine {
 public:
  explicit CosmeticFilterEngine(std::vector<CosmeticRule> rules);

  CosmeticFilterEngine(const CosmeticFilterEngine&) = delete;
  CosmeticFilterEngine& operator=(const CosmeticFilterEngine&) = delete;

  int total_rules() const { return static_cast<int>(rules_.size()); }

  // The grouped hiding CSS for |host|, or "" when nothing applies.
  // Example: ".ad, .banner { display: none !important; }"
  std::string HideCssFor(const std::string& host) const;

 private:
  // Same include/exclude semantics as the network `domain=` option.
  static bool DomainsMatch(const std::map<std::string, bool>& domains,
                           const std::string& host);
  static bool HostMatchesDomain(const std::string& host,
                                const std::string& domain);

  std::vector<CosmeticRule> rules_;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_COSMETIC_FILTER_H_
