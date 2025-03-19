#ifndef DITTO_QUICKSTART_TASKS_PEER_H
#define DITTO_QUICKSTART_TASKS_PEER_H

#include <cstdint>
#include <functional>
#include <memory>
#include <vector>

#include "task.h"

#include <jni.h>

/// An agent that can create, read, update, and delete tasks, and sync them with
/// other devices.
class TasksPeer {
public:
  /// Returns a string identifying the version of the Ditto SDK.
  static std::string get_ditto_sdk_version();

  /// Construct a new TasksPeer object.
  TasksPeer(JNIEnv *env,
            jobject context,
            std::string ditto_app_id,
            std::string ditto_online_playground_token,
            bool enable_cloud_sync,
            std::string ditto_persistence_dir,
            bool is_running_on_emulator,
            std::string ditto_custom_auth_url,
            const std::string& ditto_websocket_url);

  virtual ~TasksPeer() noexcept;

  TasksPeer(const TasksPeer &) = delete;

  TasksPeer(TasksPeer &&) = delete;

  TasksPeer &operator=(const TasksPeer &) = delete;

  TasksPeer &operator=(TasksPeer &&) = delete;

  /// Start the peer, enabling it to sync tasks with other devices.
  void start_sync();

  /// Stop the peer, disabling it from syncing tasks with other devices.
  void stop_sync();

  /// Return true if peer is currently syncing tasks with other devices.
  bool is_sync_active() const;

  /// Create a new task and add it to the collection.
  ///
  /// @return the _id of the new task.
  std::string add_task(const std::string &title, bool done);

  /// Find a task by its ID.
  ///
  /// @return the Task that has the specified ID
  ///
  /// @throws TaskException if task cannot be retrieved.
  Task get_task(const std::string &task_id);

  /// Save task properties.
  void update_task(const Task &task);

  /// Mark task as completed or not completed.
  void mark_task_complete(const std::string &task_id, bool done);

  /// Delete the specified task from the collection.
  ///
  /// Note that this marks the task as deleted, but the object remains in the local store.
  void delete_task(const std::string &task_id);

  /// Subscribe to updates to the tasks collection.
  ///
  /// The given callback will be invoked with a vector of strings containing JSON representations
  /// of the elements in the tasks collection.
  ///
  /// @returns a subscriber object that, when destroyed, will cancel the
  /// subscription.
  std::shared_ptr<ditto::StoreObserver> register_tasks_observer(
      std::function<void(const std::vector<std::string> &tasksJson)> callback);

  /// Add a set of initial documents to the tasks collection.
  void insert_initial_tasks();

private:
  class Impl; // private implementation class ("pimpl pattern")
  std::unique_ptr<Impl> impl;
};

#endif // DITTO_QUICKSTART_TASKS_PEER_H
