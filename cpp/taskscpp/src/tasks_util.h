#ifndef DITTO_QUICKSTART_TASKS_UTIL_H
#define DITTO_QUICKSTART_TASKS_UTIL_H

#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

/// Quote a string for use in a DQL identifier.
///
/// Adds backticks to the beginning and end, and escapes any backticks within
/// the string.
std::string quote_dql_identifier(const std::string &identifier);

/// Quote a string for use in a DQL statement.
///
/// Adds single-quote characters to the beginning and end, and escapes any
/// single-quotes within the string.
std::string quote_dql_string_literal(const std::string &s);

/// Convert a container of values to a single string with values separated by
/// the given delimiter.
///
/// The container element type must be convertible to a string using the
/// `std::ostream` `<<` operator.
template <class Container>
std::string join_string_values(const Container &c,
                               const std::string &delimiter = ",") {
  if (c.empty()) {
    return "";
  }

  std::ostringstream oss;
  auto it = c.cbegin();
  oss << *it;

  for (++it; it != c.cend(); ++it) {
    oss << delimiter << *it;
  }

  return oss.str();
}

/// Return a numeric representation of a pointer address, for use in logging.
inline std::string ptr_to_string(const void *ptr) {
  return std::to_string(reinterpret_cast<std::uintptr_t>(ptr));
}

/// Generate hex dump of given data on specified output stream
std::ostream &hexdump(std::ostream &os, const void *data, std::size_t size,
                      bool include_ascii = false);

inline std::string hexdump_data(const void *data, std::size_t size,
                                bool include_ascii = false) {
  std::ostringstream oss;
  hexdump(oss, data, size, include_ascii);
  return oss.str();
}

inline std::string hexdump_string(const std::string &s,
                                  bool include_ascii = false) {
  return hexdump_data(s.data(), s.size(), include_ascii);
}

#endif // DITTO_QUICKSTART_TASKS_UTIL_H
