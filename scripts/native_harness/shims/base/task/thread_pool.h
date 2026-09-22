#pragma once
#include <functional>
#include <utility>
#include <vector>
#include "base/location.h"
#include "base/task/task_traits.h"
namespace base {
class ThreadPool {
 public:
  // Host-test shim: posts to a task queue drained by
  // base::test::TaskEnvironment::RunUntilIdle() — preserving the deferred
  // (async) shape the real API has.
  static std::vector<std::function<void()>>& PendingTasks() {
    static std::vector<std::function<void()>> tasks;
    return tasks;
  }

  template <typename Task, typename Reply>
  static bool PostTaskAndReplyWithResult(const Location&, TaskTraits,
                                         Task&& task, Reply&& reply) {
    PendingTasks().push_back(
        [task = std::move(task), reply = std::move(reply)]() mutable {
          reply.Run(task.Run());
        });
    return true;
  }
};
}  // namespace base
