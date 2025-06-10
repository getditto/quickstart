#include "tasks_peer.h"
#include "tasks_log.h"
#include "join_string_values.h"
#include "transform_container.h"

#include "Ditto.h"

#include <iostream>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <thread>

using namespace std;
using json = nlohmann::json;

namespace {

/// Extract a Task object from a QueryResultItem.
Task task_from(const ditto::QueryResultItem &item) {
  return json::parse(item.json_string()).template get<Task>();
}

/// Convert a QueryResult to a collection of JSON objects.
vector<string> tasks_json_from(const ditto::QueryResult &result) {
  const auto item_count = result.item_count();
  vector<string> tasks;
  tasks.reserve(item_count);
  for (size_t i = 0; i < item_count; ++i) {
    tasks.emplace_back(result.get_item(i).json_string());
  }
  return tasks;
}

/// Initialize a Ditto instance.
unique_ptr<ditto::Ditto> init_ditto(JNIEnv *env,
                                    jobject android_context,
                                    string app_id,
                                    string online_playground_token,
                                    bool enable_cloud_sync,
                                    string persistence_dir,
                                    bool is_running_on_emulator,
                                    string custom_url,
                                    const string& websocket_url) {
  try {

    // TODO UPDATE TO USE CUSTOM URL AND WEBSOCKET
    // Docs:  https://docs.ditto.live/sdk/latest/install-guides/cpp#importing-and-initializing-ditto

    const auto identity = ditto::Identity::OnlinePlayground(
        std::move(app_id),
        std::move(online_playground_token),
        enable_cloud_sync,                  // This is required to be set to false to use the correct URLs
        std::move(custom_url)
        );

    auto ditto =
        make_unique<ditto::Ditto>(android_context, identity, std::move(persistence_dir));

    if (is_running_on_emulator) {
      // Some transports don't work correctly on emulator, so disable them.
      ditto->update_transport_config([websocket_url](ditto::TransportConfig &config) {
          config.peer_to_peer.bluetooth_le.enabled = false;
          config.peer_to_peer.wifi_aware.enabled = false;
          config.connect.websocket_urls.insert(websocket_url);
      });
    } else {
      ditto->update_transport_config([websocket_url](ditto::TransportConfig &config) {
          config.enable_all_peer_to_peer();
          config.connect.websocket_urls.insert(websocket_url);
      });
    }

    // Required for compatibility with DQL.
    ditto->disable_sync_with_v3();

    return ditto;
  } catch (const exception &err) {
    throw runtime_error(string("unable to initialize Ditto: ") + err.what());
  }
}

} // end anonymous namespace

// Private implementation of the TasksPeer class.
class TasksPeer::Impl { // NOLINT(cppcoreguidelines-special-member-functions)
private:
  unique_ptr<mutex> mtx;
  unique_ptr<ditto::Ditto> ditto;
  shared_ptr<ditto::SyncSubscription> tasks_subscription;

public:
  Impl(JNIEnv *env,
       jobject context,
       string app_id,
       string online_playground_token,
       bool enable_cloud_sync,
       string persistence_dir,
       bool is_running_on_emulator,
       string custom_auth_url,
       const string& websocket_url)
      : mtx(new mutex()),
        ditto(init_ditto(
                env,
                context,
                std::move(app_id),
                std::move(online_playground_token),
                enable_cloud_sync,
                std::move(persistence_dir),
                is_running_on_emulator,
                std::move(custom_auth_url),
                websocket_url
                )) {}

  ~Impl() noexcept {
    try {
      stop_sync();
    } catch (const exception &err) {
      cerr << "Failed to destroy tasks peer instance: " +
              string(err.what())
           << endl;
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
      const json task_args = {
          {"title",   title},
          {"done",    done},
          {"deleted", false}};
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

  Task get_task(const string &task_id) {
    try {
      lock_guard<mutex> lock(*mtx);

      if (task_id.empty()) {
        throw invalid_argument("task_id must not be empty");
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

      const auto task = task_from(result.get_item(0));
      log_debug("Retrieved task with _id " + task_id);
      return task;
    } catch (const exception &err) {
      log_error("Failed to get task with _id " + task_id + ": " +
                string(err.what()));
      throw runtime_error("unable to retrieve task: " + string(err.what()));
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
          ditto->get_store().execute(stmt, {{"title",   task.title},
                                            {"done",    task.done},
                                            {"deleted", task.deleted},
                                            {"id",      task._id}});
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
          ditto->get_store().execute(stmt, {{"done", done},
                                            {"id",   task_id}});
      log_debug("Marked task " + task_id +
                (done ? " complete" : " incomplete"));
    } catch (const exception &err) {
      log_error("Failed to mark task complete: " + string(err.what()));
      throw runtime_error("unable to mark task complete: " +
                          string(err.what()));
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

  shared_ptr<ditto::StoreObserver> register_tasks_observer(
      function<void(const vector<string> &)> callback) {
    try {
      const auto observer = ditto->get_store().register_observer(
          "SELECT * FROM tasks WHERE NOT deleted ORDER BY _id",
          [callback = std::move(callback)](const ditto::QueryResult &result) {
            const auto item_count = result.item_count();
            log_debug("Tasks collection updated; count=" +
                      to_string(item_count));
            const auto tasks = tasks_json_from(result);
            try {
              log_debug("Invoking observer callback");
              callback(tasks);
              log_debug("Observer callback completed");
            } catch (const exception &err) {
              log_error("Error in observer callback: " + string(err.what()));
            }
          });

      log_debug("Registered tasks observer");
      return observer;
    } catch (const exception &err) {
      log_error("Failed to register observer: " + string(err.what()));
      throw runtime_error("unable to register observer: " + string(err.what()));
    }
  }

  void insert_initial_tasks() {
    try {
      lock_guard<mutex> lock(*mtx);

      vector<Task> initial_tasks = {
          {"50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"},
          {"6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"},
          {"5303DDF8-0E72-4FEB-9E82-4B007E5797F0",
                                                   "Schedule dentist appointment"},
          {"38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills"}};

      for (const auto &task: initial_tasks) {
        const json task_args = {{"_id",     task._id},
                                {"title",   task.title},
                                {"done",    task.done},
                                {"deleted", task.deleted}};
        const auto command = "INSERT INTO tasks INITIAL DOCUMENTS (:newTask)";
        ditto->get_store().execute(command, {{"newTask", task_args}});
      }
    } catch (const exception &err) {
      log_error("Failed to insert initial tasks: " + string(err.what()));
      throw runtime_error("unable to insert initial tasks: " +
                          string(err.what()));
    }
  }

  vector<string> missing_permissions() const {
    auto result = ditto->missing_permissions();
    log_warning("Missing permissions: " + join_string_values<>(result));
    return result;
  }

  jobjectArray missing_permissions_jni_array() const {
    return ditto->missing_permissions_jni_array();
  }
}; // class TasksPeer::Impl

TasksPeer::TasksPeer(JNIEnv *env, jobject context, string app_id, string online_playground_token,
                     bool enable_cloud_sync, string persistence_dir, bool is_running_on_emulator,
                     string custom_auth_url, const string& websocket_url)
    : impl(make_unique<Impl>(env, context, std::move(app_id), std::move(online_playground_token),
                             enable_cloud_sync, std::move(persistence_dir),
                             is_running_on_emulator, std::move(custom_auth_url), std::move(websocket_url))) {}

TasksPeer::~TasksPeer() noexcept {
  try {
    log_debug("Destroying TasksPeer instance");
  } catch (const exception &err) {
    log_error(string("exception in TasksPeer destructor: ") + err.what());
  }
}

void TasksPeer::start_sync() { impl->start_sync(); }

void TasksPeer::stop_sync() { impl->stop_sync(); }

bool TasksPeer::is_sync_active() const { return impl->is_sync_active(); }

string TasksPeer::add_task(const string &title, bool done) {
  return impl->add_task(title, done);
}

Task TasksPeer::get_task(const string &task_id) {
  return impl->get_task(task_id);
}

void TasksPeer::update_task(const Task &task) { impl->update_task(task); }

void TasksPeer::mark_task_complete(const string &task_id, bool done) {
  impl->mark_task_complete(task_id, done);
}

void TasksPeer::delete_task(const string &task_id) {
  impl->delete_task(task_id);
}

shared_ptr<ditto::StoreObserver> TasksPeer::register_tasks_observer(
    function<void(const vector<string> &)> callback) {
  return impl->register_tasks_observer(std::move(callback));
}

string TasksPeer::get_ditto_sdk_version() {
  return ditto::Ditto::get_sdk_version();
}

void TasksPeer::insert_initial_tasks() { impl->insert_initial_tasks(); }
