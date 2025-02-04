#ifndef QUICKSTARTTASKSCPP_JOIN_STRING_VALUES_H
#define QUICKSTARTTASKSCPP_JOIN_STRING_VALUES_H

#include <sstream>
#include <string>

/// Convert a container of values to a single string with values separated by
/// the given delimiter.
///
/// The container element type must be convertible to a string using the
/// `std::ostream` `<<` operator.
template<class Container>
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

#endif //QUICKSTARTTASKSCPP_JOIN_STRING_VALUES_H
