#ifndef DITTO_QUICKSTART_TASK_JSON_H
#define DITTO_QUICKSTART_TASK_JSON_H

// This header file is not part of the public API of the Tasks library.
// It directly references the Ditto SDK, so is only used internally.

#include "task.h"

#include "Ditto.h" // for nhlohmann::json

/// Copies data from a Task to a JSON object.
void to_json(nlohmann::json &j, const Task &task);

/// Copies data from a JSON object to a Task.
void from_json(const nlohmann::json &j, Task &task);

#endif // DITTO_QUICKSTART_TASK_JSON_H
