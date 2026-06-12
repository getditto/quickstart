#ifndef DITTO_QUICKSTART_TASKS_LOG_H
#define DITTO_QUICKSTART_TASKS_LOG_H

#include "Ditto.h"

// Application-level logging functions

void log_error(const std::string &msg);
void log_warning(const std::string &msg);
void log_info(const std::string &msg);
void log_debug(const std::string &msg);
void log_verbose(const std::string &msg);

bool get_logging_enabled();
void set_logging_enabled(bool enabled);

ditto::LogLevel get_minimum_log_level();
void set_minimum_log_level(ditto::LogLevel level);

void export_log(const std::string &path);

#endif // DITTO_QUICKSTART_TASKS_LOG_H
