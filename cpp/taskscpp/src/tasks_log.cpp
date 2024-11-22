#include "tasks_log.h"
#include "tasks_exception.h"

// These functions are all thin wrappers over the ditto::Log() API.  Application
// code should call these rather than the ditto::Log() API to make it easy to
// change or extend the logging implementation.

void log_error(const std::string &msg) { ditto::Log::e(msg); }

void log_warning(const std::string &msg) { ditto::Log::w(msg); }

void log_info(const std::string &msg) { ditto::Log::i(msg); }

void log_debug(const std::string &msg) { ditto::Log::d(msg); }

void log_verbose(const std::string &msg) { ditto::Log::v(msg); }

bool get_logging_enabled() { return ditto::Log::get_logging_enabled(); }

void set_logging_enabled(bool enabled) {
  ditto::Log::set_logging_enabled(enabled);
}

ditto::LogLevel get_minimum_log_level() {
  return ditto::Log::get_minimum_log_level();
}

void set_minimum_log_level(ditto::LogLevel level) {
  ditto::Log::set_minimum_log_level(static_cast<ditto::LogLevel>(level));
}

void set_log_file(const std::string &path) { ditto::Log::set_log_file(path); }

void disable_log_file() { ditto::Log::disable_log_file(); }

void export_log(const std::string &path) {
  ditto::Log::export_to_file(path).get();
}
