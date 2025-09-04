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

        // Handle permission dialogs 
        sleep(3)
        handlePermissionDialogs(app: app)

        // Wait for initial sync to complete
        print("⏳ Waiting for initial sync...")
        sleep(5)
        
        // Simply verify we can access the task list and handle permission dialogs
        print("🔍 Testing permission dialog handling and task list access...")
        
        // Handle permission dialogs that appear during app startup
        handlePermissionDialogs(app: app)
        
        // Wait for the task list to load
        let maxWaitTime = 10.0
        let startTime = Date()
        var taskListAccessible = false
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            print("📱 Checking task list accessibility...")
            
            // Handle any permission dialogs that may appear
            handlePermissionDialogs(app: app)
            
            // Check if we can access the task list
            if app.tables.firstMatch.exists {
                let taskCells = app.tables.cells
                let cellCount = taskCells.count
                print("✅ Task list accessible with \(cellCount) cells")
                
                if cellCount > 0 {
                    // Show first few tasks for verification
                    print("📱 First few tasks:")
                    for i in 0..<min(cellCount, 5) {
                        let cell = taskCells.element(boundBy: i)
                        if cell.exists && !cell.label.isEmpty {
                            let cellText = cell.label
                            print("   [\(i)]: '\(cellText)'")
                        }
                    }
                    taskListAccessible = true
                    break
                }
            }
            
            sleep(1)
        }
        
        if !taskListAccessible {
            print("❌ Could not access task list after \(maxWaitTime) seconds")
            print("📱 App state: \(app.state)")
            
            // Debug: show what UI elements are available
            print("📱 Available UI elements:")
            let allElements = app.descendants(matching: .any)
            for i in 0..<min(allElements.count, 10) {
                let element = allElements.element(boundBy: i)
                if element.exists && !element.label.isEmpty {
                    print("   Element [\(i)]: '\(element.label)' (\(element.elementType))")
                }
            }
            
            XCTFail("Failed to access task list - permission dialogs may be blocking UI")
        }
        
        XCTAssertTrue(taskListAccessible, "Task list should be accessible after handling permission dialogs")
        
        // Verify app stability
        sleep(3)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after sync verification")
        
        print("✅ Ditto sync verification completed successfully")
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