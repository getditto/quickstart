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
                firstTextField.typeText("BrowserStack Test Task")
                
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

    func testTaskOperations() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30), 
                     "App should launch successfully")

        // Try basic task operations
        let textFields = app.textFields
        if textFields.count > 0 {
            let taskInput = textFields.element(boundBy: 0)
            if taskInput.exists {
                taskInput.tap()
                taskInput.typeText("Test Task \(Date().timeIntervalSince1970)")
                
                // Try to submit
                let buttons = app.buttons
                for i in 0..<buttons.count {
                    let button = buttons.element(boundBy: i)
                    if button.exists && button.isHittable {
                        button.tap()
                        break
                    }
                }
            }
        }

        // Verify app didn't crash
        sleep(5)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after task operations")
    }
}