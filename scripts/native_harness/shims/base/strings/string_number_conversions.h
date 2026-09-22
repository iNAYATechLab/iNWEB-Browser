#pragma once
#include <cstdlib>
#include <string>
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
}  // namespace base
