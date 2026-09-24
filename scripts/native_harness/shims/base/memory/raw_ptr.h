#pragma once
#include <cstddef>
namespace base {
// Host-test stand-in for base/memory/raw_ptr.h: a non-owning pointer
// wrapper with the operations our code uses (construct from T*, bool,
// ==, ->, *, get). The real raw_ptr adds PartitionAlloc backup ref
// counting and poisoning; behavior-compatible for the harness.
template <typename T>
class raw_ptr {
 public:
  raw_ptr() = default;
  raw_ptr(std::nullptr_t) {}
  raw_ptr(T* p) : p_(p) {}
  raw_ptr& operator=(T* p) { p_ = p; return *this; }
  raw_ptr& operator=(std::nullptr_t) { p_ = nullptr; return *this; }
  explicit operator bool() const { return p_ != nullptr; }
  T* get() const { return p_; }
  T& operator*() const { return *p_; }
  T* operator->() const { return p_; }
  bool operator==(const raw_ptr& o) const { return p_ == o.p_; }
  bool operator!=(const raw_ptr& o) const { return p_ != o.p_; }
  bool operator==(T* p) const { return p_ == p; }
  bool operator!=(T* p) const { return p_ != p; }
 private:
  T* p_ = nullptr;
};
}  // namespace base

// Mirrors partition_alloc/pointers/raw_ptr.h, which exports raw_ptr to
// the global namespace (line ~1079 upstream) so Chromium code can use
// it unqualified inside any namespace.
using base::raw_ptr;
