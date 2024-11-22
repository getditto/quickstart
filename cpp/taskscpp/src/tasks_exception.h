#ifndef DITTO_QUICKSTART_TASKS_EXCEPTION_H
#define DITTO_QUICKSTART_TASKS_EXCEPTION_H

#include <exception>
#include <string>

/// Exception class for the Tasks library.
///
/// All exceptions thrown by the Tasks library are of this type or of standard
/// C++ exception types.  Any other exception types thrown and caught internally
/// will be converted to `TasksException`.
class TasksException : public std::exception {
public:
  explicit TasksException(std::string msg) noexcept : message(std::move(msg)) {}

  explicit TasksException(const std::exception &e) : message(e.what()) {}

  const char *what() const noexcept override { return message.c_str(); }

private:
  std::string message;
};

#endif // DITTO_QUICKSTART_TASKS_EXCEPTION_H
