#ifndef DITTO_TASKSLIB_TRANSFORM_CONTAINER_H
#define DITTO_TASKSLIB_TRANSFORM_CONTAINER_H

#include <algorithm>
#include <functional>
#include <iterator>
#include <vector>

/// Apply a function to all elements of a container, returning a container of
/// results.
///
/// The input container type must support cbegin() and cend(), and the output
/// container type must support push_back().
template <class OutputContainer, class InputContainer, class UnaryOp>
OutputContainer transform_container(const InputContainer &input, UnaryOp f) {
  OutputContainer output;
  std::transform(input.cbegin(), input.cend(), std::back_inserter(output), f);
  return output;
}

#endif
