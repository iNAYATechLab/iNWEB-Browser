#pragma once
#include <type_traits>
#include <utility>
namespace base {
template <typename T>
class NoDestructor {
  // Mirrors upstream base/no_destructor.h: trivially destructible
  // types must not be wrapped (a plain function-local static is the
  // correct pattern), so the real build fails here and so does the
  // harness.
  static_assert(!std::is_trivially_destructible_v<T>,
                "T is trivially destructible; please use a function-local "
                "static of type T directly instead");

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
