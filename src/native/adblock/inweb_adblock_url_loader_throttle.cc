// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#include "chrome/android/inweb/adblock/inweb_adblock_url_loader_throttle.h"

#include <optional>
#include <utility>

#include "base/functional/bind.h"
#include "base/task/task_traits.h"
#include "base/task/thread_pool.h"
#include "chrome/android/inweb/adblock/inweb_request_destination_map.h"
#include "net/base/net_errors.h"
#include "net/url_request/redirect_info.h"
#include "services/network/public/cpp/resource_request.h"
#include "url/origin.h"

namespace inweb::adblock {

namespace {

// The engine's party classification only needs the document's host. The
// initiator origin carries exactly that (scheme + host [+ port]); opaque
// initiators yield an empty document URL, which the engine treats per its
// documented third-party rules.
GURL DocumentUrlFromInitiator(const std::optional<url::Origin>& initiator) {
  if (!initiator || initiator->opaque()) {
    return GURL();
  }
  return initiator->GetURL();
}

}  // namespace

InwebAdblockURLLoaderThrottle::InwebAdblockURLLoaderThrottle(
    FilterDecider decider)
    : decider_(std::move(decider)) {}

InwebAdblockURLLoaderThrottle::~InwebAdblockURLLoaderThrottle() = default;

void InwebAdblockURLLoaderThrottle::DetachFromCurrentSequence() {
  // Empty on purpose, and that is the honest @154 answer. There is no
  // detach API on base::WeakPtrFactory any more: DetachFromThread() was
  // removed upstream and BindToCurrentSequence() needs a passkey. It
  // needs none here — the factory is only ever asked for a weak pointer
  // inside WillStartRequest(), which per the blink contract runs after
  // any detach, so it binds to the sequence it is actually used on and no
  // pointer is carried across a sequence move.
}

void InwebAdblockURLLoaderThrottle::WillStartRequest(
    network::ResourceRequest* request,
    bool* defer) {
  document_url_ = DocumentUrlFromInitiator(request->request_initiator);
  resource_type_ = ResourceTypeFromRequestDestination(request->destination);
  Evaluate(request->url, defer);
}

void InwebAdblockURLLoaderThrottle::WillRedirectRequest(
    net::RedirectInfo* redirect_info,
    const network::mojom::URLResponseHead& response_head,
    bool* defer,
    network::HttpRequestHeadersUpdateParams* headers_update_params) {
  // Redirects re-evaluate the new URL while keeping the original document
  // and resource type: the initiator document governs party classification,
  // and a request's destination cannot change mid-load.
  Evaluate(redirect_info->new_url, defer);
}

void InwebAdblockURLLoaderThrottle::Evaluate(const GURL& request_url,
                                             bool* defer) {
  *defer = true;
  RequestContext context{request_url, document_url_, resource_type_};
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {base::TaskPriority::USER_VISIBLE},
      base::BindOnce(
          [](const FilterDecider& decider, const RequestContext& context) {
            return decider.Run(context);
          },
          decider_, std::move(context)),
      base::BindOnce(&InwebAdblockURLLoaderThrottle::OnDecision,
                     weak_factory_.GetWeakPtr()));
}

void InwebAdblockURLLoaderThrottle::OnDecision(FilterDecision decision) {
  if (decision.action == FilterAction::kBlock) {
    delegate_->CancelWithError(net::ERR_BLOCKED_BY_CLIENT,
                                "iNWEB tracking protection");
    return;
  }
  delegate_->Resume();
}

}  // namespace inweb::adblock
