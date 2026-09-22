// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter-list parser (EasyList-family syntax subset).
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/FilterListParser.kt).
//
// Line classification:
//  - blank, `!` comment, `[...]` header   → comments
//  - cosmetic rules (`##`, `#@#`, `#?#`)  → recognized and counted; the
//    network matcher ignores them (cosmetic filtering is a later phase)
//  - `@@` prefix                          → exception rule (allow)
//  - options after the last `$`           → parsed; unknown options mark
//    the rule unsupported (kept out of matching, counted honestly)
//  - anything unparseable                 → invalid (counted, never
//    silently dropped)
//
// Deviation from the Kotlin reference (robustness): a pattern that RE2
// rejects at compile time counts as invalid (the Kotlin version could
// throw lazily); regexes compile once here, at parse time.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_PARSER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_PARSER_H_

#include <map>
#include <optional>
#include <string>

#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"

namespace inweb::adblock {

class FilterListParser {
 public:
  static ParsedFilterList Parse(const std::string& text,
                                const std::string& list_id = "default");

 private:
  enum class OptionsResultKind { kInvalid, kUnsupported, kOk };
  struct OptionsResult {
    OptionsResultKind kind = OptionsResultKind::kInvalid;
    FilterOptions options;
  };

  static std::optional<NetworkFilterRule> ParseRule(const std::string& line,
                                                    int line_number);
  static OptionsResult ParseOptions(const std::string& text);
  static bool ParseDomains(const std::string& text,
                           std::map<std::string, bool>* into);
  static const std::map<std::string, ResourceType>& TypeTokens();
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_PARSER_H_
