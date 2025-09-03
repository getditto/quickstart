import XCTest

final class TasksUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Put teardown code here
    }

    func testAppLaunchAndBasicFunctionality() throws {
        let app = XCUIApplication()
        app.launch()

        // Test 1: App launches successfully  
        let appLaunchTimeout: TimeInterval = 30
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: appLaunchTimeout), 
                     "App should launch within \(appLaunchTimeout) seconds")

        // Test 2: Verify app UI is loaded
        let mainElements = app.descendants(matching: .any)
        XCTAssertGreaterThan(mainElements.count, 5, "App should have loaded UI elements")

        // Test 3: Test basic functionality - look for task input
        let textFields = app.textFields
        let buttons = app.buttons
        
        // Try to interact with the app
        if textFields.count > 0 {
            let firstTextField = textFields.element(boundBy: 0)
            if firstTextField.exists && firstTextField.isHittable {
                firstTextField.tap()
                firstTextField.typeText("BrowserStack XCUITest Task")
                
                // Look for add button
                if buttons.count > 0 {
                    let firstButton = buttons.element(boundBy: 0)
                    if firstButton.exists && firstButton.isHittable {
                        firstButton.tap()
                    }
                }
            }
        }

        // Test 4: Verify app stability - run for 20 seconds
        sleep(20)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable and running")
    }

    func testDittoInitializationStability() throws {
        let app = XCUIApplication()
        app.launch()

        // Wait for app to launch
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30), 
                     "App should launch successfully")

        // Give Ditto time to initialize
        sleep(15)
        
        // Verify app is still running (Ditto didn't crash it)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain running after Ditto initialization")
        
        // Verify UI is still responsive
        let elements = app.descendants(matching: .any)
        XCTAssertGreaterThan(elements.count, 3, "UI should still be responsive")
    }

    func testBasicAppFunctionality() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30), 
                     "App should launch successfully")

        // Handle permission dialogs 
        sleep(2)
        handlePermissionDialogs(app: app)

        // Basic functionality test - verify app UI loads and is interactive
        print("🔍 Testing basic app functionality...")
        
        // Check that main UI elements exist
        let navigationTitle = app.staticTexts["Ditto Tasks"]
        XCTAssertTrue(navigationTitle.waitForExistence(timeout: 10), "App title should be visible")
        
        // Check for new task button
        let newTaskButton = app.buttons.containing(NSPredicate(format: "label CONTAINS[cd] 'New Task'")).firstMatch
        XCTAssertTrue(newTaskButton.waitForExistence(timeout: 10), "New Task button should exist")
        
        // Test basic interaction - tap new task button
        newTaskButton.tap()
        sleep(2)
        
        // Verify edit screen opens
        let textFields = app.textFields
        XCTAssertGreaterThan(textFields.count, 0, "Edit screen should have text field")
        
        // Cancel back to main screen
        let cancelButton = app.buttons["Cancel"]
        if cancelButton.exists {
            cancelButton.tap()
        }
        
        // Verify app stability
        sleep(3)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after basic interactions")
        
        print("✅ Basic app functionality test completed successfully")
    }
    
    private func handlePermissionDialogs(app: XCUIApplication) {
        // Handle potential permission dialogs
        for i in 0..<5 {
            let allowButton = app.buttons["Allow"]
            let dontAllowButton = app.buttons["Don't Allow"] 
            let okButton = app.buttons["OK"]
            
            if allowButton.exists {
                print("📱 Handling permission: Allow")
                allowButton.tap()
                sleep(2)
            } else if dontAllowButton.exists {
                print("📱 Handling permission: Don't Allow") 
                dontAllowButton.tap()
                sleep(2)
            } else if okButton.exists {
                print("📱 Handling permission: OK")
                okButton.tap()
                sleep(2)
            } else {
                break // No more dialogs
            }
        }
    }
}