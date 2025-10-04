#include "../src/env.h"
#include "../src/task.h"
#include "../src/tasks_peer.h"
#include "../src/tasks_log.h"

#include <chrono>
#include <cstdlib>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

using std::cout;
using std::endl;
using std::string;
using std::vector;
using std::unique_ptr;
using std::exception;
using std::chrono::seconds;
using std::chrono::high_resolution_clock;
using std::chrono::milliseconds;
using std::chrono::duration_cast;
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
            cout << "FAIL: Missing DITTO_CLOUD_TASK_TITLE environment variable" << endl;
            std::exit(EXIT_FAILURE);
        }

        string expected_title = string(expected_title_env);
        cout << "Looking for seeded document: '" << expected_title << "'" << endl;

        // Initialize TasksPeer and start sync
        cout << "Initializing Ditto sync..." << endl;
        auto peer = unique_ptr<TasksPeer>(new TasksPeer(
            DITTO_APP_ID,
            DITTO_PLAYGROUND_TOKEN,
            DITTO_WEBSOCKET_URL,
            DITTO_AUTH_URL,
            true,  // enable_cloud_sync
            "/tmp/cpp_integration_test"
        ));

        peer->start_sync();
        cout << "Sync started, polling for document..." << endl;

        // Wait for sync and search for the exact document
        const auto max_wait_seconds = 30;
        const auto poll_interval_ms = 1000;
        auto found = false;

        auto start_time = high_resolution_clock::now();

        while (duration_cast<seconds>(high_resolution_clock::now() - start_time).count() < max_wait_seconds && !found) {
            auto elapsed = duration_cast<seconds>(high_resolution_clock::now() - start_time).count();

            vector<Task> tasks = peer->get_tasks();
            cout << "Checking " << tasks.size() << " synced tasks at " << elapsed << "s..." << endl;

            for (size_t i = 0; i < tasks.size(); i++) {
                const auto& task = tasks[i];
                if (task.title == expected_title) {
                    cout << "SUCCESS: Found document '" << expected_title << "' at position " << i << endl;
                    found = true;
                    break;
                }
            }

            if (!found) {
                sleep_for(milliseconds(poll_interval_ms));
            }
        }

        auto final_elapsed = duration_cast<seconds>(high_resolution_clock::now() - start_time).count();

        if (found) {
            cout << "PASS: GitHub Actions → Ditto Cloud → C++ SDK sync verified in " << final_elapsed << "s" << endl;
            return 0;
        } else {
            cout << "FAIL: Document '" << expected_title << "' not found after " << final_elapsed << "s" << endl;
            std::exit(EXIT_FAILURE);
        }

    } catch (const exception& e) {
        cout << "FAIL: Test exception: " << e.what() << endl;
        std::exit(EXIT_FAILURE);
    }
}