#pragma once
#include <filesystem>
#include <fstream>
#include <string>
#include <string_view>
#include "base/files/file_path.h"
namespace base {
inline bool ReadFileToString(const FilePath& path, std::string* contents) {
  std::ifstream in(path.value(), std::ios::binary);
  if (!in) return false;
  in.seekg(0, std::ios::end);
  contents->resize(in.tellg());
  in.seekg(0);
  in.read(contents->data(), contents->size());
  return in.good() || in.eof();
}
inline bool WriteFile(const FilePath& path, std::string_view data) {
  std::ofstream out(path.value(), std::ios::binary | std::ios::trunc);
  if (!out) return false;
  out.write(data.data(), data.size());
  return out.good();
}
inline bool DeleteFile(const FilePath& path) {
  std::error_code ec;
  return std::filesystem::remove(path.value(), ec);
}
inline bool PathExists(const FilePath& path) {
  return std::filesystem::exists(path.value());
}
inline bool CreateDirectory(const FilePath& path) {
  std::error_code ec;
  return std::filesystem::create_directories(path.value(), ec);
}
inline bool ReplaceFile(const FilePath& from, const FilePath& to,
                        void* /*error*/) {
  std::error_code ec;
  std::filesystem::rename(from.value(), to.value(), ec);
  return !ec;
}
}  // namespace base
