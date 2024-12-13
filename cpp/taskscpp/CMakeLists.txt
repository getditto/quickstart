cmake_minimum_required(VERSION 3.14)

project(taskscpp
  VERSION 1.0
  LANGUAGES CXX
  DESCRIPTION "Ditto Tasks Manager"
)

# Run cmake with -DDITTO_QUICKSTART_TUI=OFF to disable the TUI
# (This would be useful if you can't build the required dependencies.)
option(DITTO_QUICKSTART_TUI "Enable FTXUI library support" ON)

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
elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
  add_compile_options(-Wno-deprecated-declarations)

  # Enable Address Sanitizer
  add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
  add_link_options(-fsanitize=address)
endif()

if(DITTO_QUICKSTART_TUI)
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
endif()

# Add a target to generate the src/env.h file
set(ENV_INPUT ${CMAKE_CURRENT_SOURCE_DIR}/../../.env)
set(ENV_H ${CMAKE_CURRENT_SOURCE_DIR}/src/env.h)
set(ENV_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/scripts/generate_env.awk)
find_program(AWK NAMES awk nawk gawk)
add_custom_command(
  OUTPUT ${ENV_H}
  COMMAND ${AWK} -f ${ENV_SCRIPT} ${ENV_INPUT} > ${ENV_H}
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
# Note: we are using an absolute path here (rather than unqualified path
# with `target_link_directories`) to make updates to the `libditto.a` artifact
# (e.g., from having recompiled the monorepo's C++ SDK or whatnot) force cmake
# to redo the linkage step.
target_link_libraries(taskscpp PRIVATE ${CMAKE_SOURCE_DIR}/sdk/libditto.a)

if(DITTO_QUICKSTART_TUI)
  target_compile_definitions(taskscpp PRIVATE DITTO_QUICKSTART_TUI)
  target_link_libraries(taskscpp PRIVATE ftxui::screen ftxui::dom ftxui::component)
endif()

# Add dependency on cxxopts library
target_include_directories(taskscpp PRIVATE third_party/cxxopts/include)
