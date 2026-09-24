// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — rule-against-request matching (implementation).

#include "chrome/android/inweb/adblock/inweb_rule_matcher.h"

#include <string>

#include "base/strings/string_util.h"
#include "chrome/android/inweb/adblock/inweb_domain_classifier.h"
#include "re2/re2.h"

namespace inweb::adblock {

// static
bool RuleMatcher::Matches(const NetworkFilterRule& rule,
                          const RequestContext& request) {
  if (rule.unsupported) {
    return false;
  }
  if (!OptionsSatisfied(rule.options, request)) {
    return false;
  }
  if (!rule.regex) {
    return false;
  }
  return RE2::PartialMatch(request.request_url.possibly_invalid_spec(),
                           *rule.regex);
}

// static
std::string RuleMatcher::HostOf(const GURL& url) {
  if (!url.is_valid()) {
    return std::string();
  }
  // GURL hosts are already canonical (lowercased); ToLowerASCII is a
  // no-op safety net.
  return base::ToLowerASCII(url.host());
}

// static
bool RuleMatcher::OptionsSatisfied(const FilterOptions& options,
                                   const RequestContext& request) {
  const ResourceType type = request.resource_type;
  if (!options.types.empty() && options.types.count(type) == 0) {
    return false;
  }
  if (options.negated_types.count(type) != 0) {
    return false;
  }
  if (options.third_party.has_value()) {
    if (IsThirdPartyRequest(request) != *options.third_party) {
      return false;
    }
  }
  if (!options.domains.empty() &&
      !DocumentDomainAllowed(options.domains, request)) {
    return false;
  }
  return true;
}

// static
bool RuleMatcher::IsThirdPartyRequest(const RequestContext& request) {
  const std::string doc_host = HostOf(request.document_url);
  if (doc_host.empty()) {
    return true;
  }
  const std::string req_host = HostOf(request.request_url);
  if (req_host.empty()) {
    return false;
  }
  return DomainClassifier::IsThirdParty(doc_host, req_host);
}

// static
bool RuleMatcher::DocumentDomainAllowed(
    const std::map<std::string, bool>& domains,
    const RequestContext& request) {
  const std::string doc_host = HostOf(request.document_url);
  if (doc_host.empty()) {
    return false;
  }

  for (const auto& [domain, include] : domains) {
    if (!include && HostMatchesDomain(doc_host, domain)) {
      return false;
    }
  }

  bool has_includes = false;
  for (const auto& [domain, include] : domains) {
    if (include) {
      has_includes = true;
      if (HostMatchesDomain(doc_host, domain)) {
        return true;
      }
    }
  }
  return !has_includes;
}

// static
bool RuleMatcher::HostMatchesDomain(const std::string& host,
                                    const std::string& domain) {
  return host == domain || base::EndsWith(host, "." + domain);
}

}  // namespace inweb::adblock
