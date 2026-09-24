#pragma once
#include "url/gurl.h"
namespace net {
struct RedirectInfo {
  GURL new_url;
};
}  // namespace net
