#pragma once
#include <string>
#include <string_view>
namespace base {
// Host-test stand-in for base::containers/span.h's as_byte_span: upstream
// returns span<const uint8_t>; the harness returns string_view, which the
// HexEncode* shims consume — call sites stay identical in both worlds.
inline std::string_view as_byte_span(const std::string& s) { return s; }
}  // namespace base
