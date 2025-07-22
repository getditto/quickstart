import Testing
import Foundation
import DittoSwift

@testable import Tasks

@MainActor
struct DittoManagerTests {
    
    @Test
    func testDittoPopulateTaskCollection() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        
        // Assert
        try await dittoManager.populateTaskCollection()
        let tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 4, "Should have 4 initial tasks")
        
        // Verify specific tasks exist
        let expectedTitles = [
            "Buy groceries",
            "Clean the kitchen",
            "Schedule dentist appointment",
            "Pay bills"
        ]
        
        // Check each expected title exists exactly once
        for expectedTitle in expectedTitles {
            let matchingTasks = tasks.filter { $0.title == expectedTitle }
            #expect(matchingTasks.count == 1,
                    "Should find exactly one task with title '\(expectedTitle)', found \(matchingTasks.count)")
        }
        
        // Verify IDs are unique
        let uniqueIds = Set(tasks.map { $0._id })
        #expect(uniqueIds.count == tasks.count,
                "All task IDs should be unique")
        
        //clean up
        try await cleanUpCollection(dittoManager)
    }

    @Test
    func testDittoInsertTaskModel() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        var tasks = try await getTasksCollection(dittoManager)
        let newTask = createInitialTask()
        
        // Get initial count
        let initialCount = tasks.count
        
        // Act
        try await dittoManager.insertTaskModel(newTask)

        // get updated tasks
        tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == initialCount + 1, "Should have one more task after insertion")
        
        // Verify the new task was added correctly
        if let insertedTask = tasks.first(where: { $0._id == newTask._id }) {
            #expect(insertedTask.title == newTask.title, "Task title should match")
            #expect(insertedTask.done == newTask.done, "Task done status should match")
            #expect(insertedTask.deleted == newTask.deleted, "Task deleted status should match")
        } else {
            #expect(Bool(false), "New task should be found in tasks array")
        }
        
        //clean up
        try await cleanUpCollection(dittoManager)
    }
    
    @Test
    func testDittoUpdateTaskModel() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        var tasks = try await getTasksCollection(dittoManager)
        let initialTask = createInitialTask()
        
        // Add initial task
        try await dittoManager.insertTaskModel(initialTask)

        // Create updated version of task
        var updatedTask = initialTask
        updatedTask.title = "Updated Title"
        updatedTask.done = true
        
        // Act
        try await dittoManager.updateTaskModel(updatedTask)

        // Assert
        tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 1, "Should still have one task after update")
        if let resultTask = tasks.first(where: { $0._id == updatedTask._id }) {
            #expect(resultTask._id == initialTask._id, "Task ID should remain unchanged")
            #expect(resultTask.title == "Updated Title", "Task title should be updated")
            #expect(resultTask.done == true, "Task done status should be updated")
        } else {
            #expect(Bool(false), "Updated task should be found in tasks array")
        }
        
        //clean up
        try await cleanUpCollection(dittoManager)
    }
    
    @Test
    func testDittoUpdateTaskModel_WhenDeleted() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        var initialTask = createInitialTask()
        
        // Add initial task
        try await dittoManager.insertTaskModel(initialTask)
        var tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 1, "Should have 1 task after insert")
        
        // Act
        initialTask.deleted = true
        try await dittoManager.updateTaskModel(initialTask)

        // Assert
        tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 0, "Should have 0 task after update")
        
        //clean up
        try await cleanUpCollection(dittoManager)
    }
    
    @Test
    func testDittoToggleComplete() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        let initialTask = createInitialTask()
        
        // Add initial task
        try await dittoManager.insertTaskModel(initialTask)
        var tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 1, "Should have 1 task after insert")
        
        // Act - Toggle complete (false -> true)
        try await dittoManager.toggleComplete(task: initialTask)

        // Assert
        tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 1, "Should still have 1 task after update")
        if let resultTask = tasks.first(where: { $0._id == initialTask._id }) {
            #expect(resultTask._id == initialTask._id, "Task ID should remain unchanged")
            #expect(resultTask.title == initialTask.title, "Task title remain unchanged")
            #expect(resultTask.done == true, "Task should be marked as done after first toggle")
            
            // Assert a second toggle
            // Act - Toggle complete again (true -> false)
            try await dittoManager.toggleComplete(task: resultTask)
            tasks = try await getTasksCollection(dittoManager)
            if let finalTask = tasks.first(where: { $0._id == initialTask._id }) {
                #expect(finalTask.done == false, "Task should be marked as not done after second toggle")
                #expect(finalTask._id == initialTask._id, "Task ID should remain unchanged")
                #expect(finalTask.title == initialTask.title, "Task title should remain unchanged")
            } else {
                #expect(Bool(false), "Task should still exist after second toggle")
            }
            
        } else {
            #expect(Bool(false), "Updated task should be found in tasks array")
        }
    }
    
    @Test
    func testDittoDeleteTaskModel() async throws {
        // Arrange
        let dittoManager = try await createDittoManagerForTests()
        let initialTask = createInitialTask()
        
        // Add initial task
        try await dittoManager.insertTaskModel(initialTask)
        var tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 1, "Should have 1 task after insert")
        if let resultTask = tasks.first(where: { $0._id == initialTask._id }) {
            #expect(resultTask._id == initialTask._id, "Task ID should remain unchanged")
            #expect(resultTask.title == initialTask.title, "Task title remain unchanged")
        } else {
            #expect(Bool(false), "Inserted task should be found in tasks array")
        }
        
        // Act
        try await dittoManager.deleteTaskModel(initialTask)
        
        // Assert
        tasks = try await getTasksCollection(dittoManager)
        #expect(tasks.count == 0, "Task array should be 0 after deletion")
        #expect(!tasks.contains(where: { $0._id == initialTask._id }),
                "Deleted task should not be in array")
    }
    
    
    private func createInitialTask() -> Tasks.TaskModel {
        return Tasks.TaskModel(
            _id: UUID().uuidString,
            title: "Test Task",
            done: false,
            deleted: false
        )
    }
    
    private func getTasksCollection(_ dittoManager: Tasks.DittoManager) async throws -> [Tasks.TaskModel] {
        if let dittoInstance = dittoManager.ditto {
            let results = try await dittoInstance.store.execute(query: "SELECT * FROM tasks WHERE NOT deleted")
            let tasks = results.items.compactMap{
                Tasks.TaskModel($0.jsonString())
            }
            return tasks
        }
        return []
    }
    
    private func createDittoManagerForTests() async throws -> Tasks.DittoManager  {
        let dittoManager = Tasks.DittoManager()
        // setup logging
        DittoLogger.enabled = true
        DittoLogger.minimumLogLevel = .debug
        
        // Create unique test directory
        let testDirectoryPath = FileManager.default.temporaryDirectory
            .appendingPathComponent("ditto_test_\(UUID().uuidString)")
        
        // Ensure directory exists
        try FileManager.default.createDirectory(
            at: testDirectoryPath,
            withIntermediateDirectories: true
        )
        do {
            // Initialize Ditto with test-specific directory
            dittoManager.ditto = Ditto(
                identity: .onlinePlayground(
                    appID: Env.DITTO_APP_ID,
                    token: Env.DITTO_PLAYGROUND_TOKEN,
                    enableDittoCloudSync: false,
                    customAuthURL: URL(string: Env.DITTO_AUTH_URL)
                ),
                persistenceDirectory: testDirectoryPath
            )
            try await dittoManager.ditto?.store.execute(
                query: "ALTER SYSTEM SET USER_COLLECTION_SYNC_SCOPES = :syncScopes",
                arguments: ["syncScopes":
                                [ "tasks": "LocalPeerOnly"]])
            
            try dittoManager.ditto?.disableSyncWithV3()
        } catch {
            // Clean up test directory if initialization fails
            try? FileManager.default.removeItem(at: testDirectoryPath)
        }
        return dittoManager
    }
    
    private func cleanUpCollection(_ dittoManager: Tasks.DittoManager) async throws {
        if let dittoInstance = dittoManager.ditto {
            dittoManager.subscription?.cancel()
            try await dittoInstance.store.execute(query: "EVICT FROM tasks")
            if (dittoInstance.isSyncActive) {
                dittoInstance.stopSync()
            }
            let directory = dittoInstance.persistenceDirectory
            try? FileManager.default.removeItem(at: directory)
        }
    }
}
