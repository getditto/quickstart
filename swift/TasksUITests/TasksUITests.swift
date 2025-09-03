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
        
        // Look for GitHub test document (like JavaScript test does)
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        print("🔍 Looking for GitHub Test Task with run ID: \(githubRunId)")
        
        // Wait for the seeded document to appear (like JavaScript wait_for_sync_document)
        var foundDocument = false
        let maxWaitTime = 30.0
        let startTime = Date()
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            // Look for table cells containing the GitHub run ID
            let taskCells = app.tables.cells
            for i in 0..<taskCells.count {
                let cell = taskCells.element(boundBy: i)
                if cell.exists {
                    let cellText = cell.label
                    if cellText.contains(githubRunId) && cellText.contains("GitHub Test Task") {
                        print("✅ Found synced document: \(cellText)")
                        foundDocument = true
                        break
                    }
                }
            }
            
            if foundDocument {
                break
            }
            
            // Also check static text elements
            let staticTexts = app.staticTexts
            for i in 0..<staticTexts.count {
                let text = staticTexts.element(boundBy: i)
                if text.exists {
                    let textContent = text.label
                    if textContent.contains(githubRunId) && textContent.contains("GitHub Test Task") {
                        print("✅ Found synced document in text: \(textContent)")
                        foundDocument = true
                        break
                    }
                }
            }
            
            if foundDocument {
                break
            }
            
            sleep(1) // Check every second like JavaScript
        }
        
        if !foundDocument {
            print("❌ GitHub test document not found after \(maxWaitTime) seconds")
            // Debug: show what we do have
            print("🔍 Available UI elements:")
            let allCells = app.tables.cells
            for i in 0..<min(allCells.count, 5) {
                let cell = allCells.element(boundBy: i)
                if cell.exists && !cell.label.isEmpty {
                    print("   Cell: '\(cell.label)'")
                }
            }
            let allTexts = app.staticTexts
            for i in 0..<min(allTexts.count, 10) {
                let text = allTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty && text.label.count > 3 {
                    print("   Text: '\(text.label)'")
                }
            }
            XCTFail("Failed to sync test document from Ditto Cloud")
        }
        
        XCTAssertTrue(foundDocument, "GitHub test document should be synced from Ditto Cloud")
        
        // Verify app stability
        sleep(3)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after sync verification")
        
        print("✅ Ditto sync verification completed successfully")
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