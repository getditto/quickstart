#ifndef DITTO_QUICKSTART_TASK_H
#define DITTO_QUICKSTART_TASK_H

#include <string>

/// Representation of a to-do item.
///
/// If data members of this struct are changed, the `to_json()` and
/// `from_json()` functions in tasks_json.cpp must be updated to match.
struct Task {
  std::string _id;
  std::string title;
  bool done = false;
  bool deleted = false;

  Task() = default;

  Task(const std::string &id, const std::string &title, bool done = false,
       bool deleted = false)
      : _id(id), title(title), done(done), deleted(deleted) {}

  bool operator==(const Task &other) const {
    return _id == other._id &&     //
           title == other.title && //
           done == other.done &&   //
           deleted == other.deleted;
  }
};

#endif // DITTO_QUICKSTART_TASK_H
