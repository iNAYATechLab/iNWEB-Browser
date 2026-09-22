#pragma once
#include <mutex>
namespace base {
using Lock = std::mutex;
class AutoLock {
 public:
  explicit AutoLock(Lock& l) : l_(l) { l_.lock(); }
  ~AutoLock() { l_.unlock(); }
 private:
  Lock& l_;
};
}  // namespace base
