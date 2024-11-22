#include "tasks_log.h"
#include "tasks_exception.h"

#include "Ditto.h"

// Any thrown exception will be converted to a TasksException.
//
// NOLINTNEXTLINE(cppcoreguidelines-macro-usage)
#define WRAP_EXCEPTION(stmt)                                                   \
  try {                                                                        \
    stmt;                                                                      \
  } catch (const std::exception &err) {                                        \
    throw tasks::TasksException(err.what());                                   \
  }

void tasks::log_error(const std::string &msg) {
  WRAP_EXCEPTION(ditto::Log::e(msg))
}

void tasks::log_warning(const std::string &msg) {
  WRAP_EXCEPTION(ditto::Log::w(msg))
}

void tasks::log_info(const std::string &msg) {
  WRAP_EXCEPTION(ditto::Log::i(msg))
}

void tasks::log_debug(const std::string &msg) {
  WRAP_EXCEPTION(ditto::Log::d(msg))
}

void tasks::log_verbose(const std::string &msg) {
  WRAP_EXCEPTION(ditto::Log::v(msg))
}

bool tasks::get_logging_enabled() {
  WRAP_EXCEPTION(return ditto::Log::get_logging_enabled())
}

void tasks::set_logging_enabled(bool enabled){
    WRAP_EXCEPTION(ditto::Log::set_logging_enabled(enabled))}

tasks::LogLevel tasks::get_minimum_log_level() {
  WRAP_EXCEPTION(
      return static_cast<tasks::LogLevel>(ditto::Log::get_minimum_log_level()))
}

void tasks::set_minimum_log_level(tasks::LogLevel level) {
  WRAP_EXCEPTION(
      ditto::Log::set_minimum_log_level(static_cast<ditto::LogLevel>(level)))
}

void tasks::set_log_file(const std::string &path) {
  WRAP_EXCEPTION(ditto::Log::set_log_file(path))
}

void tasks::disable_log_file() {
  WRAP_EXCEPTION(ditto::Log::disable_log_file())
}

void tasks::export_log(const std::string &path) {
  WRAP_EXCEPTION(ditto::Log::export_to_file(path).get())
}
