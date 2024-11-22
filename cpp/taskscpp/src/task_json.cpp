#include "task_json.h"
#include "tasks_exception.h"

void to_json(nlohmann::json &j, const Task &task) {
  try {
    j = nlohmann::json{
        {"title", task.title}, {"done", task.done}, {"deleted", task.deleted}};
    if (!task._id.empty()) {
      j["_id"] = task._id;
    }
  } catch (const std::exception &err) {
    throw TasksException(err);
  }
}

void from_json(const nlohmann::json &j, Task &task) {
  try {
    task._id = j.value("_id", "");
    task.title = j.value("title", "");
    task.done = j.value("done", false);
    task.deleted = j.value("deleted", false);
  } catch (const std::exception &err) {
    throw TasksException(err);
  }
}
