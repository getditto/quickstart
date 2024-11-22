#ifndef DITTO_QUICKSTART_TASK_H
#define DITTO_QUICKSTART_TASK_H

#include <string>

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

#endif // DITTO_QUICKSTART_TASK_H
