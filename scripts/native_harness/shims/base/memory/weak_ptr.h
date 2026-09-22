#pragma once
namespace base {
template <typename T>
class WeakPtrFactory;
template <typename T>
class WeakPtr {
 public:
  WeakPtr() = default;
  T* get() const { return p_; }
  explicit operator bool() const { return p_ != nullptr; }
 private:
  friend class WeakPtrFactory<T>;
  explicit WeakPtr(T* p) : p_(p) {}
  T* p_ = nullptr;
};
template <typename T>
class WeakPtrFactory {
 public:
  explicit WeakPtrFactory(T* p) : p_(p) {}
  WeakPtr<T> GetWeakPtr() { return WeakPtr<T>(p_); }
  void DetachFromThread() {}
  void InvalidateWeakPtrs() {}
 private:
  T* p_;
};
}  // namespace base
