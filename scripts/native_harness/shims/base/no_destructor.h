#pragma once
#include <utility>
namespace base {
template <typename T>
class NoDestructor {
 public:
  template <typename... Args>
  explicit NoDestructor(Args&&... args) {
    new (storage_) T(std::forward<Args>(args)...);
  }
  NoDestructor(const NoDestructor&) = delete;
  NoDestructor& operator=(const NoDestructor&) = delete;
  const T& operator*() const { return *get(); }
  T& operator*() { return *get(); }
  const T* operator->() const { return get(); }
  T* operator->() { return get(); }
  T* get() { return reinterpret_cast<T*>(storage_); }
  const T* get() const { return reinterpret_cast<const T*>(storage_); }
 private:
  alignas(T) unsigned char storage_[sizeof(T)];
};
}  // namespace base
