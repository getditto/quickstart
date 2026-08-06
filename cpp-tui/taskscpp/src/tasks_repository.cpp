#include "tasks_repository.h"
#include "tasks_log.h"

#include "Ditto.h"

#include <iostream>
#include <mutex>
#include <sstream>
#include <stdexcept>

using namespace std;
using json = nlohmann::json;

/// Extract a Task object from a QueryResultItem.
static Task task_from(const ditto::QueryResultItem &item) {
  return json::parse(item.json_string()).template get<Task>();
}

/// Convert a QueryResult to a collection of Task objects.
static vector<Task> tasks_from(const ditto::QueryResult &result) {
  const auto item_count = result.item_count();
  vector<Task> tasks;
  tasks.reserve(item_count);
  for (size_t i = 0; i < item_count; ++i) {
    tasks.emplace_back(task_from(result.get_item(i)));
  }
  return tasks;
}

// Private implementation of the TasksRepository class.
// NOLINTNEXTLINE(cppcoreguidelines-special-member-functions)
class TasksRepository::Impl {
private:
  shared_ptr<mutex> mtx;
  // Shared ownership of the manager keeps the Ditto instance alive for the
  // repository's whole lifetime. `ditto` is cached from `manager->get_ditto()`;
  // `manager` must be declared before `ditto` because `ditto` is initialized
  // from it.
  shared_ptr<DittoManager> manager;
  shared_ptr<ditto::Ditto> ditto;
  shared_ptr<ditto::SyncSubscription> tasks_subscription;

  string select_tasks_query() {
    return "SELECT * FROM tasks WHERE NOT deleted";
  }

public:
  Impl(shared_ptr<DittoManager> ditto_manager)
      : mtx(make_shared<mutex>()), manager(std::move(ditto_manager)),
        ditto(manager->get_ditto()) {
    // Register a subscription, which determines what data syncs to this peer.
    tasks_subscription =
        ditto->get_sync().register_subscription("SELECT * FROM tasks");
  }

  shared_ptr<DittoManager> ditto_manager() const { return manager; }

  ~Impl() noexcept {
    try {
      if (tasks_subscription) {
        tasks_subscription->cancel();
        tasks_subscription.reset();
      }
    } catch (const exception &err) {
      std::cerr << "Failed to destroy tasks repository instance: " +
                       string(err.what())
                << std::endl;
    }
  }

  string add_task(const string &title, bool done) {
    try {
      const json task_args = {
          {"title", title}, {"done", done}, {"deleted", false}};
      const auto command = "INSERT INTO tasks DOCUMENTS (:task)";
      const auto result =
          ditto->get_store().execute(command, {{"task", task_args}});
      auto task_id = result.mutated_document_ids()[0];
      log_debug("Added task: " + task_id.to_string());
      return task_id.to_string();
    } catch (const exception &err) {
      log_error("Failed to add task: " + string(err.what()));
      throw runtime_error("unable to add task: " + string(err.what()));
    }
  }

  vector<Task> get_tasks() {
    try {
      const auto result = ditto->get_store().execute(select_tasks_query());
      const auto tasks = tasks_from(result);
      log_debug("Retrieved tasks; count=" + to_string(tasks.size()));
      return tasks;
    } catch (const exception &err) {
      log_error("Failed to get tasks: " + string(err.what()));
      throw runtime_error("unable to get tasks: " + string(err.what()));
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
      throw runtime_error("unable to delete task: " + string(err.what()));
    }
  }

  shared_ptr<ditto::StoreObserver> register_tasks_observer(
      std::function<void(const std::vector<Task> &)> callback) {
    try {
      const auto observer = ditto->get_store().register_observer(
          select_tasks_query(), [callback](const ditto::QueryResult &result) {
            const auto item_count = result.item_count();
            log_debug("Tasks collection updated; count=" +
                      to_string(item_count));
            const auto tasks = tasks_from(result);
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

}; // class TasksRepository::Impl

TasksRepository::TasksRepository(shared_ptr<DittoManager> manager)
    : impl(make_shared<Impl>(std::move(manager))) {}

shared_ptr<DittoManager> TasksRepository::ditto_manager() const {
  return impl->ditto_manager();
}

TasksRepository::~TasksRepository() noexcept {
  try {
    log_debug("Destroying TasksRepository instance");
  } catch (const std::exception &) { // NOLINT(bugprone-empty-catch)
  }
}

string TasksRepository::add_task(const string &title, bool done) {
  return impl->add_task(title, done);
}

vector<Task> TasksRepository::get_tasks() { return impl->get_tasks(); }

Task TasksRepository::find_matching_task(const string &task_id_substring) {
  return impl->find_matching_task(task_id_substring);
}

void TasksRepository::mark_task_complete(const string &task_id, bool done) {
  impl->mark_task_complete(task_id, done);
}

void TasksRepository::update_task_title(const string &task_id,
                                        const string &title) {
  impl->update_task_title(task_id, title);
}

void TasksRepository::delete_task(const string &task_id) {
  impl->delete_task(task_id);
}

shared_ptr<ditto::StoreObserver> TasksRepository::register_tasks_observer(
    function<void(const std::vector<Task> &)> callback) {
  return impl->register_tasks_observer(callback);
}
