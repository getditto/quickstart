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

    func testDittoSyncedDocument() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30), 
                     "App should launch successfully")

        // Look for seeded document that was uploaded to Ditto cloud
        let runNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? "unknown"
        let seededTaskText = "Test Task from BrowserStack #\(runNumber)"
        
        print("🔍 LOOKING for seeded document: \(seededTaskText)")

        // Handle permission dialogs first 
        sleep(3)
        handlePermissionDialogs(app: app)

        // Wait longer for Ditto sync to download the seeded document
        print("⏳ Waiting for Ditto to sync seeded document from cloud...")
        sleep(10)  // Give Ditto time to sync
        
        // Look for the seeded task in the main list
        print("🔍 Searching for seeded document in main task list...")
        let taskCells = app.tables.cells.containing(NSPredicate(format: "label CONTAINS[cd] '\(seededTaskText)'"))
        let taskFound = taskCells.firstMatch.waitForExistence(timeout: 20) // Wait up to 20s for sync
        
        if taskFound {
            print("✅ SUCCESS: Found seeded document in main list - Ditto cloud sync working!")
        } else {
            // Debug: show available cells to help troubleshoot  
            print("❌ FAIL: Seeded document not found, available cells:")
            let allCells = app.tables.cells
            for i in 0..<min(allCells.count, 10) {
                let cell = allCells.element(boundBy: i)
                if cell.exists {
                    print("   - '\(cell.label)'")
                }
            }
            
            // Also check static text elements in case tasks appear differently
            print("📝 Available static text elements:")
            let staticTexts = app.staticTexts
            for i in 0..<min(staticTexts.count, 10) {
                let text = staticTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty {
                    print("   - '\(text.label)'")
                }
            }
        }
        
        XCTAssertTrue(taskFound, "Seeded document should be synced from Ditto cloud and visible in main list")

        // Verify app stability
        sleep(5)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after sync verification")
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