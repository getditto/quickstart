#ifndef DITTO_QUICKSTART_TASKS_REPOSITORY_H
#define DITTO_QUICKSTART_TASKS_REPOSITORY_H

#include <functional>
#include <memory>
#include <string>
#include <vector>

#include "ditto_manager.h"
#include "task.h"

/// Owns everything specific to the tasks data: the sync subscription, the
/// task CRUD operations, and registration of the store observer that streams
/// the current task list. It talks to Ditto directly through the instance
/// vended by DittoManager.
class TasksRepository {
public:
  /// Construct a repository backed by the manager's Ditto instance. This
  /// registers the sync subscription so Ditto syncs matching documents from
  /// other devices.
  TasksRepository(const DittoManager &manager);

  virtual ~TasksRepository() noexcept;

  TasksRepository(const TasksRepository &) = default;
  TasksRepository(TasksRepository &&) = default;

  TasksRepository &operator=(const TasksRepository &) = delete;
  TasksRepository &operator=(TasksRepository &&) = delete;

  /// Create a new task and add it to the collection.
  ///
  /// @return the _id of the new task.
  std::string add_task(const std::string &title, bool done);

  /// Get all tasks in the collection.
  ///
  /// This method will return a maximum of 1000 tasks.  If there are more tasks
  /// than that in the collection, some will be ignored.
  ///
  /// @return all tasks in the collection, ordered by ID.
  std::vector<Task> get_tasks();

  /// Find a task by a substring of its ID.
  ///
  /// This function is provided for use by command-line interfaces or other
  /// kinds of apps where entering a full task ID is inconvenient.
  ///
  /// @return the Task that matches the specified ID
  ///
  /// @throws TaskException if task cannot be found, or if there are multiple
  /// matches.
  Task find_matching_task(const std::string &task_id_substring);

  /// Mark task as completed or not completed.
  void mark_task_complete(const std::string &task_id, bool done);

  /// Change the title of the specified task
  void update_task_title(const std::string &task_id, const std::string &title);

  /// Delete the specified task from the collection.
  ///
  /// Note that this marks the task as deleted, so it will no longer appear in
  /// `get_tasks()` results; the object remains in the local store.
  void delete_task(const std::string &task_id);

  /// Subscribe to updates to the tasks collection.
  ///
  /// @returns a subscriber object that, when destroyed, will cancel the
  /// subscription.
  std::shared_ptr<ditto::StoreObserver> register_tasks_observer(
      std::function<void(const std::vector<Task> &)> callback);

private:
  class Impl; // private implementation class ("pimpl pattern")
  std::shared_ptr<Impl> impl;
};

#endif // DITTO_QUICKSTART_TASKS_REPOSITORY_H
