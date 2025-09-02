#include "../src/env.h"
#include "../src/task.h"

#include <iostream>
#include <cassert>
#include <sstream>
#include <string>
#include <chrono>

// Using specific declarations to avoid namespace pollution
using std::cout;
using std::endl;
using std::string;
using std::exception;
using std::chrono::high_resolution_clock;
using std::chrono::microseconds;
using std::chrono::duration_cast;

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
 * Unit tests for C++ TUI Tasks app
 * Tests core functionality and configuration
 */

class UnitTests {
public:
    /**
     * Test that the app can access required environment variables
     */
    void test_configuration_validation() {
        cout << "🔄 Testing configuration validation..." << endl;
        
        // Test environment variables are accessible
        string app_id = DITTO_APP_ID;
        string token = DITTO_PLAYGROUND_TOKEN;
        string auth_url = DITTO_AUTH_URL;
        string websocket_url = DITTO_WEBSOCKET_URL;
        
        cout << "📝 Ditto config - AppID: " << app_id.substr(0, 8) << "..." << endl;
        cout << "📝 Auth URL: " << auth_url << endl;
        cout << "📝 WebSocket URL: " << websocket_url << endl;
        
        // Basic validation that credentials are present
        assert(!app_id.empty());
        assert(!token.empty());
        assert(!auth_url.empty());
        assert(!websocket_url.empty());
        
        // Validate configuration format
        assert(app_id.length() >= 8);
        assert(auth_url.find("http") == 0);
        assert(websocket_url.find("ws") == 0);
        
        cout << "✅ All configuration variables are present and valid" << endl;
        cout << "✅ Unit test prerequisites met" << endl;
    }
    
    /**
     * Test Task model integrity and properties
     */
    void test_task_model_integrity() {
        cout << "📋 Testing Task model field integrity..." << endl;
        
        // Create a test task
        string test_task_id = "test_task_12345";
        Task test_task(test_task_id, "Test Task", false, false);
        
        // Verify task properties
        assert(test_task._id == test_task_id);
        assert(test_task.title == "Test Task");
        assert(!test_task.done);
        assert(!test_task.deleted);
        
        // Test task equality
        Task identical_task(test_task_id, "Test Task", false, false);
        assert(test_task == identical_task);
        
        // Test different task
        Task different_task("different_id", "Different Task", true, false);
        assert(!(test_task == different_task));
        
        cout << "✅ Task model integrity validated" << endl;
    }
    
    /**
     * Test Task JSON serialization/deserialization
     */
    void test_task_json_operations() {
        cout << "🔄 Testing Task JSON operations..." << endl;
        
        // Create a test task
        Task original_task("json_test_123", "JSON Test Task", true, false);
        
        // Convert to JSON
        nlohmann::json j;
        to_json(j, original_task);
        
        // Verify JSON structure
        assert(j["_id"] == "json_test_123");
        assert(j["title"] == "JSON Test Task");
        assert(j["done"] == true);
        assert(j["deleted"] == false);
        
        // Convert back from JSON
        Task deserialized_task;
        from_json(j, deserialized_task);
        
        // Verify deserialized task matches original
        assert(original_task == deserialized_task);
        
        cout << "✅ Task JSON operations validated" << endl;
    }
    
    /**
     * Test performance of basic operations
     */
    void test_basic_performance() {
        cout << "⚡ Testing basic performance..." << endl;
        
        auto start_time = high_resolution_clock::now();
        
        // Test task creation performance
        for (int i = 0; i < 1000; i++) {
            Task task("task_" + std11_compat::to_string(i), "Performance Test " + std11_compat::to_string(i), false, false);
            assert(!task._id.empty());
        }
        
        auto end_time = high_resolution_clock::now();
        auto duration = duration_cast<microseconds>(end_time - start_time);
        
        cout << "✅ Created 1000 tasks in " << duration.count() << " microseconds" << endl;
        assert(duration.count() < 10000); // Should be very fast (< 10ms)
        
        cout << "✅ Basic performance acceptable" << endl;
    }
    
    /**
     * Run all unit tests
     */
    void run_all_tests() {
        cout << "🚀 Starting C++ Unit Tests..." << endl;
        cout << "==============================" << endl;
        
        try {
            test_configuration_validation();
            test_task_model_integrity();
            test_task_json_operations();
            test_basic_performance();
            
            cout << "==============================" << endl;
            cout << "✅ ALL UNIT TESTS PASSED!" << endl;
            cout << "🎯 Verified: Configuration, Task model, JSON ops, and performance" << endl;
            
        } catch (const exception& e) {
            cout << "❌ Unit test failed: " << e.what() << endl;
            throw;
        }
    }
};

int main() {
    try {
        UnitTests tests;
        tests.run_all_tests();
        return 0;
    } catch (const exception& e) {
        cout << "❌ Unit tests failed: " << e.what() << endl;
        return 1;
    }
}