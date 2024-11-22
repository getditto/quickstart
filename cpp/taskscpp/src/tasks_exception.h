#ifndef DITTO_TASKS_EXCEPTION_H
#define DITTO_TASKS_EXCEPTION_H

#include <exception>
#include <string>

namespace tasks {

/// Exception class for the Tasks library.
///
/// All exceptions thrown by the Tasks library are of this type or of standard
/// C++ exception types.  Any other exception types thrown and caught internally
/// will be converted to `TasksException`.
class TasksException : public std::exception {
public:
  explicit TasksException(std::string msg) noexcept : message(std::move(msg)) {}

#ifndef SWIG
  explicit TasksException(const std::exception &e) : message(e.what()) {}
#endif

  const char *what() const noexcept override { return message.c_str(); }

private:
  std::string message;
};

} // namespace tasks

#endif // DITTO_TASKS_EXCEPTION_H
