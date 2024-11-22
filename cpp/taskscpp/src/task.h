#ifndef DITTO_TASKSLIB_TASK_H
#define DITTO_TASKSLIB_TASK_H

#include <string>

namespace tasks {

/// Representation of a to-do item.
struct Task {
  std::string _id;
  std::string title;
  bool done = false;
  bool deleted = false;

  Task() = default;

  Task(const std::string &id, const std::string &title, bool done,
       bool deleted = false)
      : _id(id), title(title), done(done), deleted(deleted) {}
};

} // namespace tasks

#endif // TASKSLIB_TASK_H
