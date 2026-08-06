#include "../src/ditto_manager.h"
#include "../src/env.h"
#include "../src/task.h"
#include "../src/tasks_log.h"
#include "../src/tasks_repository.h"

#include <chrono>
#include <cstdlib>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

using std::cout;
using std::endl;
using std::exception;
using std::string;
using std::vector;
using std::chrono::duration_cast;
using std::chrono::high_resolution_clock;
using std::chrono::milliseconds;
using std::chrono::seconds;
using std::this_thread::sleep_for;

/**
 * Simple test that verifies GitHub-seeded document appears in synced task list
 */
int main() {
  try {
    cout << "C++ GitHub Seeded Document Test" << endl;

    // Get the exact document title that GitHub Actions seeded
    const auto expected_title_env = getenv("DITTO_CLOUD_TASK_TITLE");
    if (!expected_title_env || string(expected_title_env).empty()) {
      cout << "FAIL: Missing DITTO_CLOUD_TASK_TITLE environment variable"
           << endl;
      std::exit(EXIT_FAILURE);
    }

    string expected_title = string(expected_title_env);
    cout << "Looking for seeded document: '" << expected_title << "'" << endl;

    // Initialize the Ditto manager and tasks repository, then start sync
    cout << "Initializing Ditto sync..." << endl;
    auto manager = std::make_shared<DittoManager>(
        DITTO_DATABASE_ID, DITTO_DEVELOPMENT_TOKEN, DITTO_SERVER_URL,
        DITTO_OFFLINE_LICENSE_TOKEN, "/tmp/cpp_integration_test");
    TasksRepository repository(manager);

    manager->start_sync();
    cout << "Sync started, polling for document..." << endl;

    // Wait for sync and search for the exact document
    const auto max_wait_seconds = 30;
    const auto poll_interval_ms = 1000;
    auto found = false;

    auto start_time = high_resolution_clock::now();

    while (duration_cast<seconds>(high_resolution_clock::now() - start_time)
                   .count() < max_wait_seconds &&
           !found) {
      auto elapsed =
          duration_cast<seconds>(high_resolution_clock::now() - start_time)
              .count();

      vector<Task> tasks = repository.get_tasks();
      cout << "Checking " << tasks.size() << " synced tasks at " << elapsed
           << "s..." << endl;

      for (size_t i = 0; i < tasks.size(); i++) {
        const auto &task = tasks[i];
        if (task.title == expected_title) {
          cout << "SUCCESS: Found document '" << expected_title
               << "' at position " << i << endl;
          found = true;
          break;
        }
      }

      if (!found) {
        sleep_for(milliseconds(poll_interval_ms));
      }
    }

    auto final_elapsed =
        duration_cast<seconds>(high_resolution_clock::now() - start_time)
            .count();

    if (found) {
      cout << "PASS: GitHub Actions → Ditto server → C++ SDK sync verified in "
           << final_elapsed << "s" << endl;
      return 0;
    } else {
      cout << "FAIL: Document '" << expected_title << "' not found after "
           << final_elapsed << "s" << endl;
      std::exit(EXIT_FAILURE);
    }

  } catch (const exception &e) {
    cout << "FAIL: Test exception: " << e.what() << endl;
    std::exit(EXIT_FAILURE);
  }
}
