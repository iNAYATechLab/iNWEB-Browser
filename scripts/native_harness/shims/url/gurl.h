#pragma once
#include <string>
class GURL {
 public:
  GURL() = default;
  explicit GURL(const std::string& spec);
  bool is_valid() const { return valid_; }
  bool is_empty() const { return spec_.empty(); }
  std::string host() const { return host_; }
  const std::string& possibly_invalid_spec() const { return spec_; }
 private:
  std::string spec_;
  std::string host_;
  bool valid_ = false;
};
