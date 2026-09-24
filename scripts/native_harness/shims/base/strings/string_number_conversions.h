#pragma once
#include <cstdlib>
#include <string>
#include <string_view>
namespace base {
template <typename T>
std::string NumberToString(T value) {
  return std::to_string(value);
}
inline bool StringToInt64(const std::string& input, int64_t* out) {
  char* end = nullptr;
  const long long value = std::strtoll(input.c_str(), &end, 10);
  if (end == input.c_str() || *end != '\0') return false;
  *out = value;
  return true;
}
inline bool StringToInt(const std::string& input, int* out) {
  int64_t value = 0;
  if (!StringToInt64(input, &value)) return false;
  *out = static_cast<int>(value);
  return true;
}
// Lowercase hex (matches upstream base::HexEncodeLower semantics).
// The harness implementation may use raw indexing — the unsafe-buffers
// plugin only runs in the real Chromium build, and the audit scans
// src/native, not shims.
inline std::string HexEncodeLower(std::string_view bytes) {
  static const char* kHex = "0123456789abcdef";
  std::string out;
  out.reserve(bytes.size() * 2);
  for (const char c : bytes) {
    const unsigned char b = static_cast<unsigned char>(c);
    out.push_back(kHex[b >> 4]);
    out.push_back(kHex[b & 0x0f]);
  }
  return out;
}
}  // namespace base
