// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — data-saver decision core (PHASE7 §4, §19).
// See the header for the honesty rules this implementation is bound by.

#include "chrome/android/inweb/offline/inweb_data_saver.h"

#include "chrome/android/inweb/adblock/inweb_domain_classifier.h"

namespace inweb::offline {

namespace {

bool IsThirdParty(const adblock::RequestContext& request) {
  return adblock::DomainClassifier::IsThirdParty(request.document_url.host(),
                                                 request.request_url.host());
}

}  // namespace

// DataSaverStatistics ------------------------------------------------------

DataSaverStatistics::DataSaverStatistics() = default;
DataSaverStatistics::~DataSaverStatistics() = default;

void DataSaverStatistics::RecordAvoided(bool size_known,
                                        int64_t content_length) const {
  base::AutoLock lock(lock_);
  ++requests_avoided_;
  if (size_known && content_length > 0) {
    estimated_bytes_saved_ += content_length;
  } else {
    ++requests_with_unknown_size_;
  }
}

void DataSaverStatistics::RecordSpeculativeSuppressed() const {
  base::AutoLock lock(lock_);
  ++speculative_suppressed_;
}

int DataSaverStatistics::requests_avoided() const {
  base::AutoLock lock(lock_);
  return requests_avoided_;
}

int DataSaverStatistics::requests_with_unknown_size() const {
  base::AutoLock lock(lock_);
  return requests_with_unknown_size_;
}

int DataSaverStatistics::speculative_suppressed() const {
  base::AutoLock lock(lock_);
  return speculative_suppressed_;
}

int64_t DataSaverStatistics::estimated_bytes_saved() const {
  base::AutoLock lock(lock_);
  return estimated_bytes_saved_;
}

void DataSaverStatistics::Reset() const {
  base::AutoLock lock(lock_);
  requests_avoided_ = 0;
  requests_with_unknown_size_ = 0;
  speculative_suppressed_ = 0;
  estimated_bytes_saved_ = 0;
}

// DataSaverPolicy ----------------------------------------------------------

DataSaverPolicy::DataSaverPolicy(DataSaverSettings settings)
    : settings_(settings) {}

DataSaverPolicy::~DataSaverPolicy() = default;

void DataSaverPolicy::UpdateSettings(DataSaverSettings settings) {
  settings_ = settings;
}

DataSaverDecision DataSaverPolicy::Decide(const adblock::RequestContext& request,
                                          bool is_speculative) const {
  DataSaverDecision decision;
  if (!settings_.enabled) {
    return decision;  // kNone — data saver is off, it claims nothing.
  }

  // Speculative work first: it is not a request the user made, so
  // suppressing it never changes what the page shows.
  if (is_speculative && settings_.reduce_speculative) {
    decision.block = true;
    decision.reason = DataSaverBlockReason::kSpeculative;
    return decision;
  }

  switch (request.resource_type) {
    case adblock::ResourceType::kImage:
      if (settings_.block_third_party_images && IsThirdParty(request)) {
        decision.block = true;
        decision.reason = DataSaverBlockReason::kThirdPartyImage;
      }
      break;
    case adblock::ResourceType::kFont:
      if (settings_.block_third_party_fonts && IsThirdParty(request)) {
        decision.block = true;
        decision.reason = DataSaverBlockReason::kThirdPartyFont;
      }
      break;
    case adblock::ResourceType::kMedia:
      // Opt-in: media is often the very thing the user opened the page for,
      // so blocking it is never a default.
      if (settings_.block_media) {
        decision.block = true;
        decision.reason = DataSaverBlockReason::kHeavyweightMedia;
      }
      break;
    default:
      break;  // documents, scripts, XHR … are never blocked by data saver
  }
  return decision;
}

}  // namespace inweb::offline
