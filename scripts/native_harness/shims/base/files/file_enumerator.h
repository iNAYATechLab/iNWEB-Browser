#pragma once
#include <filesystem>
#include "base/files/file_path.h"
namespace base {
class FileEnumerator {
 public:
  enum { FILES = 1 };
  FileEnumerator(const FilePath& root, bool /*recursive*/, int /*type*/)
      : it_(std::filesystem::directory_iterator(root.value())) {}
  FilePath Next() {
    while (it_ != end_) {
      const std::filesystem::directory_entry entry = *it_++;
      if (entry.is_regular_file()) return FilePath(entry.path().string());
    }
    return FilePath();
  }
 private:
  std::filesystem::directory_iterator it_;
  std::filesystem::directory_iterator end_;
};
}  // namespace base
