// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — registrable-domain (eTLD+1) classification.
//
// Interface ported from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/DomainClassifier.kt). The Kotlin version
// used a built-in table of common multi-part public suffixes; this native
// integration uses Chromium's full Public Suffix List instead (the
// documented engine-side integration intent — PHASE4-ADBLOCK-DESIGN §1).

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DOMAIN_CLASSIFIER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DOMAIN_CLASSIFIER_H_

#include <string>

namespace inweb::adblock {

class DomainClassifier {
 public:
  // The registrable domain of a host (e.g. `a.b.example.com` →
  // `example.com`). IP literals, single-label hosts (e.g. `localhost`)
  // and bare public suffixes return the (lowercased) host itself.
  static std::string RegistrableDomain(const std::string& host);

  // True when the two hosts belong to different registrable domains.
  static bool IsThirdParty(const std::string& document_host,
                           const std::string& request_host);
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_DOMAIN_CLASSIFIER_H_
