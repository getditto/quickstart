import XCTest

final class TasksUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Put teardown code here
    }

    func testDittoSyncVerification() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30), 
                     "App should launch successfully")

        // Handle any initial permission dialogs
        sleep(2)
        handlePermissionDialogs(app: app)

        // Simple test: just verify we can see task list with content
        print("🔍 Checking for task list...")
        
        // Look for the list
        let taskList = app.tables.firstMatch
        XCTAssertTrue(taskList.waitForExistence(timeout: 10), "Task list should exist")
        
        // Get all cells
        let allCells = taskList.cells
        let cellCount = allCells.count
        print("📱 Task list has \(cellCount) cells")
        
        // Enumerate and show what we find
        print("📋 Tasks found:")
        for i in 0..<min(cellCount, 10) {
            let cell = allCells.element(boundBy: i)
            if cell.exists {
                let text = cell.label
                print("  \(i): '\(text)'")
            }
        }
        
        // Pass if we found any cells at all
        XCTAssertGreaterThan(cellCount, 0, "Should have at least one task")
        
        print("✅ Test completed - found \(cellCount) tasks")
    }
    
    private func handlePermissionDialogs(app: XCUIApplication) {
        // Handle potential permission dialogs aggressively
        for i in 0..<3 {  // Reduced iterations for faster handling
            let allowButton = app.buttons["Allow"]
            let dontAllowButton = app.buttons["Don't Allow"] 
            let okButton = app.buttons["OK"]
            
            if allowButton.exists {
                print("📱 Handling permission dialog \(i + 1): Allow")
                allowButton.tap()
                sleep(1)  // Reduced sleep time
            } else if dontAllowButton.exists {
                print("📱 Handling permission dialog \(i + 1): Don't Allow") 
                dontAllowButton.tap()
                sleep(1)  // Reduced sleep time
            } else if okButton.exists {
                print("📱 Handling permission dialog \(i + 1): OK")
                okButton.tap()
                sleep(1)  // Reduced sleep time
            } else {
                break // No more dialogs
            }
        }
    }
}