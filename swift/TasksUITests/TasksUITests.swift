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
        
        // Look for GitHub test document using exact same env vars as workflow
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""
        let expectedDocId = "github_test_\(githubRunId)_\(githubRunNumber)"
        let expectedTitle = "GitHub Test Task \(githubRunId)"
        
        print("🔍 Looking for EXACT document:")
        print("   - Document ID: '\(expectedDocId)'")  
        print("   - Title: '\(expectedTitle)'")
        
        // Wait for the seeded document to appear (like JavaScript wait_for_sync_document)
        var foundDocument = false
        let maxWaitTime = 30.0
        let startTime = Date()
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            // Scroll up to ensure we see all content  
            if app.tables.firstMatch.exists {
                app.tables.firstMatch.swipeDown() // Scroll to top
                sleep(1)
            }
            
            // Search for the EXACT document title in cells
            let taskCells = app.tables.cells
            
            for i in 0..<taskCells.count {
                let cell = taskCells.element(boundBy: i)
                if cell.exists {
                    let cellText = cell.label
                    // Look for exact title match
                    if cellText == expectedTitle {
                        print("✅ Found EXACT document in cell: '\(cellText)'")
                        foundDocument = true
                        break
                    }
                    // Also check if cell contains the exact title as part of a larger string
                    if cellText.contains(expectedTitle) {
                        print("✅ Found document (partial match) in cell: '\(cellText)'")
                        foundDocument = true
                        break
                    }
                }
            }
            
            if foundDocument {
                break
            }
            
            // Also check static text elements for exact title
            let staticTexts = app.staticTexts
            for i in 0..<staticTexts.count {
                let text = staticTexts.element(boundBy: i)
                if text.exists {
                    let textContent = text.label
                    if textContent == expectedTitle || textContent.contains(expectedTitle) {
                        print("✅ Found EXACT document in text: '\(textContent)'")
                        foundDocument = true
                        break
                    }
                }
            }
            
            if foundDocument {
                break
            }
            
            // Scroll down to check for more content
            if app.tables.firstMatch.exists {
                app.tables.firstMatch.swipeUp() // Scroll down to see more
                sleep(1)
            }
            
            sleep(2) // Check every 2 seconds
        }
        
        if !foundDocument {
            print("❌ EXACT test document not found after \(maxWaitTime) seconds")
            print("🔍 Expected EXACT document:")
            print("   - Document ID: '\(expectedDocId)'")
            print("   - Title: '\(expectedTitle)'")
            print("   - GitHub Run ID: '\(githubRunId)'")  
            print("   - GitHub Run Number: '\(githubRunNumber)'")
            
            // Debug: show what we do have
            print("📱 Available task cells:")
            let allCells = app.tables.cells
            for i in 0..<min(allCells.count, 15) {
                let cell = allCells.element(boundBy: i)
                if cell.exists && !cell.label.isEmpty {
                    let cellText = cell.label
                    let exactMatch = cellText == expectedTitle
                    let containsTitle = cellText.contains(expectedTitle) 
                    print("   Cell [\(i)]: '\(cellText)' [exact:\(exactMatch), contains:\(containsTitle)]")
                }
            }
            
            print("📝 Available static texts:")
            let allTexts = app.staticTexts
            for i in 0..<min(allTexts.count, 10) {
                let text = allTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty && text.label.count > 2 {
                    let textContent = text.label
                    let exactMatch = textContent == expectedTitle
                    let containsTitle = textContent.contains(expectedTitle)
                    print("   Text [\(i)]: '\(textContent)' [exact:\(exactMatch), contains:\(containsTitle)]")
                }
            }
            
            XCTFail("Failed to sync EXACT test document '\(expectedTitle)' from Ditto Cloud")
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