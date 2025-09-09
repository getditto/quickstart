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
        cout << "🔍 C++ GitHub Seeded Document Test" << endl;
        cout << "===================================" << endl;
        
        // Get the exact document title that GitHub Actions seeded
        const char* expected_title_env = getenv("GITHUB_TEST_DOC_TITLE");
        if (!expected_title_env || string(expected_title_env).empty()) {
            cout << "❌ Missing GITHUB_TEST_DOC_TITLE environment variable" << endl;
            return 1;
        }
        
        string expected_title = string(expected_title_env) + "_SIMULATE_FAILURE";
        cout << "📝 Looking for GitHub-seeded document: '" << expected_title << "'" << endl;
        
        // Initialize TasksPeer and start sync
        cout << "🔄 Initializing Ditto and starting sync..." << endl;
        auto peer = unique_ptr<TasksPeer>(new TasksPeer(
            DITTO_APP_ID,
            DITTO_PLAYGROUND_TOKEN, 
            DITTO_WEBSOCKET_URL,
            DITTO_AUTH_URL,
            true,  // enable_cloud_sync
            "/tmp/cpp_integration_test"
        ));
        
        peer->start_sync();
        cout << "✅ Ditto sync started" << endl;
        
        // Wait for sync and search for the exact document
        const int max_wait_seconds = 30;
        const int poll_interval_ms = 1000;
        bool found = false;
        
        auto start_time = high_resolution_clock::now();
        
        while (duration_cast<seconds>(high_resolution_clock::now() - start_time).count() < max_wait_seconds && !found) {
            auto elapsed = duration_cast<seconds>(high_resolution_clock::now() - start_time).count();
            cout << "📱 Checking synced tasks at " << elapsed << "s..." << endl;
            
            vector<Task> tasks = peer->get_tasks();
            cout << "📋 Found " << tasks.size() << " tasks (sorted by title ASC)" << endl;
            
            for (size_t i = 0; i < tasks.size(); i++) {
                const auto& task = tasks[i];
                cout << "   [" << i << "] '" << task.title << "'" << endl;
                
                if (task.title == expected_title) {
                    cout << "✅ FOUND document at position " << i << "!" << endl;
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                cout << "⏳ Document not found yet, waiting..." << endl;
                sleep_for(milliseconds(poll_interval_ms));
            }
        }
        
        auto final_elapsed = duration_cast<seconds>(high_resolution_clock::now() - start_time).count();
        
        if (found) {
            cout << "🎉 SUCCESS: Found GitHub-seeded document after " << final_elapsed << "s" << endl;
            cout << "✅ GitHub Actions → Ditto Cloud → C++ SDK sync verified!" << endl;
            return 0;
        } else {
            cout << "❌ FAILURE: Document '" << expected_title << "' not found after " << final_elapsed << "s" << endl;
            return 1;
        }
        
    } catch (const exception& e) {
        cout << "❌ Test failed with exception: " << e.what() << endl;
        return 1;
    }
}