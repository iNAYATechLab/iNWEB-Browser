#pragma once
#include <string>
#include <string_view>
namespace base {
inline std::string ToLowerASCII(std::string_view s) {
  std::string out(s);
  for (char& c : out) if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
  return out;
}
inline bool EndsWith(std::string_view s, std::string_view suffix) {
  return s.size() >= suffix.size() && s.substr(s.size() - suffix.size()) == suffix;
}
inline bool StartsWith(std::string_view s, std::string_view prefix) {
  return s.size() >= prefix.size() && s.substr(0, prefix.size()) == prefix;
}
enum TrimPositions { TRIM_TRAILING = 1, TRIM_LEADING = 2, TRIM_ALL = 3 };
inline std::string_view TrimString(std::string_view input, std::string_view trim_chars, TrimPositions pos) {
  size_t b = (pos & TRIM_LEADING) ? input.find_first_not_of(trim_chars) : 0;
  if (b == std::string_view::npos) return std::string_view();
  size_t e = (pos & TRIM_TRAILING) ? input.find_last_not_of(trim_chars) + 1 : input.size();
  return input.substr(b, e - b);
}
extern const char kWhitespaceASCII[];

// Upstream parity: base::TrimWhitespaceASCII (string_util.h).
inline std::string_view TrimWhitespaceASCII(std::string_view input,
                                            TrimPositions pos) {
  return TrimString(input, kWhitespaceASCII, pos);
}
}  // namespace base
