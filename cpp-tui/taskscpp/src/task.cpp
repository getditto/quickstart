#include "task.h"

void to_json(nlohmann::json &j, const Task &task) {
  j = nlohmann::json{
      {"title", task.title}, {"done", task.done}, {"deleted", task.deleted}};
  if (!task._id.empty()) {
    j["_id"] = task._id;
  }
}

void from_json(const nlohmann::json &j, Task &task) {
  task._id = j.value("_id", "");
  task.title = j.value("title", "");
  task.done = j.value("done", false);
  task.deleted = j.value("deleted", false);
}
