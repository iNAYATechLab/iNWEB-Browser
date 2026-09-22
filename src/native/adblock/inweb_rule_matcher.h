// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — rule-against-request matching.
//
// Ported 1:1 from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/RuleMatcher.kt): option constraints
// first (type, party, document domain), then the pattern regex.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RULE_MATCHER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RULE_MATCHER_H_

#include <map>
#include <string>

#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "url/gurl.h"

namespace inweb::adblock {

class RuleMatcher {
 public:
  // True when `rule` matches `request` (pattern AND options).
  static bool Matches(const NetworkFilterRule& rule,
                      const RequestContext& request);

  // Robust host extraction. GURL canonicalizes (lowercase, no port or
  // userinfo); returns "" when the URL has no usable host. Replaces the
  // Kotlin URI-parse + manual-fallback pair.
  static std::string HostOf(const GURL& url);

 private:
  static bool OptionsSatisfied(const FilterOptions& options,
                               const RequestContext& request);
  static bool IsThirdPartyRequest(const RequestContext& request);
  // `domain=` semantics (ABP): an entry matches when the document HOST is
  // equal to the entry or a subdomain of it — full-host matching, not
  // registrable-domain reduction.
  static bool DocumentDomainAllowed(
      const std::map<std::string, bool>& domains,
      const RequestContext& request);
  static bool HostMatchesDomain(const std::string& host,
                                const std::string& domain);
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_RULE_MATCHER_H_
