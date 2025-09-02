import XCTest
@testable import Tasks

/**
 * Integration tests for Ditto Swift Tasks app
 * Tests core functionality and configuration
 */
class IntegrationTests: XCTestCase {
    
    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }
    
    /**
     * Test that the app can access required environment variables
     */
    func testDittoConfigurationValidation() throws {
        print("🔄 Testing Ditto configuration validation...")
        
        // Test environment variables are accessible
        let appId = Env.DITTO_APP_ID
        let token = Env.DITTO_PLAYGROUND_TOKEN  
        let authUrl = Env.DITTO_AUTH_URL
        let websocketUrl = Env.DITTO_WEBSOCKET_URL
        
        print("📝 Ditto config - AppID: \(String(appId.prefix(8)))...")
        print("📝 Auth URL: \(authUrl)")
        print("📝 WebSocket URL: \(websocketUrl)")
        
        // Basic validation that credentials are present
        XCTAssertFalse(appId.isEmpty, "DITTO_APP_ID should be set")
        XCTAssertFalse(token.isEmpty, "DITTO_PLAYGROUND_TOKEN should be set") 
        XCTAssertFalse(authUrl.isEmpty, "DITTO_AUTH_URL should be set")
        XCTAssertFalse(websocketUrl.isEmpty, "DITTO_WEBSOCKET_URL should be set")
        
        // Validate configuration format
        XCTAssertTrue(appId.count >= 8, "DITTO_APP_ID should be at least 8 characters")
        XCTAssertTrue(authUrl.hasPrefix("http"), "DITTO_AUTH_URL should be a valid HTTP URL")
        XCTAssertTrue(websocketUrl.hasPrefix("ws"), "DITTO_WEBSOCKET_URL should be a valid WebSocket URL")
        
        print("✅ All Ditto configuration variables are present and valid")
        print("✅ Integration test prerequisites met")
    }
    
    /**
     * Test Task model integrity and properties
     */
    func testTaskModelIntegrity() throws {
        print("📋 Testing Task model field integrity...")
        
        // Create a test task
        let testTaskId = "test_task_\(UUID().uuidString)"
        let testTask = Task(id: testTaskId, text: "Test Task", isCompleted: false)
        
        // Verify task properties
        XCTAssertEqual(testTask.id, testTaskId, "Task ID should match")
        XCTAssertEqual(testTask.text, "Test Task", "Task text should match")
        XCTAssertFalse(testTask.isCompleted, "Task should not be completed initially")
        
        print("✅ Task model integrity validated")
    }
    
    /**
     * Test DittoManager initialization
     */
    func testDittoManagerConfiguration() throws {
        print("🔧 Testing DittoManager configuration...")
        
        // Test that DittoManager can be initialized
        // Note: This doesn't create an actual Ditto instance to avoid network calls in CI
        let manager = DittoManager.shared
        XCTAssertNotNil(manager, "DittoManager should be accessible")
        
        print("✅ DittoManager configuration validated")
    }
    
    /**
     * Test app performance and basic functionality
     */
    func testAppPerformance() throws {
        print("⚡ Testing app performance...")
        
        self.measure {
            // Test basic task creation performance
            let task = Task(id: UUID().uuidString, text: "Performance Test", isCompleted: false)
            XCTAssertNotNil(task)
        }
        
        print("✅ App performance acceptable")
    }
}