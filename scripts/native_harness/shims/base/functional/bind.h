#pragma once
#include <utility>
#include "base/functional/callback.h"
#include "base/memory/weak_ptr.h"
namespace base {
namespace internal {
template <typename F>
class BoundOnce {
 public:
  explicit BoundOnce(F f) : f_(std::move(f)) {}
  template <typename... CallArgs>
  auto Run(CallArgs&&... args) {
    return f_(std::forward<CallArgs>(args)...);
  }
 private:
  F f_;
};
}  // namespace internal

// Host-test shims: bind-and-call semantics only (no cross-thread posting —
// the thread-pool shim executes via a queue drained by TaskEnvironment).
template <typename F, typename... Args>
auto BindRepeating(F f, Args... args) {
  return [f, args...](auto&&... call_args) mutable -> decltype(auto) {
    return f(args..., std::forward<decltype(call_args)>(call_args)...);
  };
}

template <typename R, typename T, typename... MArgs>
auto BindOnce(R (T::*method)(MArgs...), WeakPtr<T> weak) {
  return internal::BoundOnce([method, weak](MArgs... args) -> R {
    if (T* p = weak.get()) {
      return (p->*method)(std::forward<MArgs>(args)...);
    }
    return R();
  });
}

template <typename F, typename... Args>
auto BindOnce(F f, Args... args) {
  return internal::BoundOnce(
      [f, args...]() mutable { return f(args...); });
}
}  // namespace base
