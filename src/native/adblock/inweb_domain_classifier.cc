// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — registrable-domain (eTLD+1) classification (implementation).

#include "chrome/android/inweb/adblock/inweb_domain_classifier.h"

#include <string>

#include "base/strings/string_util.h"
#include "net/base/registry_controlled_domains.h"

namespace inweb::adblock {

// static
std::string DomainClassifier::RegistrableDomain(const std::string& host) {
  std::string h(base::TrimString(host, ".", base::TRIM_ALL));
  h = base::ToLowerASCII(h);
  if (h.empty()) {
    return h;
  }
  std::string registrable =
      net::registry_controlled_domains::GetDomainAndRegistry(
          h, net::registry_controlled_domains::EXCLUDE_PRIVATE_REGISTRIES);
  if (!registrable.empty()) {
    return registrable;
  }
  // No registrable domain: IP literal, single-label host, or a bare public
  // suffix — the lowercased host itself (Kotlin-reference parity).
  return h;
}

// static
bool DomainClassifier::IsThirdParty(const std::string& document_host,
                                    const std::string& request_host) {
  return RegistrableDomain(document_host) != RegistrableDomain(request_host);
}

}  // namespace inweb::adblock
