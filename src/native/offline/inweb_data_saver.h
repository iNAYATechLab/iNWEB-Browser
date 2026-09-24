// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// iNWEB Browser — data-saver decision core (PHASE7 §4, §19; MASTER-SPEC
// §19/§20; ADR-019/020).
//
// Data-saver mode is a RULE-CLASS MODE over the iNWEB filter engine: the
// same request context, the same decision path, no second engine. What it
// adds is a classification of heavyweight / non-essential resource classes
// that are blocked while the mode is on.
//
// Honesty rules that shape this file (§20, §57):
//   * No proxy, no client-side compression of arbitrary HTTPS traffic, no
//     "bytes saved by compression" claim of any kind — none exists.
//   * The counter reports REQUESTS AVOIDED: real, counted BLOCK decisions
//     under data-saver rules.
//   * "Bytes saved" is exposed only as `estimated_bytes_saved()`, an
//     ESTIMATE built solely from content-lengths actually observed for the
//     requests we avoided. Requests whose size we never saw are counted
//     separately in `requests_with_unknown_size()` and contribute ZERO to
//     the estimate. Nothing is extrapolated, averaged or guessed.

#ifndef CHROME_ANDROID_INWEB_OFFLINE_INWEB_DATA_SAVER_H_
#define CHROME_ANDROID_INWEB_OFFLINE_INWEB_DATA_SAVER_H_

#include <cstdint>

#include "base/synchronization/lock.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"

namespace inweb::offline {

// Why a request was blocked (or not) under data-saver rules. `kNone` means
// the request is untouched by data saver — it may still be blocked by the
// normal filter lists; data saver never takes credit for those.
enum class DataSaverBlockReason {
  kNone,
  kThirdPartyImage,   // image served from a different registrable domain
  kThirdPartyFont,    // webfont served from a different registrable domain
  kHeavyweightMedia,  // media (video/audio) — opt-in, see settings
  kSpeculative,       // prefetch / preconnect suppressed, not a user request
};

// Which resource classes the mode blocks. Defaults are the conservative
// ones: third-party images on, media and third-party fonts off (blocking
// either can break pages the user is actively looking at).
struct DataSaverSettings {
  bool enabled = false;
  bool block_third_party_images = true;
  bool block_third_party_fonts = false;
  bool block_media = false;
  // Speculative preconnect/prefetch is reduced while the mode is on
  // (PHASE7 §4.2). Suppressing it is a data-saver decision, not a filter
  // decision, so it is accounted separately.
  bool reduce_speculative = true;
};

struct DataSaverDecision {
  bool block = false;
  DataSaverBlockReason reason = DataSaverBlockReason::kNone;
};

// Thread-safe accounting for the mode. Modeled on the filter engine's
// EngineStatistics: locks internally, never blocks on I/O.
class DataSaverStatistics {
 public:
  DataSaverStatistics();
  ~DataSaverStatistics();
  DataSaverStatistics(const DataSaverStatistics&) = delete;
  DataSaverStatistics& operator=(const DataSaverStatistics&) = delete;
  DataSaverStatistics(DataSaverStatistics&&) = delete;
  DataSaverStatistics& operator=(DataSaverStatistics&&) = delete;

  // One avoided request. `size_known` false (content-length never
  // observed) counts the request but adds nothing to the estimate — see
  // the file comment.
  void RecordAvoided(bool size_known, int64_t content_length) const;
  // A speculative request suppressed before it was ever issued.
  void RecordSpeculativeSuppressed() const;

  int requests_avoided() const;
  int requests_with_unknown_size() const;
  int speculative_suppressed() const;
  // ESTIMATE: sum of observed content-lengths of avoided requests. Never
  // presented as a measured saving.
  int64_t estimated_bytes_saved() const;
  void Reset() const;

 private:
  mutable base::Lock lock_;
  mutable int requests_avoided_ = 0;
  mutable int requests_with_unknown_size_ = 0;
  mutable int speculative_suppressed_ = 0;
  mutable int64_t estimated_bytes_saved_ = 0;
};

// The rule-class policy. Pure and synchronous: same input, same decision,
// no hidden state beyond the settings.
class DataSaverPolicy {
 public:
  explicit DataSaverPolicy(DataSaverSettings settings);
  ~DataSaverPolicy();
  DataSaverPolicy(const DataSaverPolicy&) = delete;
  DataSaverPolicy& operator=(const DataSaverPolicy&) = delete;
  DataSaverPolicy(DataSaverPolicy&&) = delete;
  DataSaverPolicy& operator=(DataSaverPolicy&&) = delete;

  const DataSaverSettings& settings() const { return settings_; }
  void UpdateSettings(DataSaverSettings settings);

  // Decides for one request. `is_speculative` marks prefetch/preconnect
  // work the page did not ask for directly.
  DataSaverDecision Decide(const adblock::RequestContext& request,
                           bool is_speculative) const;

 private:
  DataSaverSettings settings_;
};

}  // namespace inweb::offline

#endif  // CHROME_ANDROID_INWEB_OFFLINE_INWEB_DATA_SAVER_H_
