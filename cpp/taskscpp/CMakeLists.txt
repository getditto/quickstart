cmake_minimum_required(VERSION 3.14)

# Set the project name and version
project(taskscpp VERSION 1.0 LANGUAGES CXX)

# Set the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_CXX_EXTENSIONS OFF)

# We may have a build of the Ditto SDK with Address Sanitizer enabled, so we
# need compatible build flags.
if(CMAKE_CXX_COMPILER_ID STREQUAL "Clang" OR CMAKE_CXX_COMPILER_ID STREQUAL "AppleClang")
  # Enable Address Sanitizer
  add_compile_options(-fsanitize=address -fno-omit-frame-pointer -Wno-deprecated-declarations)
  add_link_options(-fsanitize=address)
elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
  # Enable Address Sanitizer
  add_compile_options(-fsanitize=address -fno-omit-frame-pointer -Wno-deprecated-declarations)
  add_link_options(-fsanitize=address)
endif()

# Add a target to generate the src/env.h file
set(ENV_INPUT ${CMAKE_CURRENT_SOURCE_DIR}/../../.env)
set(ENV_H ${CMAKE_CURRENT_SOURCE_DIR}/src/env.h)
set(ENV_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/scripts/generate_env.awk)
add_custom_command(
  OUTPUT ${ENV_H}
  COMMAND awk -f ${ENV_SCRIPT} ${ENV_INPUT} > ${ENV_H}
  DEPENDS ${ENV_INPUT} ${ENV_SCRIPT}
  COMMENT "Generating env.h"
)
add_custom_target(env_h DEPENDS ${ENV_H})

# Compile all the .cpp files in the src directory to build taskscpp
file(GLOB_RECURSE SOURCES "src/*.cpp")
add_executable(taskscpp ${SOURCES})
add_dependencies(taskscpp env_h)

# Include the Ditto SDK header and link with the Ditto SDK
target_include_directories(taskscpp PRIVATE sdk)
target_link_libraries(taskscpp PRIVATE ${CMAKE_SOURCE_DIR}/sdk/libditto.a)

# Add dependency on cxxopts library
target_include_directories(taskscpp PRIVATE third_party/cxxopts/include)
