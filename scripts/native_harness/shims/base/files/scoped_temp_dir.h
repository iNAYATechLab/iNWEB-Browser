#pragma once
#include <cstdlib>
#include <filesystem>
#include <string>
#include "base/files/file_path.h"
namespace base {
class ScopedTempDir {
 public:
  ScopedTempDir() = default;
  ~ScopedTempDir() {
    if (!path_.empty()) {
      std::error_code ec;
      std::filesystem::remove_all(path_, ec);
    }
  }
  bool CreateUniqueTempDir() {
    char tmpl[] = "/tmp/inweb-harness-XXXXXX";
    const char* dir = mkdtemp(tmpl);
    if (!dir) return false;
    path_ = dir;
    return true;
  }
  FilePath GetPath() const { return FilePath(path_); }
 private:
  std::string path_;
};
}  // namespace base
