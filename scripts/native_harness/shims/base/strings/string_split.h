#pragma once
#include <string>
#include <string_view>
#include <vector>
#include "base/strings/string_util.h"
namespace base {
enum WhitespaceResult { TRIM_WHITESPACE, KEEP_WHITESPACE };
enum SplitResult { SPLIT_WANT_ALL, SPLIT_WANT_NONEMPTY };
inline std::vector<std::string> SplitString(std::string_view input, std::string_view separators,
                                            WhitespaceResult ws, SplitResult result) {
  std::vector<std::string> out;
  size_t start = 0;
  while (true) {
    size_t p = input.find_first_of(separators, start);
    std::string_view tok = (p == std::string_view::npos) ? input.substr(start) : input.substr(start, p - start);
    if (ws == TRIM_WHITESPACE) tok = TrimString(tok, kWhitespaceASCII, TRIM_ALL);
    if (result == SPLIT_WANT_ALL || !tok.empty()) out.emplace_back(tok);
    if (p == std::string_view::npos) break;
    start = p + 1;
  }
  return out;
}
}  // namespace base
