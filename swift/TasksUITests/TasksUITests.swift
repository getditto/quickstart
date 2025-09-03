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
        
        // Look for GitHub test document (like JavaScript test does)
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        print("🔍 Looking for GitHub Test Task with run ID: \(githubRunId)")
        
        // Wait for the seeded document to appear (like JavaScript wait_for_sync_document)
        var foundDocument = false
        let maxWaitTime = 30.0
        let startTime = Date()
        
        print("🔍 Looking for document with run ID: '\(githubRunId)' and title containing 'GitHub Test Task'")
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            // Scroll up to ensure we see all content  
            if app.tables.firstMatch.exists {
                app.tables.firstMatch.swipeDown() // Scroll to top
                sleep(1)
            }
            
            // First pass: Look for any content with the run ID (more flexible)
            var runIdFound = false
            let taskCells = app.tables.cells
            
            // Search through all visible cells
            for i in 0..<taskCells.count {
                let cell = taskCells.element(boundBy: i)
                if cell.exists {
                    let cellText = cell.label
                    if cellText.contains(githubRunId) {
                        print("🎯 Found run ID '\(githubRunId)' in cell: '\(cellText)'")
                        runIdFound = true
                        // Check if it's also our test task
                        if cellText.contains("GitHub Test Task") {
                            print("✅ Found complete synced document: \(cellText)")
                            foundDocument = true
                            break
                        }
                    }
                }
            }
            
            if foundDocument {
                break
            }
            
            // Also check static text elements for run ID
            let staticTexts = app.staticTexts
            for i in 0..<staticTexts.count {
                let text = staticTexts.element(boundBy: i)
                if text.exists {
                    let textContent = text.label
                    if textContent.contains(githubRunId) {
                        print("🎯 Found run ID '\(githubRunId)' in text: '\(textContent)'")
                        runIdFound = true
                        if textContent.contains("GitHub Test Task") {
                            print("✅ Found complete synced document in text: \(textContent)")
                            foundDocument = true
                            break
                        }
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
            
            // If we found the run ID but not complete document, log it
            if runIdFound {
                print("📝 Found run ID but incomplete document - continuing to wait...")
            }
            
            sleep(2) // Check every 2 seconds
        }
        
        if !foundDocument {
            print("❌ GitHub test document not found after \(maxWaitTime) seconds")
            print("🔍 Expected to find:")
            print("   - Run ID: '\(githubRunId)'")
            print("   - Title: 'GitHub Test Task \(githubRunId)'")
            print("   - Document ID: 'github_test_\(githubRunId)_*'")
            
            // Debug: show what we do have with more detail
            print("📱 Available UI elements (first 10):")
            let allCells = app.tables.cells
            for i in 0..<min(allCells.count, 10) {
                let cell = allCells.element(boundBy: i)
                if cell.exists && !cell.label.isEmpty {
                    let cellText = cell.label
                    let hasRunId = cellText.contains(githubRunId)
                    let hasGithub = cellText.lowercased().contains("github")
                    let hasTest = cellText.lowercased().contains("test")
                    print("   Cell [\(i)]: '\(cellText)' [runId:\(hasRunId), github:\(hasGithub), test:\(hasTest)]")
                }
            }
            
            print("📝 Available static texts (first 15):")
            let allTexts = app.staticTexts
            for i in 0..<min(allTexts.count, 15) {
                let text = allTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty && text.label.count > 2 {
                    let textContent = text.label
                    let hasRunId = textContent.contains(githubRunId)
                    let hasGithub = textContent.lowercased().contains("github")
                    print("   Text [\(i)]: '\(textContent)' [runId:\(hasRunId), github:\(hasGithub)]")
                }
            }
            
            // Also check for any text containing partial matches
            print("🔎 Searching for partial matches...")
            let searchTerms = [githubRunId, "GitHub", "Test", "github_test"]
            for term in searchTerms {
                var matchCount = 0
                for i in 0..<allCells.count {
                    let cell = allCells.element(boundBy: i)
                    if cell.exists && cell.label.lowercased().contains(term.lowercased()) {
                        matchCount += 1
                        if matchCount <= 3 { // Show first 3 matches
                            print("   Found '\(term)' in cell: '\(cell.label)'")
                        }
                    }
                }
                if matchCount > 0 {
                    print("   Total cells matching '\(term)': \(matchCount)")
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