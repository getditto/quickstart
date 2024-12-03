cmake_minimum_required(VERSION 3.14)

project(taskscpp VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_CXX_EXTENSIONS OFF)

# We may have a build of the Ditto SDK with Address Sanitizer enabled, so we
# need compatible build flags.
if(CMAKE_CXX_COMPILER_ID STREQUAL "Clang" OR CMAKE_CXX_COMPILER_ID STREQUAL "AppleClang")
  add_compile_options(-Wno-deprecated-declarations)

  # Enable Address Sanitizer
  add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
  add_link_options(-fsanitize=address)

  if (CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    # Avoid "ld: warning: __eh_frame section too large (max 16MB) to encode dwarf
    # unwind offsets in compact unwind table, performance of exception handling
    # might be affected"
    add_link_options(-Wl,-no_compact_unwind)
  endif()
elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
  add_compile_options(-Wno-deprecated-declarations)

  # Enable Address Sanitizer
  add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
  add_link_options(-fsanitize=address)
endif()

# Fetch the FTXUI library
include(FetchContent)
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/ftxui
  GIT_TAG v5.0.0
)
FetchContent_GetProperties(ftxui)
if(NOT ftxui_POPULATED)
  FetchContent_Populate(ftxui)
  add_subdirectory(${ftxui_SOURCE_DIR} ${ftxui_BINARY_DIR} EXCLUDE_FROM_ALL)
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
target_link_libraries(taskscpp PRIVATE ftxui::screen ftxui::dom ftxui::component)

# Add dependency on cxxopts library
target_include_directories(taskscpp PRIVATE third_party/cxxopts/include)
