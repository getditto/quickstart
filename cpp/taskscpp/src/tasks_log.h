#ifndef DITTO_TASKSLIB_TASKS_LOG_H
#define DITTO_TASKSLIB_TASKS_LOG_H

#include <string>

// The Tasks library uses the underlying Ditto SDK for logging, but does not
// directly expose any Ditto APIs.  Instead it provides these functions which
// call the Ditto SDK functions.  See
// <https://software.ditto.live/cpp/Ditto/4.8.0/api-reference/classditto_1_1_log.html>
// for more information about what these functions do.

namespace tasks {

enum class LogLevel { error = 1, warning, info, debug, verbose };

void log_error(const std::string &msg);
void log_warning(const std::string &msg);
void log_info(const std::string &msg);
void log_debug(const std::string &msg);
void log_verbose(const std::string &msg);

bool get_logging_enabled();
void set_logging_enabled(bool enabled);

LogLevel get_minimum_log_level();
void set_minimum_log_level(LogLevel level);

void set_log_file(const std::string &path);
void disable_log_file();

void export_log(const std::string &path);

} // namespace tasks

#endif // DITTO_TASKSLIB_TASKS_LOG_H
