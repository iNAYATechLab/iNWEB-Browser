#pragma once
#include "url/gurl.h"
namespace url {
class Origin {
 public:
  Origin() = default;
  static Origin Create(const GURL& url) {
    Origin o;
    o.url_ = url;
    return o;
  }
  bool opaque() const { return url_.is_empty(); }
  GURL GetURL() const { return url_; }
 private:
  GURL url_;
};
}  // namespace url
