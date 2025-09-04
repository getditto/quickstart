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

        // Look for "Clean the kitchen" in the task list specifically
        print("🔍 Looking for 'Clean the kitchen' in the task list...")
        
        let targetTask = "Clean the kitchen"
        var foundTask = false
        
        // Find the task list (SwiftUI List becomes a table in XCUITest)
        if app.tables.firstMatch.exists {
            let taskList = app.tables.firstMatch
            print("📱 Found task list table")
            
            // Check static texts within the table (task titles)
            let taskTexts = taskList.staticTexts
            print("  Task list has \(taskTexts.count) text elements")
            
            for i in 0..<taskTexts.count {
                let text = taskTexts.element(boundBy: i)
                if text.exists && text.label == targetTask {
                    print("✅ Found target task in task list!")
                    print("  Index in task list: \(i)")
                    print("  Full label: '\(text.label)'")
                    foundTask = true
                    break
                }
            }
            
            if !foundTask {
                // Show what tasks are in the list
                print("\n📋 Tasks in the task list:")
                for i in 0..<min(taskTexts.count, 15) {
                    let text = taskTexts.element(boundBy: i)
                    if text.exists && !text.label.isEmpty {
                        print("  [\(i)]: '\(text.label)'")
                    }
                }
            }
        } else {
            print("❌ No task list table found")
        }
        
        XCTAssertTrue(foundTask, "Should find 'Clean the kitchen' in the task list")
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