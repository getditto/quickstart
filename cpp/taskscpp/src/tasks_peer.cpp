#include "tasks_peer.h"
#include "task_json.h"
#include "tasks_log.h"
#include "transform_container.h"

#include "Ditto.h"

#include <iostream>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <thread>

using namespace std;
using json = nlohmann::json;

/// Extract a Task object from a QueryResultItem.
static Task task_from(const ditto::QueryResultItem &item) {
  return json::parse(item.json_string()).template get<Task>();
}

/// Convert a QueryResult to a JSON string
static string to_json_string(const ditto::QueryResult &result) {
  const auto items = transform_container<vector<string>>(
      result.items(),
      [](const ditto::QueryResultItem &item) { return item.json_string(); });

  const auto modified_document_ids = transform_container<vector<string>>(
      result.mutated_document_ids(),
      [](const ditto::DocumentId &id) { return id.to_string(); });

  nlohmann::json result_json = {
      {"items", items}, {"modified_document_ids", modified_document_ids}};
  return result_json.dump();
}

/// Initialize a Ditto instance.
static shared_ptr<ditto::Ditto>
init_ditto(string app_id, string online_playground_token,
           bool enable_cloud_sync, string persistence_dir,
           TasksPeer::TransportConfig xport_cfg) {
  try {
    const auto identity = ditto::Identity::OnlinePlayground(
        std::move(app_id), std::move(online_playground_token),
        enable_cloud_sync);

    auto ditto =
        std::make_shared<ditto::Ditto>(identity, std::move(persistence_dir));

    ditto->update_transport_config(
        [&xport_cfg](ditto::TransportConfig &ditto_xport_cfg) {
          if (xport_cfg.disable_ble) {
            ditto_xport_cfg.peer_to_peer.bluetooth_le.enabled = false;
          }
          if (xport_cfg.disable_lan) {
            ditto_xport_cfg.peer_to_peer.lan.enabled = false;
          }
          if (xport_cfg.disable_awdl) {
            ditto_xport_cfg.peer_to_peer.awdl.enabled = false;
          }
          if (xport_cfg.disable_wifi_aware) {
            ditto_xport_cfg.peer_to_peer.wifi_aware.enabled = false;
          }
        });

    // Required for compatibility with DQL.
    ditto->disable_sync_with_v3();

    ditto->get_small_peer_info().set_enabled(true);

    return ditto;
  } catch (const exception &err) {
    throw runtime_error("unable to initialize Ditto: " + string(err.what()));
  }
}

// Private implementation of the TasksPeer::TasksObserver class.
class TasksPeer::TasksObserver::
    Impl { // NOLINT(cppcoreguidelines-special-member-functions)

private:
  shared_ptr<ditto::StoreObserver> observer;

public:
  explicit Impl(shared_ptr<ditto::StoreObserver> observer)
      : observer(std::move(observer)) {}

  ~Impl() noexcept {
    try {
      observer->cancel();
    } catch (const exception &err) {
      log_error("Failed to cancel observer: " + string(err.what()));
    }
  }

  void cancel() { observer->cancel(); }

  bool is_cancelled() const { return observer->is_cancelled(); }
};

TasksPeer::TasksObserver::TasksObserver(Impl *impl) : impl(impl) {}

TasksPeer::TasksObserver::~TasksObserver() noexcept = default;

void TasksPeer::TasksObserver::cancel() { impl->cancel(); }

bool TasksPeer::TasksObserver::is_cancelled() { return impl->is_cancelled(); }

// Private implementation of the TasksPeer class.
class TasksPeer::Impl { // NOLINT(cppcoreguidelines-special-member-functions)
private:
  shared_ptr<mutex> mtx;
  shared_ptr<ditto::Ditto> ditto;
  shared_ptr<ditto::SyncSubscription> tasks_subscription;

  string select_tasks_query(bool include_deleted_tasks = false) {
    if (include_deleted_tasks) {
      return "SELECT * FROM tasks ORDER BY _id";
    } else {
      return "SELECT * FROM tasks WHERE NOT deleted ORDER BY _id";
    }
  }

public:
  Impl(string app_id, string online_playground_token, bool enable_cloud_sync,
       string persistence_dir, TransportConfig transports)
      : mtx(new mutex()),
        ditto(init_ditto(std::move(app_id), std::move(online_playground_token),
                         enable_cloud_sync, std::move(persistence_dir),
                         transports)) {}

  ~Impl() noexcept {
    try {
      stop_sync();
    } catch (const exception &err) {
      std::cerr << "Failed to destroy tasks peer instance: " +
                       string(err.what())
                << std::endl;
    }
  }

  void start_sync() {
    if (is_sync_active()) {
      return;
    }

    ditto->start_sync();
    tasks_subscription =
        ditto->sync().register_subscription("SELECT * FROM tasks");
  }

  void stop_sync() {
    if (!is_sync_active()) {
      return;
    }

    tasks_subscription->cancel();
    tasks_subscription.reset();
    ditto->stop_sync();
  }

  bool is_sync_active() const { return ditto->get_is_sync_active(); }

  string add_task(const string &title, bool done) {
    try {
      lock_guard<mutex> lock(*mtx);

      const json task_args = {
          {"title", title}, {"done", done}, {"deleted", false}};
      const auto command = "INSERT INTO tasks DOCUMENTS (:newTask)";
      const auto result =
          ditto->get_store().execute(command, {{"newTask", task_args}});

      auto task_id = result.mutated_document_ids()[0].to_string();
      log_debug("Added task: " + task_id);
      return task_id;
    } catch (const exception &err) {
      log_error("Failed to add task: " + string(err.what()));
      throw runtime_error("unable to add task: " + string(err.what()));
    }
  }

  vector<Task> get_tasks(bool include_deleted_tasks) {
    try {
      lock_guard<mutex> lock(*mtx);

      const auto result =
          ditto->get_store().execute(select_tasks_query(include_deleted_tasks));
      const auto item_count = result.item_count();

      vector<Task> tasks;
      tasks.reserve(item_count);

      for (size_t i = 0; i < item_count; ++i) {
        const auto task = task_from(result.get_item(i));
        tasks.emplace_back(std::move(task));
      }

      log_debug("Retrieved tasks; count=" + to_string(item_count));
      return tasks;
    } catch (const exception &err) {
      log_error("Failed to get tasks: " + string(err.what()));
      throw runtime_error("unable to get tasks: " + string(err.what()));
    }
  }

  Task get_task(const string &task_id) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id.empty()) {
        throw invalid_argument("id_substring must not be empty");
      }
      const auto query = "SELECT * FROM tasks WHERE _id = :id AND NOT deleted";
      const auto result = ditto->get_store().execute(query, {{"id", task_id}});
      const auto item_count = result.item_count();
      if (item_count == 0) {
        throw runtime_error(string("no tasks found with id \"") + task_id +
                            "\"");
      } else if (item_count > 1) {
        throw runtime_error("more than one task found with id \"" + task_id +
                            "\"");
      }

      auto task = task_from(result.get_item(0));
      log_debug("Retrieved task with _id " + task_id);
      return task;
    } catch (const exception &err) {
      log_error("Failed to get task with _id " + task_id + ": " +
                string(err.what()));
      throw runtime_error("unable to retrieve task: " + string(err.what()));
    }
  }

  Task find_matching_task(const string &task_id_substring) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id_substring.empty()) {
        throw invalid_argument("id_substring must not be empty");
      }
      const auto query = "SELECT * FROM tasks"
                         " WHERE contains(_id, :idSubstring)"
                         " AND NOT deleted";
      const auto result = ditto->get_store().execute(
          query, {{"idSubstring", task_id_substring}});
      const auto item_count = result.item_count();
      if (item_count == 0) {
        throw runtime_error(string("no tasks found with id containing \"") +
                            task_id_substring + "\"");
      } else if (item_count > 1) {
        throw runtime_error("more than one task found with id containing \"" +
                            task_id_substring + "\"");
      }

      auto task = task_from(result.get_item(0));
      log_debug("Found matching task for " + task_id_substring + ": " +
                task._id);
      return task;
    } catch (const exception &err) {
      log_error("Failed to find matching task: " + string(err.what()));
      throw runtime_error("unable to find matching task: " +
                          string(err.what()));
    }
  }

  void update_task(const Task &task) {
    try {
      lock_guard<mutex> lock(*mtx);

      const auto stmt = "UPDATE tasks SET"
                        " title = :title,"
                        " done = :done,"
                        " deleted = :deleted"
                        " WHERE _id = :id";
      const auto result =
          ditto->get_store().execute(stmt, {{"title", task.title},
                                            {"done", task.done},
                                            {"deleted", task.deleted},
                                            {"id", task._id}});
      if (result.mutated_document_ids().empty()) {
        throw runtime_error("task not found with ID: " + task._id);
      }
      log_debug("Updated task: " + task._id);
    } catch (const exception &err) {
      log_error("Failed to update task: " + string(err.what()));
      throw runtime_error("unable to update task: " + string(err.what()));
    }
  }

  void mark_task_complete(const string &task_id, bool done) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id.empty()) {
        throw invalid_argument("task ID must not be empty");
      }

      const auto stmt = "UPDATE tasks SET done = :done WHERE _id = :id";
      const auto result =
          ditto->get_store().execute(stmt, {{"done", done}, {"id", task_id}});
      log_debug("Marked task " + task_id +
                (done ? " complete" : " incomplete"));
    } catch (const exception &err) {
      log_error("Failed to mark task complete: " + string(err.what()));
      throw runtime_error("unable to mark task complete: " +
                          string(err.what()));
    }
  }

  void update_task_title(const string &task_id, const string &title) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id.empty()) {
        throw invalid_argument("task ID must not be empty");
      }

      const auto stmt = "UPDATE tasks SET title = :title WHERE _id = :id";
      const auto result =
          ditto->get_store().execute(stmt, {{"title", title}, {"id", task_id}});
      if (result.mutated_document_ids().empty()) {
        throw runtime_error("task not found with ID: " + task_id);
      }
      log_debug("Updated task title: " + task_id);
    } catch (const exception &err) {
      log_error("Failed to update task title: " + string(err.what()));
      throw runtime_error("unable to update task title: " + string(err.what()));
    }
  }

  void delete_task(const string &task_id) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id.empty()) {
        throw invalid_argument("task ID must not be empty");
      }

      const auto stmt = "UPDATE tasks SET deleted = true WHERE _id = :id";
      const auto result = ditto->get_store().execute(stmt, {{"id", task_id}});
      if (result.mutated_document_ids().empty()) {
        throw runtime_error("task not found with ID: " + task_id);
      }
      log_debug("Deleted task: " + task_id);
    } catch (const exception &err) {
      log_error("Failed to delete task: " + string(err.what()));
      throw runtime_error("unable to evict task: " + string(err.what()));
    }
  }

  void evict_deleted_tasks() {
    try {
      lock_guard<mutex> lock(*mtx);

      const auto stmt = "EVICT FROM tasks WHERE deleted = true";
      ditto->get_store().execute(stmt);
      log_debug("Evicted deleted tasks");
    } catch (const exception &err) {
      log_error("Failed to evict deleted tasks: " + string(err.what()));
      throw runtime_error("unable to evict task: " + string(err.what()));
    }
  }

  shared_ptr<TasksPeer::TasksObserver> register_tasks_observer(
      const std::function<void(const std::vector<Task> &)> &callback) {
    try {
      const auto observer = ditto->get_store().register_observer(
          select_tasks_query(), [callback](const ditto::QueryResult &result) {
            const auto item_count = result.item_count();
            log_debug("Tasks collection updated; count=" +
                      to_string(item_count));
            vector<Task> tasks;
            tasks.reserve(item_count);
            for (auto i = 0; i < item_count; ++i) {
              tasks.emplace_back(task_from(result.get_item(i)));
            }

            try {
              log_debug("Invoking observer callback");
              callback(tasks);
              log_debug("Observer callback completed");
            } catch (const exception &err) {
              log_error("Error in observer callback: " + string(err.what()));
            }
          });

      log_debug("Registered tasks observer");
      auto *observer_impl = new TasksObserver::Impl(observer);
      return shared_ptr<TasksObserver>(new TasksObserver(observer_impl));
    } catch (const exception &err) {
      log_error("Failed to register observer: " + string(err.what()));
      throw runtime_error("unable to register observer: " + string(err.what()));
    }
  }

  string execute_dql_query(const string &query) {
    try {
      lock_guard<mutex> lock(*mtx);

      const auto result = ditto->get_store().execute(query);
      log_debug("Executed DQL query");
      return to_json_string(result);
    } catch (const exception &err) {
      log_error("Failed to execute DQL query: " + string(err.what()));
      throw runtime_error("unable to execute DQL query: " + string(err.what()));
    }
  }
}; // class TasksPeer::Impl

TasksPeer TasksPeer::create(string ditto_app_id,
                            string ditto_online_playground_token,
                            bool enable_cloud_sync,
                            string ditto_persistence_dir,
                            TransportConfig transports) {
  return {std::move(ditto_app_id), std::move(ditto_online_playground_token),
          enable_cloud_sync, std::move(ditto_persistence_dir), transports};
}

TasksPeer::TasksPeer(string app_id, string online_playground_token,
                     bool enable_cloud_sync, string persistence_dir,
                     TasksPeer::TransportConfig transports)
    : impl(new Impl(std::move(app_id), std::move(online_playground_token),
                    enable_cloud_sync, std::move(persistence_dir),
                    transports)) {}

TasksPeer::~TasksPeer() noexcept {
  try {
    log_debug("Destroying TasksPeer instance");
  } catch (const std::exception &) { // NOLINT(bugprone-empty-catch)
  }
}

void TasksPeer::start_sync() { impl->start_sync(); }

void TasksPeer::stop_sync() { impl->stop_sync(); }

bool TasksPeer::is_sync_active() const { return impl->is_sync_active(); }

string TasksPeer::add_task(const string &title, bool done) {
  return impl->add_task(title, done);
}

vector<Task> TasksPeer::get_tasks(bool include_deleted_tasks) {
  return impl->get_tasks(include_deleted_tasks);
}

Task TasksPeer::get_task(const string &task_id) {
  return impl->get_task(task_id);
}

Task TasksPeer::find_matching_task(const string &task_id_substring) {
  return impl->find_matching_task(task_id_substring);
}

void TasksPeer::update_task(const Task &task) { impl->update_task(task); }

void TasksPeer::mark_task_complete(const string &task_id, bool done) {
  impl->mark_task_complete(task_id, done);
}

void TasksPeer::update_task_title(const string &task_id, const string &title) {
  impl->update_task_title(task_id, title);
}

void TasksPeer::delete_task(const string &task_id) {
  impl->delete_task(task_id);
}

void TasksPeer::evict_deleted_tasks() { impl->evict_deleted_tasks(); }

shared_ptr<TasksPeer::TasksObserver> TasksPeer::register_tasks_observer(
    const function<void(const std::vector<Task> &)> &callback) {
  return impl->register_tasks_observer(callback);
}

shared_ptr<TasksPeer::TasksObserver>
TasksPeer::register_tasks_observer(TasksPeer::TasksObserverHandler *handler) {
  return impl->register_tasks_observer(
      [handler](const std::vector<Task> &tasks) {
        handler->on_tasks_updated(tasks);
      });
}

string TasksPeer::execute_dql_query(const string &query) {
  return impl->execute_dql_query(query);
}

string TasksPeer::get_ditto_sdk_version() {
  return ditto::Ditto::get_sdk_version();
}

TasksPeer::TasksObserverHandler::TasksObserverHandler() {
  log_debug("TasksObserverHandler created");
}

TasksPeer::TasksObserverHandler::~TasksObserverHandler() noexcept {
  try {
    log_debug("TasksObserverHandler destroyed");
  } catch (const std::exception &e) {
    std::cerr << "Error in ~TasksObserverHandler: " << e.what() << std::endl;
  }
}

void TasksPeer::TasksObserverHandler::on_tasks_updated(
    const std::vector<Task> &tasks) {
  log_warning("TasksObserverHandler::on_tasks_updated should be overridden");
}
