#pragma once
#include <functional>
namespace base {
// Host-test shim: only the single-argument form the engine throttle uses.
template <typename Sig>
class RepeatingCallback;
template <typename R, typename A>
class RepeatingCallback<R(A)> {
 public:
  RepeatingCallback() = default;
  template <typename F>
  RepeatingCallback(F f) : fn_(std::move(f)) {}
  R Run(const A& arg) const { return fn_(arg); }
  explicit operator bool() const { return static_cast<bool>(fn_); }
 private:
  std::function<R(A)> fn_;
};
}  // namespace base
