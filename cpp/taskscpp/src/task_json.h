#ifndef TASKSLIB_TASK_JSON_H
#define TASKSLIB_TASK_JSON_H

// This header file is not part of the public API of the Tasks library.
// It directly references the Ditto SDK, so is only used internally.

#include "task.h"

#include "Ditto.h" // for nhlohmann::json

namespace tasks {

/// Copies data from a Task to a JSON object.
void to_json(nlohmann::json &j, const Task &task);

/// Copies data from a JSON object to a Task.
void from_json(const nlohmann::json &j, Task &task);

} // namespace tasks

#endif // TASKSLIB_TASK_JSON_H
