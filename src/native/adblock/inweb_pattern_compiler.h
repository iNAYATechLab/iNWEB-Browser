// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — filter pattern → RE2 translation.
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/PatternCompiler.kt).
//
// Translation rules (EasyList semantics):
//  - `||x` becomes `^[scheme]://[optional subdomain-ish prefix.]x`
//  - `|` at the start becomes `^`; `|` at the end becomes `$`
//  - `*` becomes `.*`
//  - `^` becomes a separator: any character that is NOT a letter, digit,
//    `_`, `-`, `.` or `%` — or the end of the URL
//  - everything else is matched literally (case-insensitively)

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_PATTERN_COMPILER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_PATTERN_COMPILER_H_

#include <memory>
#include <string>

#include "re2/re2.h"

namespace inweb::adblock {

class PatternCompiler {
 public:
  // Compiles the matcher regex for a rule body plus its anchor flags.
  // Returns nullptr if RE2 rejects the translated pattern (treated by the
  // parser as an invalid rule).
  static std::unique_ptr<RE2> Compile(const std::string& pattern,
                                      bool domain_anchor,
                                      bool anchor_start,
                                      bool anchor_end);

  // Exposed for unit tests.
  static std::string Translate(const std::string& pattern,
                               bool domain_anchor,
                               bool anchor_start,
                               bool anchor_end);
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_PATTERN_COMPILER_H_
