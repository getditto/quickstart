#ifndef DITTO_QUICKSTART_TASK_JSON_H
#define DITTO_QUICKSTART_TASK_JSON_H

#include "task.h"

#include "Ditto.h" // includes nlohmann::json library

// For information about how the nlohmann::json library handles
// serialization/deserialization of C++ types, see
// <https://github.com/nlohmann/json#arbitrary-types-conversions>

/// Copies data from a Task to a JSON object.
void to_json(nlohmann::json &j, const Task &task);

/// Copies data from a JSON object to a Task.
void from_json(const nlohmann::json &j, Task &task);

#endif // DITTO_QUICKSTART_TASK_JSON_H
