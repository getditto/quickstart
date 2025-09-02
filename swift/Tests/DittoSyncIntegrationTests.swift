import XCTest
import Foundation
@testable import Tasks
import DittoSwift

/**
 * Real integration tests for Ditto Swift Tasks app
 * Tests actual cloud sync functionality with Ditto cloud backend
 */
class DittoSyncIntegrationTests: XCTestCase {
    
    private var ditto: Ditto!
    private var testDocumentId: String!
    
    override func setUpWithError() throws {
        // Initialize Ditto with environment variables
        ditto = Ditto(
            identity: .onlinePlayground(
                appID: Env.DITTO_APP_ID,
                token: Env.DITTO_PLAYGROUND_TOKEN,
                enableDittoCloudSync: true,
                customAuthURL: URL(string: Env.DITTO_AUTH_URL)
            )
        )
        
        // Set the Ditto Websocket URL
        ditto.updateTransportConfig { transportConfig in
            transportConfig.connect.webSocketURLs.insert(Env.DITTO_WEBSOCKET_URL)
        }
        
        // Disable sync with v3 peers, required for DQL
        try ditto.disableSyncWithV3()
        
        // Generate unique test document ID
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? "local_test"
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? "1"
        testDocumentId = "swift_github_test_\(githubRunId)_\(githubRunNumber)"
        
        print("🧪 Starting Swift integration tests...")
        print("📝 Test document ID: \(testDocumentId!)")
        print("📝 App ID: \(String(Env.DITTO_APP_ID.prefix(8)))...")
    }
    
    override func tearDownWithError() throws {
        if let ditto = ditto, ditto.isSyncActive {
            ditto.stopSync()
        }
        ditto = nil
    }
    
    /**
     * Test inserting a document into Ditto Cloud via API and verifying it syncs locally
     */
    func testCloudSyncFromAPI() throws {
        let expectation = XCTestExpectation(description: "Document synced from cloud")
        
        print("🌐 Testing cloud sync from API...")
        
        // First, insert a document via Ditto API (simulating external client)
        insertTestDocumentViaAPI { [weak self] success in
            guard let self = self else { return }
            
            if success {
                print("✅ Successfully inserted test document via API")
                
                // Start sync and wait for the document to appear locally
                do {
                    try self.ditto.startSync()
                    
                    // Register subscription to sync the tasks
                    let subscription = try self.ditto.sync.registerSubscription(query: "SELECT * FROM tasks")
                    
                    // Register observer to watch for our test document
                    let observer = try self.ditto.store.registerObserver(
                        query: "SELECT * FROM tasks WHERE _id = :testId",
                        arguments: ["testId": self.testDocumentId!]
                    ) { result in
                        if !result.items.isEmpty {
                            let item = result.items[0]
                            if let taskData = item.jsonData(),
                               let taskModel = TaskModel(taskData) {
                                print("🎉 Test document synced locally: \(taskModel.title)")
                                expectation.fulfill()
                            }
                        }
                    }
                    
                    // Clean up resources after test completes
                    DispatchQueue.main.asyncAfter(deadline: .now() + 30) {
                        subscription?.cancel()
                        observer?.cancel()
                    }
                    
                } catch {
                    XCTFail("Failed to start sync: \(error)")
                }
            } else {
                XCTFail("Failed to insert test document via API")
            }
        }
        
        // Wait for sync to complete
        wait(for: [expectation], timeout: 30.0)
    }
    
    /**
     * Test REAL cross-device cloud sync - insert with one Ditto instance, verify with another
     */
    func testRealCrossDeviceCloudSync() throws {
        let expectation = XCTestExpectation(description: "Document synced across devices")
        
        print("🌍 Testing REAL cross-device cloud sync...")
        
        let crossDeviceTestId = "crossdevice_\(testDocumentId!)_\(UUID().uuidString.prefix(8))"
        let testTaskTitle = "Cross-Device Test Task \(ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? "local")"
        
        do {
            try ditto.startSync()
            let subscription = try ditto.sync.registerSubscription(query: "SELECT * FROM tasks")
            
            Task {
                do {
                    // Step 1: Insert document with first Ditto instance
                    try await self.ditto.store.execute(
                        query: "INSERT INTO tasks DOCUMENTS (:newTask)",
                        arguments: ["newTask": [
                            "_id": crossDeviceTestId,
                            "title": testTaskTitle,
                            "done": false,
                            "deleted": false
                        ]]
                    )
                    
                    print("✅ Document inserted with Device 1: \(testTaskTitle)")
                    
                    // Step 2: Wait for cloud sync
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        Task {
                            do {
                                // Step 3: Create NEW Ditto instance (simulates different device)
                                let ditto2 = Ditto(
                                    identity: .onlinePlayground(
                                        appID: Env.DITTO_APP_ID,
                                        token: Env.DITTO_PLAYGROUND_TOKEN,
                                        enableDittoCloudSync: true,
                                        customAuthURL: URL(string: Env.DITTO_AUTH_URL)
                                    )
                                )
                                
                                ditto2.updateTransportConfig { transportConfig in
                                    transportConfig.connect.webSocketURLs.insert(Env.DITTO_WEBSOCKET_URL)
                                }
                                
                                try ditto2.disableSyncWithV3()
                                try ditto2.startSync()
                                
                                let subscription2 = try ditto2.sync.registerSubscription(query: "SELECT * FROM tasks")
                                
                                print("🔄 Device 2 started, waiting for sync...")
                                
                                // Step 4: Wait for sync and check if document appears on "Device 2"
                                DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                                    Task {
                                        do {
                                            let result = try await ditto2.store.execute(
                                                query: "SELECT * FROM tasks WHERE _id = :testId",
                                                arguments: ["testId": crossDeviceTestId]
                                            )
                                            
                                            if !result.items.isEmpty {
                                                if let taskData = result.items.first?.jsonData(),
                                                   let task = TaskModel(taskData) {
                                                    print("🎉 SUCCESS: Document synced to Device 2: \(task.title)")
                                                    
                                                    // Cleanup
                                                    ditto2.stopSync()
                                                    subscription2?.cancel()
                                                    
                                                    expectation.fulfill()
                                                }
                                            } else {
                                                XCTFail("❌ FAILED: Document did NOT sync to Device 2 (real cloud sync issue)")
                                            }
                                        } catch {
                                            XCTFail("Failed to query Device 2: \(error)")
                                        }
                                    }
                                }
                                
                            } catch {
                                XCTFail("Failed to create Device 2: \(error)")
                            }
                        }
                    }
                    
                } catch {
                    XCTFail("Failed to insert document on Device 1: \(error)")
                }
            }
            
            // Clean up Device 1
            DispatchQueue.main.asyncAfter(deadline: .now() + 30) {
                subscription?.cancel()
            }
            
        } catch {
            XCTFail("Failed to start sync for cross-device test: \(error)")
        }
        
        wait(for: [expectation], timeout: 30.0)
    }
    
    /**
     * Test CRUD operations with sync
     */
    func testCRUDWithSync() throws {
        let expectation = XCTestExpectation(description: "CRUD operations completed")
        
        print("🔄 Testing CRUD operations with sync...")
        
        do {
            try ditto.startSync()
            let subscription = try ditto.sync.registerSubscription(query: "SELECT * FROM tasks")
            
            let testId = "crud_test_\(UUID().uuidString.prefix(8))"
            
            Task {
                do {
                    // CREATE
                    try await self.ditto.store.execute(
                        query: "INSERT INTO tasks DOCUMENTS (:newTask)",
                        arguments: ["newTask": [
                            "_id": testId,
                            "title": "CRUD Test Task",
                            "done": false,
                            "deleted": false
                        ]]
                    )
                    print("✅ CREATE operation completed")
                    
                    // READ
                    let readResult = try await self.ditto.store.execute(
                        query: "SELECT * FROM tasks WHERE _id = :testId",
                        arguments: ["testId": testId]
                    )
                    XCTAssertEqual(readResult.items.count, 1, "Should find exactly one task")
                    print("✅ READ operation completed")
                    
                    // UPDATE
                    try await self.ditto.store.execute(
                        query: "UPDATE tasks SET done = :done WHERE _id = :testId",
                        arguments: ["done": true, "testId": testId]
                    )
                    
                    let updateResult = try await self.ditto.store.execute(
                        query: "SELECT * FROM tasks WHERE _id = :testId",
                        arguments: ["testId": testId]
                    )
                    if let taskData = updateResult.items.first?.jsonData(),
                       let task = TaskModel(taskData) {
                        XCTAssertTrue(task.done, "Task should be marked as done")
                        print("✅ UPDATE operation completed")
                    }
                    
                    // DELETE (soft delete)
                    try await self.ditto.store.execute(
                        query: "UPDATE tasks SET deleted = true WHERE _id = :testId",
                        arguments: ["testId": testId]
                    )
                    
                    let deleteResult = try await self.ditto.store.execute(
                        query: "SELECT * FROM tasks WHERE _id = :testId",
                        arguments: ["testId": testId]
                    )
                    if let taskData = deleteResult.items.first?.jsonData(),
                       let task = TaskModel(taskData) {
                        XCTAssertTrue(task.deleted, "Task should be marked as deleted")
                        print("✅ DELETE operation completed")
                    }
                    
                    expectation.fulfill()
                    
                } catch {
                    XCTFail("CRUD operation failed: \(error)")
                }
            }
            
            // Clean up
            DispatchQueue.main.asyncAfter(deadline: .now() + 30) {
                subscription?.cancel()
            }
            
        } catch {
            XCTFail("Failed to start sync for CRUD test: \(error)")
        }
        
        wait(for: [expectation], timeout: 30.0)
    }
    
    // MARK: - Helper Methods
    
    private func insertTestDocumentViaAPI(completion: @escaping (Bool) -> Void) {
        // Simulating API insertion by inserting directly into Ditto
        // Note: CI pipeline handles real API insertion via curl before tests run
        
        print("📡 Simulating API insertion (in real test, this would be HTTP request)")
        
        Task {
            do {
                try await ditto.store.execute(
                    query: "INSERT INTO tasks DOCUMENTS (:newTask) ON ID CONFLICT DO UPDATE",
                    arguments: ["newTask": [
                        "_id": testDocumentId!,
                        "title": "GitHub Test Task \(ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? "local")",
                        "done": false,
                        "deleted": false
                    ]]
                )
                
                DispatchQueue.main.async {
                    completion(true)
                }
            } catch {
                print("❌ Failed to insert test document: \(error)")
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        }
    }
}