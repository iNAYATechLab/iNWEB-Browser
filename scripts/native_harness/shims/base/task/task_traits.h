#pragma once
namespace base {
enum class TaskPriority { BEST_EFFORT, USER_VISIBLE, USER_BLOCKING };
struct TaskTraits {
  constexpr TaskTraits() = default;
  constexpr TaskTraits(TaskPriority) {}
};
}  // namespace base
