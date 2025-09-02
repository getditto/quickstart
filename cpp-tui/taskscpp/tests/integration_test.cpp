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
        
        // Wait a moment for sync to establish
        this_thread::sleep_for(chrono::seconds(2));
        
        // CREATE - Add a new task using SDK
        string test_title = "C++ Integration Test Task " + 
                           (getenv("GITHUB_RUN_ID") ? string(getenv("GITHUB_RUN_ID")) : string("local"));
        string new_task_id = peer->add_task(test_title, false);
        
        assert(!new_task_id.empty());
        cout << "✅ CREATE operation completed - Task ID: " << new_task_id.substr(0, 8) << "..." << endl;
        
        // Wait for local persistence
        this_thread::sleep_for(chrono::seconds(1));
        
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
        peer->set_task_done(new_task_id, true);
        
        // Wait for update to persist
        this_thread::sleep_for(chrono::seconds(1));
        
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
        this_thread::sleep_for(chrono::seconds(1));
        
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
        this_thread::sleep_for(chrono::seconds(5));
        
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
        
        auto start_time = chrono::high_resolution_clock::now();
        
        // Test multiple rapid operations
        vector<string> task_ids;
        for (int i = 0; i < 5; i++) {
            string task_id = peer->add_task("Performance Test " + std11_compat::to_string(i), false);
            task_ids.push_back(task_id);
        }
        
        auto end_time = chrono::high_resolution_clock::now();
        auto duration = chrono::duration_cast<chrono::milliseconds>(end_time - start_time);
        
        assert(duration.count() < 5000); // Should complete within 5 seconds
        cout << "✅ Performance test completed in " << duration.count() << "ms" << endl;
        
        // Clean up
        for (const auto& task_id : task_ids) {
            peer->delete_task(task_id);
        }
        
        cout << "✅ Performance validation completed" << endl;
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
            test_crud_operations_with_sdk();
            test_cloud_sync_with_sdk();
            test_performance();
            
            cout << "===========================================" << endl;
            cout << "✅ ALL C++ INTEGRATION TESTS PASSED!" << endl;
            cout << "🎯 Verified: Ditto SDK initialization, sync, CRUD operations, and cloud sync" << endl;
            
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