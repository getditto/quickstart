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

        // Look for "Clean the kitchen" in the actual list structure
        print("🔍 Looking for 'Clean the kitchen' in the list structure...")
        
        let targetTask = "Clean the kitchen"
        var foundTask = false
        
        // First try to find it in static texts (since we know it's StaticText type)
        print("📱 Checking static texts...")
        let staticTexts = app.staticTexts
        print("  Found \(staticTexts.count) static texts")
        
        for i in 0..<staticTexts.count {
            let text = staticTexts.element(boundBy: i)
            if text.exists && text.label == targetTask {
                print("✅ Found target task in static texts!")
                print("  Index in static texts: \(i)")
                print("  Full label: '\(text.label)'")
                foundTask = true
                break
            }
        }
        
        if !foundTask {
            // Show what static texts we did find (likely the task list)
            print("\n📋 Available static texts (tasks):")
            for i in 0..<min(staticTexts.count, 25) {
                let text = staticTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty && text.label.count > 3 {
                    let label = text.label
                    // Skip UI labels
                    if !label.contains("Ditto") && !label.contains("App ID") && !label.contains("Token") && 
                       !label.contains("Sync") {
                        print("  [\(i)]: '\(label)'")
                    }
                }
            }
        }
        
        XCTAssertTrue(foundTask, "Should find 'Clean the kitchen' in static texts")
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