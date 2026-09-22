#pragma once
#include <optional>
#include "services/network/public/mojom/fetch_api.mojom-shared.h"
#include "url/gurl.h"
#include "url/origin.h"
namespace network {
struct ResourceRequest {
  GURL url;
  network::mojom::RequestDestination destination =
      network::mojom::RequestDestination::kEmpty;
  std::optional<url::Origin> request_initiator;
  bool is_outermost_main_frame = false;
};
struct HttpRequestHeadersUpdateParams {};
}  // namespace network
namespace network::mojom {
struct URLResponseHead {};
}  // namespace network::mojom
