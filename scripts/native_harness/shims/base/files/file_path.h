#pragma once
#include <string>
#define FILE_PATH_LITERAL(x) (x)
namespace base {
class FilePath {
 public:
  using StringType = std::string;
  static constexpr char kExtensionSeparator = '.';
  FilePath() = default;
  explicit FilePath(std::string path) : path_(std::move(path)) {}
  const std::string& value() const { return path_; }
  FilePath Append(const std::string& component) const {
    return FilePath(path_ + "/" + component);
  }
  FilePath InsertBeforeExtension(const std::string& suffix) const {
    const size_t dot = path_.rfind('.');
    if (dot == std::string::npos) return FilePath(path_ + suffix);
    return FilePath(path_.substr(0, dot) + suffix + path_.substr(dot));
  }
  bool empty() const { return path_.empty(); }
 private:
  std::string path_;
};
}  // namespace base
