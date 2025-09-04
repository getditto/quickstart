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

        // Now look specifically for "Clean the kitchen" task
        print("🔍 Looking for 'Clean the kitchen' task...")
        
        let targetTask = "Clean the kitchen"
        var foundTask = false
        
        // Search through all UI elements for the kitchen task
        let allElements = app.descendants(matching: .any)
        print("📱 Searching through \(allElements.count) elements...")
        
        for i in 0..<allElements.count {
            let element = allElements.element(boundBy: i)
            if element.exists && !element.label.isEmpty {
                let label = element.label
                if label.contains(targetTask) {
                    print("✅ Found target task!")
                    print("  Element type: \(element.elementType)")
                    print("  Full label: '\(label)'")
                    print("  Index: \(i)")
                    foundTask = true
                    break
                }
            }
        }
        
        if !foundTask {
            print("❌ Target task '\(targetTask)' not found")
            
            // Show what tasks we did find
            print("\n📋 Available tasks (first 15):")
            var taskCount = 0
            for i in 0..<allElements.count {
                let element = allElements.element(boundBy: i)
                if element.exists && !element.label.isEmpty && element.label.count > 3 {
                    let label = element.label
                    // Skip obvious UI elements
                    if !label.contains("Ditto") && !label.contains("App ID") && !label.contains("Token") {
                        print("  [\(taskCount)]: '\(label)' (\(element.elementType))")
                        taskCount += 1
                        if taskCount >= 15 { break }
                    }
                }
            }
        }
        
        XCTAssertTrue(foundTask, "Should find 'Clean the kitchen' task")
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