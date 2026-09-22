#pragma once
#include <string_view>
namespace net { class RedirectInfo; }
namespace network { class ResourceRequest; struct HttpRequestHeadersUpdateParams; }
namespace network::mojom { struct URLResponseHead; }
namespace blink {
// Host-test shim of the blink URLLoaderThrottle base (154 form).
class URLLoaderThrottle {
 public:
  class Delegate {
   public:
    virtual void CancelWithError(int error_code,
                                 std::string_view custom_reason = "") = 0;
    virtual void Resume() = 0;
   protected:
    virtual ~Delegate() = default;
  };
  virtual ~URLLoaderThrottle() = default;
  virtual void DetachFromCurrentSequence() {}
  virtual void WillStartRequest(network::ResourceRequest* request, bool* defer) {}
  virtual void WillRedirectRequest(net::RedirectInfo* redirect_info,
                                   const network::mojom::URLResponseHead& response_head,
                                   bool* defer,
                                   network::HttpRequestHeadersUpdateParams* headers_update_params) {}
  void set_delegate(Delegate* delegate) { delegate_ = delegate; }
 protected:
  URLLoaderThrottle() = default;
  Delegate* delegate_ = nullptr;
};
}  // namespace blink
