#ifndef DITTO_QUICKSTART_TASK_H
#define DITTO_QUICKSTART_TASK_H

#include <string>

#include "Ditto.h"

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

  Task(std::string id, std::string ttl, bool is_done = false,
       bool is_deleted = false)
      : _id(std::move(id)), title(std::move(ttl)), done(is_done), deleted(is_deleted) {}

  bool operator==(const Task &other) const {
    return _id == other._id &&     //
           title == other.title && //
           done == other.done &&   //
           deleted == other.deleted;
  }
};

// For information about how the nlohmann::json library handles
// serialization/deserialization of C++ types, see
// <https://github.com/nlohmann/json#arbitrary-types-conversions>

/// Copies data from a Task to a JSON object.
void to_json(nlohmann::json &j, const Task &task);

/// Copies data from a JSON object to a Task.
void from_json(const nlohmann::json &j, Task &task);

#endif // DITTO_QUICKSTART_TASK_H
