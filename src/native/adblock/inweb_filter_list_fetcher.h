// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_FETCHER_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_FETCHER_H_

#include <optional>
#include <string>
#include <utility>

namespace inweb::adblock {

// Outcome of one filter-list download attempt. Ported from the Kotlin
// reference (lists/FilterListFetcher.kt).
struct FetchResult {
  enum class Kind {
    // 200 OK — full body plus revalidation validators (may be null).
    kSuccess,
    // 304 Not Modified — cached content is still current.
    kNotModified,
    // Transport or HTTP failure — the caller keeps serving cached content.
    kFailure,
  };

  static FetchResult Success(std::string body,
                             std::optional<std::string> etag,
                             std::optional<std::string> last_modified) {
    FetchResult result;
    result.kind = Kind::kSuccess;
    result.body = std::move(body);
    result.etag = std::move(etag);
    result.last_modified = std::move(last_modified);
    return result;
  }
  static FetchResult NotModified(std::optional<std::string> etag,
                                 std::optional<std::string> last_modified) {
    FetchResult result;
    result.kind = Kind::kNotModified;
    result.etag = std::move(etag);
    result.last_modified = std::move(last_modified);
    return result;
  }
  static FetchResult Failure(std::string reason) {
    FetchResult result;
    result.kind = Kind::kFailure;
    result.reason = std::move(reason);
    return result;
  }

  Kind kind = Kind::kFailure;
  std::string body;  // kSuccess only
  std::optional<std::string> etag;
  std::optional<std::string> last_modified;
  std::string reason;  // kFailure only
};

// Port: how filter lists are downloaded. The Kotlin reference ships a
// pure-JVM HttpURLConnection implementation; the Chromium-side production
// transport is a SimpleURLLoader adapter that needs profile/
// storage-partition wiring and async threading through the manager.
//
// That adapter is deliberately NOT part of patch 0007 (ADR-041): v1 ships
// the embedded default list plus this interface and the full
// fetch/refresh decision logic, so the subscription transport can land as
// a follow-up without touching the engine. Tests provide scriptable
// fetchers.
class FilterListFetcher {
 public:
  virtual ~FilterListFetcher() = default;

  // Fetches `url`. When |etag| / |last_modified| are supplied they are
  // sent as conditional validators (If-None-Match / If-Modified-Since) so
  // an unchanged list costs one cheap 304 round-trip.
  virtual FetchResult Fetch(
      const std::string& url,
      const std::optional<std::string>& etag,
      const std::optional<std::string>& last_modified) = 0;
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_FILTER_LIST_FETCHER_H_
