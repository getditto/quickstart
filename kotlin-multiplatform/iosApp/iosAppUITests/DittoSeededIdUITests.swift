import XCTest

final class DittoSeededIdUITests: XCTestCase {

    func testSeededTaskVisible() {
        guard let taskId = ProcessInfo.processInfo.environment["DITTO_TASK_ID"] else {
            XCTFail("DITTO_TASK_ID environment variable is required")
            return
        }
        
        let app = XCUIApplication()
        app.launchEnvironment["DITTO_TASK_ID"] = taskId
        app.launch()
        
        // Wait for the task with the seeded ID to appear
        let taskText = app.staticTexts[taskId]
        XCTAssertTrue(taskText.waitForExistence(timeout: 10), 
                      "Task with ID '\(taskId)' should be visible in the UI")
    }
}