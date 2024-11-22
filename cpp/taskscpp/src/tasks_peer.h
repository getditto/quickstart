#ifndef DITTO_QUICKSTART_TASKS_PEER_H
#define DITTO_QUICKSTART_TASKS_PEER_H

#include <cstdint>
#include <functional>
#include <memory>
#include <vector>

#include "task.h"

/// An agent that can create, read, update, and delete tasks, and sync them with
/// other devices.
class TasksPeer {
public:
  /// Returns a string identifying the version of the Ditto SDK.
  static std::string get_ditto_sdk_version();

  /// By default, all transport types are enabled, but they can be disabled.
  struct TransportConfig {
    bool disable_ble = false;
    bool disable_lan = false;
    bool disable_awdl = false;
    bool disable_wifi_aware = false;
  };

  /// A subscription registration returned by @ref
  /// `register_tasks_observer()`.
  class TasksObserver {

  public:
    virtual ~TasksObserver() noexcept;

    TasksObserver(const TasksObserver &) = delete;
    TasksObserver(const TasksObserver &&) = delete;
    TasksObserver &operator=(const TasksObserver &) = delete;
    TasksObserver &operator=(const TasksObserver &&) = delete;

    /// Cancel the subscription.
    void cancel();

    /// Return true if the subscription has been cancelled, or false otherwise.
    bool is_cancelled();

  private:
    class Impl; // private implementation class
    std::shared_ptr<Impl> impl;
    friend class TasksPeer;
    TasksObserver(Impl *impl);
  };

  /// Construct a new TasksPeer object.
  TasksPeer(std::string ditto_app_id, std::string ditto_online_playground_token,
            bool enable_cloud_sync, std::string ditto_persistence_dir,
            TransportConfig transports);

  virtual ~TasksPeer() noexcept;

  TasksPeer(const TasksPeer &) = default;
  TasksPeer(TasksPeer &&) = default;

  TasksPeer &operator=(const TasksPeer &) = delete;
  TasksPeer &operator=(TasksPeer &&) = delete;

  /// Construct a new TasksPeer object.
  ///
  /// This is provided as an alternative to calling the constructor directly,
  /// which can be complicated when calling C++ code from another language.
  static TasksPeer create(std::string ditto_app_id,
                          std::string ditto_online_playground_token,
                          bool enable_cloud_sync,
                          std::string ditto_persistence_dir,
                          TransportConfig transports);

  /// Stop the peer, disabling it from syncing tasks with other devices.
  void stop_sync();

  /// Create a new task and add it to the collection.
  ///
  /// @return the _id of the new task.
  std::string add_task(const std::string &title, bool done);

  /// Get all tasks in the collection, optionally including those that have been
  /// deleted but are still in the local store.
  ///
  /// This method will return a maximum of 1000 tasks.  If there are more tasks
  /// than that in the collection, some will be ignored.
  ///
  /// @return all tasks in the collection, ordered by ID.
  std::vector<Task> get_tasks(bool include_deleted_tasks = false);

  /// Find a task by its ID.
  ///
  /// @return the Task that exactly matches the specified ID
  ///
  /// @throws TaskException if task cannot be retrieved.
  Task get_task(const std::string &task_id);

  /// Save task properties.
  void update_task(const Task &task);

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
  /// Note that this marks the task as deleted, and it will no longer appear in
  /// `get_tasks()` results, but the object remains in the local store until
  /// @ref `evict_deleted_tasks()` is called.
  void delete_task(const std::string &task_id);

  /// Remove all deleted tasks from the local store.
  void evict_deleted_tasks();

  /// Run a DQL query using the peer's Ditto instance.
  ///
  /// This function is provided for diagnostic purposes.  It should not be used
  /// for general application functionality.
  ///
  /// @param query DQL query string
  /// @return JSON result dictionary with `items` and `modified_document_ids`
  /// arrays.
  std::string execute_dql_query(const std::string &query);

  /// Subscribe to updates to the tasks collection.
  ///
  /// @returns a subscriber object that, when destroyed, will cancel the
  /// subscription.
  std::shared_ptr<TasksObserver> register_tasks_observer(
      const std::function<void(const std::vector<Task> &)> &callback);

  class TasksObserverHandler {
  public:
    TasksObserverHandler();

    virtual ~TasksObserverHandler() noexcept;

    TasksObserverHandler(const TasksObserverHandler &) = delete;
    TasksObserverHandler(const TasksObserverHandler &&) = delete;
    TasksObserverHandler &operator=(const TasksObserverHandler &) = delete;
    TasksObserverHandler &operator=(const TasksObserverHandler &&) = delete;

    /// Called when the tasks collection has been updated.
    virtual void on_tasks_updated(const std::vector<Task> &tasks);
  };

  /// Subscribe to updates to the tasks collection.
  ///
  /// The caller is responsible for ensuring that the handler object outlives
  /// the subscription.
  ///
  /// @returns a subscriber object that, when destroyed, will cancel the
  /// subscription.
  std::shared_ptr<TasksObserver>
  register_tasks_observer(TasksObserverHandler *handler);

private:
  class Impl; // private implementation class
  std::shared_ptr<Impl> impl;
};

#endif // DITTO_QUICKSTART_TASKS_PEER_H
