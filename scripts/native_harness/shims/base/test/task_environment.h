#pragma once
#include <utility>
#include "base/task/thread_pool.h"
namespace base::test {
class TaskEnvironment {
 public:
  enum class ThreadPoolPointerSupport { kNoPointerSupport };
  TaskEnvironment() = default;
  explicit TaskEnvironment(ThreadPoolPointerSupport) {}
  void RunUntilIdle() {
    while (!base::ThreadPool::PendingTasks().empty()) {
      std::vector<std::function<void()>> tasks =
          std::move(base::ThreadPool::PendingTasks());
      base::ThreadPool::PendingTasks().clear();
      for (auto& task : tasks) {
        task();
      }
    }
  }
};
}  // namespace base::test
