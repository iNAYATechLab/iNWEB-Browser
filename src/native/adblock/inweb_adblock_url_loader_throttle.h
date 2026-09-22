// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_URL_LOADER_THROTTLE_H_
#define CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_URL_LOADER_THROTTLE_H_

#include "base/functional/callback.h"
#include "base/memory/weak_ptr.h"
#include "chrome/android/inweb/adblock/inweb_request_context.h"
#include "chrome/android/inweb/adblock/inweb_resource_type.h"
#include "third_party/blink/public/common/loader/url_loader_throttle.h"
#include "url/gurl.h"

namespace inweb::adblock {

// Produces the filter decision for a request. Invoked on the thread pool;
// implementations must be safe to call from any sequence (production:
// InwebAdblockEngineHolder::Decide; tests: scriptable lambdas).
using FilterDecider =
    base::RepeatingCallback<FilterDecision(const RequestContext&)>;

// Defers each observed request (start + redirects) until the native ad-block
// engine has decided, then resumes or cancels:
//   kBlock → Delegate::CancelWithError(net::ERR_BLOCKED_BY_CLIENT)
//   kAllow/kPass → Delegate::Resume()
//
// v1 scope (PHASE4 §7, ADR-040): the outermost main-frame navigation is
// exempt (the registration site skips it); redirects re-evaluate against
// the redirect target with the original document; party classification
// uses the request initiator origin. Cosmetic filtering and
// websocket/popup handling are out of scope in v1.
class InwebAdblockURLLoaderThrottle : public blink::URLLoaderThrottle {
 public:
  explicit InwebAdblockURLLoaderThrottle(FilterDecider decider);
  ~InwebAdblockURLLoaderThrottle() override;

  InwebAdblockURLLoaderThrottle(const InwebAdblockURLLoaderThrottle&) = delete;
  InwebAdblockURLLoaderThrottle& operator=(
      const InwebAdblockURLLoaderThrottle&) = delete;

  // blink::URLLoaderThrottle:
  void DetachFromCurrentSequence() override;
  void WillStartRequest(network::ResourceRequest* request,
                        bool* defer) override;
  void WillRedirectRequest(
      net::RedirectInfo* redirect_info,
      const network::mojom::URLResponseHead& response_head,
      bool* defer,
      network::HttpRequestHeadersUpdateParams* headers_update_params) override;

 private:
  void Evaluate(const GURL& request_url, bool* defer);
  void OnDecision(FilterDecision decision);

  FilterDecider decider_;
  GURL document_url_;
  ResourceType resource_type_ = ResourceType::kOther;
  base::WeakPtrFactory<InwebAdblockURLLoaderThrottle> weak_factory_{this};
};

}  // namespace inweb::adblock

#endif  // CHROME_ANDROID_INWEB_ADBLOCK_INWEB_ADBLOCK_URL_LOADER_THROTTLE_H_
