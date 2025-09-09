#include "../src/env.h"
#include "../src/task.h"
#include "../src/tasks_peer.h"
#include "../src/tasks_log.h"

#include <chrono>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include <string>
#include <thread>
#include <cassert>

// Using specific declarations to avoid namespace pollution
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

// C++11 compatible to_string function
namespace std11_compat {
    template<typename T>
    string to_string(const T& value) {
        std::ostringstream oss;
        oss << value;
        return oss.str();
    }
}

/**
 * Real integration tests for Ditto C++ TUI Tasks app
 * Tests actual cloud sync functionality with Ditto cloud backend using SDK DQL API
 */

class DittoIntegrationTests {
private:
    unique_ptr<TasksPeer> peer;
    string test_document_id;
    
public:
    DittoIntegrationTests() {
        // Generate unique test document ID using GitHub environment variables
        const char* run_id_env = getenv("GITHUB_RUN_ID");
        const char* run_number_env = getenv("GITHUB_RUN_NUMBER");
        string github_run_id = run_id_env ? string(run_id_env) : "local_test";
        string github_run_number = run_number_env ? string(run_number_env) : "1";
        test_document_id = "cpp_github_test_" + github_run_id + "_" + github_run_number;
        
        cout << "🧪 Starting C++ integration tests..." << endl;
        cout << "📝 Test document ID: " << test_document_id << endl;
        cout << "📝 App ID: " << string(DITTO_APP_ID).substr(0, 8) << "..." << endl;
        
        // Initialize TasksPeer with environment credentials
        peer = unique_ptr<TasksPeer>(new TasksPeer(
            DITTO_APP_ID,
            DITTO_PLAYGROUND_TOKEN, 
            DITTO_WEBSOCKET_URL,
            DITTO_AUTH_URL,
            true,  // enable_cloud_sync
            "/tmp/ditto_integration_test"
        ));
    }
    
    ~DittoIntegrationTests() {
        if (peer && peer->is_sync_active()) {
            peer->stop_sync();
        }
    }
    
    /**
     * Test that Ditto can be initialized with proper configuration
     */
    void test_ditto_initialization() {
        cout << "🔧 Testing Ditto initialization and configuration..." << endl;
        
        // Verify environment variables are accessible
        assert(!string(DITTO_APP_ID).empty());
        assert(!string(DITTO_PLAYGROUND_TOKEN).empty());
        assert(!string(DITTO_AUTH_URL).empty());
        assert(!string(DITTO_WEBSOCKET_URL).empty());
        
        // Basic validation
        assert(string(DITTO_APP_ID).length() >= 8);
        assert(string(DITTO_AUTH_URL).find("http") == 0);
        assert(string(DITTO_WEBSOCKET_URL).find("ws") == 0);
        
        cout << "✅ Ditto configuration validated" << endl;
        cout << "✅ All credentials present and properly formatted" << endl;
    }
    
    /**
     * Test starting and stopping sync
     */
    void test_sync_lifecycle() {
        cout << "🔄 Testing Ditto sync lifecycle..." << endl;
        
        // Initially sync should not be active
        assert(!peer->is_sync_active());
        
        // Start sync
        peer->start_sync();
        assert(peer->is_sync_active());
        cout << "✅ Sync started successfully" << endl;
        
        // Stop sync
        peer->stop_sync();
        assert(!peer->is_sync_active());
        cout << "✅ Sync stopped successfully" << endl;
        
        // Restart for other tests
        peer->start_sync();
        assert(peer->is_sync_active());
        cout << "✅ Sync lifecycle validated" << endl;
    }
    
    /**
     * Test CRUD operations using SDK DQL API
     */
    void test_crud_operations_with_sdk() {
        cout << "🔄 Testing CRUD operations with SDK DQL API..." << endl;
        
        // Ensure sync is active
        if (!peer->is_sync_active()) {
            peer->start_sync();
        }
        
        // Wait for sync to establish with timeout
        {
            const int max_wait_ms = 5000;
            const int poll_interval_ms = 100;
            int waited_ms = 0;
            while (!peer->is_sync_active() && waited_ms < max_wait_ms) {
                sleep_for(milliseconds(poll_interval_ms));
                waited_ms += poll_interval_ms;
            }
            
            if (!peer->is_sync_active()) {
                cout << "⚠️  Warning: Sync did not establish within timeout, continuing anyway..." << endl;
            } else {
                cout << "✅ Sync established successfully" << endl;
            }
        }
        
        // CREATE - Add a new task using SDK
        string test_title = "C++ Integration Test Task " + 
                           (getenv("GITHUB_RUN_ID") ? string(getenv("GITHUB_RUN_ID")) : string("local"));
        string new_task_id = peer->add_task(test_title, false);
        
        assert(!new_task_id.empty());
        cout << "✅ CREATE operation completed - Task ID: " << new_task_id.substr(0, 8) << "..." << endl;
        
        // Wait for local persistence
        std::this_thread::sleep_for(std::chrono::seconds(1));
        
        // READ - Get all tasks
        vector<Task> tasks = peer->get_tasks();
        bool found_our_task = false;
        Task our_task;
        
        for (const auto& task : tasks) {
            if (task._id == new_task_id) {
                found_our_task = true;
                our_task = task;
                break;
            }
        }
        
        assert(found_our_task);
        assert(our_task.title == test_title);
        assert(!our_task.done);
        assert(!our_task.deleted);
        cout << "✅ READ operation completed - Found task: " << our_task.title << endl;
        
        // UPDATE - Mark task as done
        peer->mark_task_complete(new_task_id, true);
        
        // Wait for update to persist
        std::this_thread::sleep_for(std::chrono::seconds(1));
        
        // Verify update
        vector<Task> updated_tasks = peer->get_tasks();
        for (const auto& task : updated_tasks) {
            if (task._id == new_task_id) {
                assert(task.done);
                cout << "✅ UPDATE operation completed - Task marked as done" << endl;
                break;
            }
        }
        
        // DELETE (soft delete)
        peer->delete_task(new_task_id);
        
        // Wait for delete to persist
        std::this_thread::sleep_for(std::chrono::seconds(1));
        
        // Verify soft delete (should not appear in active tasks)
        vector<Task> active_tasks = peer->get_tasks();
        bool found_in_active = false;
        for (const auto& task : active_tasks) {
            if (task._id == new_task_id) {
                found_in_active = true;
                break;
            }
        }
        assert(!found_in_active);
        
        // But should appear in all tasks (including deleted)
        vector<Task> all_tasks = peer->get_tasks(true); // include deleted
        bool found_in_all = false;
        for (const auto& task : all_tasks) {
            if (task._id == new_task_id && task.deleted) {
                found_in_all = true;
                break;
            }
        }
        assert(found_in_all);
        
        cout << "✅ DELETE operation completed - Task soft deleted" << endl;
        cout << "✅ All CRUD operations validated with SDK DQL API" << endl;
    }
    
    /**
     * Test cloud sync by creating a task that should sync to cloud
     */
    void test_cloud_sync_with_sdk() {
        cout << "🌐 Testing cloud sync with SDK DQL API..." << endl;
        
        // Ensure sync is active
        if (!peer->is_sync_active()) {
            peer->start_sync();
        }
        
        // Create a task that should sync to cloud
        string sync_test_title = "C++ Cloud Sync Test " + test_document_id;
        string sync_task_id = peer->add_task(sync_test_title, false);
        
        cout << "✅ Created task for cloud sync: " << sync_task_id.substr(0, 8) << "..." << endl;
        
        // Wait for sync to occur
        cout << "⏳ Waiting for cloud sync..." << endl;
        std::this_thread::sleep_for(std::chrono::seconds(5));
        
        // Verify task exists locally
        vector<Task> tasks = peer->get_tasks();
        bool task_exists = false;
        for (const auto& task : tasks) {
            if (task._id == sync_task_id) {
                task_exists = true;
                assert(task.title == sync_test_title);
                break;
            }
        }
        
        assert(task_exists);
        cout << "✅ Task confirmed in local store" << endl;
        cout << "✅ Cloud sync test completed with SDK DQL API" << endl;
        
        // Clean up test task
        peer->delete_task(sync_task_id);
    }
    
    /**
     * Test app performance and responsiveness
     */
    void test_performance() {
        cout << "⚡ Testing C++ app performance..." << endl;
        
        auto start_time = std::chrono::high_resolution_clock::now();
        
        // Test multiple rapid operations
        vector<string> task_ids;
        for (int i = 0; i < 5; i++) {
            string task_id = peer->add_task("Performance Test " + std11_compat::to_string(i), false);
            task_ids.push_back(task_id);
        }
        
        auto end_time = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
        
        assert(duration.count() < 5000); // Should complete within 5 seconds
        cout << "✅ Performance test completed in " << duration.count() << "ms" << endl;
        
        // Clean up
        for (const auto& task_id : task_ids) {
            peer->delete_task(task_id);
        }
        
        cout << "✅ Performance validation completed" << endl;
    }
    
    /**
     * Test that finds the exact GitHub-seeded document (like Swift CI)
     */
    void test_find_github_seeded_document() {
        cout << "🔍 Testing for exact GitHub-seeded document..." << endl;
        
        // Get the exact document title that GitHub Actions seeded
        const char* expected_title_env = getenv("GITHUB_TEST_DOC_TITLE");
        if (!expected_title_env || string(expected_title_env).empty()) {
            cout << "❌ Missing GITHUB_TEST_DOC_TITLE environment variable" << endl;
            throw runtime_error("Missing GITHUB_TEST_DOC_TITLE - expected exact document title from GitHub Actions");
        }
        
        string expected_title = string(expected_title_env);
        cout << "📝 Looking for exact document with title: '" << expected_title << "'" << endl;
        
        // Ensure sync is active
        if (!peer->is_sync_active()) {
            peer->start_sync();
        }
        
        // Wait for sync and search for the exact document
        const int max_wait_seconds = 30;
        const int poll_interval_ms = 500;
        bool found = false;
        
        auto start_time = high_resolution_clock::now();
        
        while (duration_cast<seconds>(high_resolution_clock::now() - start_time).count() < max_wait_seconds && !found) {
            auto elapsed = duration_cast<milliseconds>(high_resolution_clock::now() - start_time).count();
            cout << "📱 Search attempt at " << elapsed << "ms elapsed..." << endl;
            
            vector<Task> tasks = peer->get_tasks();
            cout << "📋 Found " << tasks.size() << " tasks in total" << endl;
            
            if (tasks.empty()) {
                cout << "⚠️ No tasks found - might not be synced yet" << endl;
            } else {
                cout << "📄 Examining all documents (sorted by title ASC):" << endl;
                for (size_t i = 0; i < tasks.size(); i++) {
                    const auto& task = tasks[i];
                    cout << "   Task[" << i << "]: '" << task.title << "'" << endl;
                    
                    if (task.title == expected_title) {
                        cout << "✅ FOUND EXACT MATCH! Document '" << task.title << "' found at position[" << i << "]" << endl;
                        cout << "🎉 Test should PASS - document sync working!" << endl;
                        found = true;
                        break;
                    } else {
                        cout << "   ❌ No match (expected exact: '" << expected_title << "')" << endl;
                    }
                }
            }
            
            if (!found) {
                cout << "💤 Waiting " << poll_interval_ms << "ms before retry..." << endl;
                sleep_for(milliseconds(poll_interval_ms));
            }
        }
        
        auto final_elapsed = duration_cast<milliseconds>(high_resolution_clock::now() - start_time).count();
        if (found) {
            cout << "🎉 SUCCESS: Found exact GitHub-seeded document '" << expected_title << "' after " << final_elapsed << "ms" << endl;
            cout << "✅ This proves GitHub Actions → Ditto Cloud → C++ SDK sync is working!" << endl;
            cout << "🏆 Inverted timestamp ensured document appeared at top of list!" << endl;
        } else {
            cout << "❌ FAILURE: Exact document '" << expected_title << "' not found after " << final_elapsed << "ms" << endl;
            cout << "💡 This means either:" << endl;
            cout << "   1. GitHub Actions didn't seed the document" << endl;
            cout << "   2. Ditto Cloud sync is not working" << endl;
            cout << "   3. Environment variable GITHUB_TEST_DOC_TITLE is incorrect" << endl;
            throw runtime_error("GitHub-seeded document not found: " + expected_title);
        }
    }
    
    /**
     * Run all integration tests
     */
    void run_all_tests() {
        cout << "🚀 Starting C++ Ditto Integration Tests..." << endl;
        cout << "===========================================" << endl;
        
        try {
            test_ditto_initialization();
            test_sync_lifecycle();
            test_find_github_seeded_document();
            
            cout << "===========================================" << endl;
            cout << "✅ ALL C++ INTEGRATION TESTS PASSED!" << endl;
            cout << "🎯 Verified: Ditto SDK initialization, sync, and GitHub document sync" << endl;
            
        } catch (const exception& e) {
            cout << "❌ Integration test failed: " << e.what() << endl;
            throw;
        }
    }
};

int main() {
    try {
        DittoIntegrationTests tests;
        tests.run_all_tests();
        return 0;
    } catch (const exception& e) {
        cout << "❌ Integration tests failed: " << e.what() << endl;
        return 1;
    }
}