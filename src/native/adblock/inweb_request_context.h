// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — one network request, as seen by the decision engine.
//
// Ported from the tested Kotlin reference implementation
// (src/core/tracking-protection/…/TrackingProtectionEngine.kt, data class
// RequestContext / FilterAction / FilterDecision). Minimal surface by
// design: requestUrl, documentUrl, resourceType only.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_CONTEXT_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_CONTEXT_H_

#include <string>

#include "base/memory/raw_ptr.h"
#include "chrome/android/inweb/adblock/inweb_filter_rule.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "url/gurl.h"

namespace inweb::adblock {

// One network request, as seen by the decision engine.
struct RequestContext {
  GURL request_url;
  GURL document_url;
  ResourceType resource_type = ResourceType::kOther;
};

// Decision for a request.
enum class FilterAction { kBlock, kAllow, kPass };

// The decision plus what caused it (for the Security Center, §24).
// `matched_rule` points into the engine's rule storage and is valid for
// as long as the engine snapshot that produced the decision is alive.
struct FilterDecision {
  // Complex by the style plugin (raw_ptr + string members): full
  // out-of-line rule-of-five, definitions in
  // inweb_tracking_protection_engine.cc.
  FilterDecision();
  FilterDecision(const FilterDecision&);
  FilterDecision& operator=(const FilterDecision&);
  FilterDecision(FilterDecision&&);
  FilterDecision& operator=(FilterDecision&&);
  ~FilterDecision();

  FilterAction action = FilterAction::kPass;
  raw_ptr<const NetworkFilterRule> matched_rule;
  std::string allowlisted_site;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_REQUEST_CONTEXT_H_
