#include "tasks_log.h"

// These functions are all thin wrappers over the ditto::Logger API.
// Application code should call these rather than the ditto::Logger API to make
// it easy to change or extend the logging implementation.

void log_error(const std::string &msg) { ditto::Logger::e(msg); }

void log_warning(const std::string &msg) { ditto::Logger::w(msg); }

void log_info(const std::string &msg) { ditto::Logger::i(msg); }

void log_debug(const std::string &msg) { ditto::Logger::d(msg); }

void log_verbose(const std::string &msg) { ditto::Logger::v(msg); }

bool get_logging_enabled() { return ditto::Logger::get_logging_enabled(); }

void set_logging_enabled(bool enabled) {
  ditto::Logger::set_logging_enabled(enabled);
}

ditto::LogLevel get_minimum_log_level() {
  return ditto::Logger::get_minimum_log_level();
}

void set_minimum_log_level(ditto::LogLevel level) {
  ditto::Logger::set_minimum_log_level(level);
}

void export_log(const std::string &path) {
  ditto::Logger::export_to_file(path).get();
}
