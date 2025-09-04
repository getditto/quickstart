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

        // Handle any permission dialogs that appear
        sleep(2)
        handlePermissionDialogs(app: app)

        // Look for GitHub-seeded document - should fail locally, pass on BrowserStack
        print("🔍 Looking for GitHub-seeded document in task list...")
        
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""
        let expectedTitle = "GitHub Test Task \(githubRunId)"
        let expectedDocId = "000_github_test_\(githubRunId)_\(githubRunNumber)"
        
        print("🔍 Environment variables:")
        print("  GITHUB_RUN_ID: '\(githubRunId)'")
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")
        print("📋 Looking for:")
        print("  Title: '\(expectedTitle)'")
        print("  Doc ID: '\(expectedDocId)'")
        
        var foundDocument = false
        let maxWaitTime = 10.0
        let startTime = Date()
        
        // Wait up to 10 seconds for the GitHub document to appear
        while Date().timeIntervalSince(startTime) < maxWaitTime && !foundDocument {
            handlePermissionDialogs(app: app)
            
            let collectionView = app.collectionViews.firstMatch
            if collectionView.exists {
                let cells = collectionView.cells
                print("📱 Task list has \(cells.count) cells (checking at \(String(format: "%.1f", Date().timeIntervalSince(startTime)))s)")
                
                // Look through each cell for the GitHub document
                for i in 0..<cells.count {
                    let cell = cells.element(boundBy: i)
                    if cell.exists {
                        // Check static texts within the cell
                        let texts = cell.staticTexts
                        for j in 0..<texts.count {
                            let text = texts.element(boundBy: j)
                            if text.exists && !text.label.isEmpty {
                                let label = text.label
                                
                                // Check for GitHub document (will only match if real env vars and document exists)
                                if label.contains(expectedTitle) || label.contains(expectedDocId) {
                                    print("✅ Found GitHub document!")
                                    print("  Cell index: \(i)")
                                    print("  Text label: '\(label)'")
                                    print("  Found after: \(String(format: "%.1f", Date().timeIntervalSince(startTime)))s")
                                    foundDocument = true
                                    break
                                }
                            }
                        }
                        if foundDocument { break }
                    }
                }
            }
            
            if !foundDocument {
                sleep(1) // Wait 1 second before retrying
            }
        }
            
        if !foundDocument {
            print("❌ GitHub document not found after 10 seconds")
            
            // Show what documents we did find
            let collectionView = app.collectionViews.firstMatch
            if collectionView.exists {
                let cells = collectionView.cells
                print("\n📋 Available documents:")
                for i in 0..<min(cells.count, 10) {
                    let cell = cells.element(boundBy: i)
                    if cell.exists {
                        let texts = cell.staticTexts
                        for j in 0..<texts.count {
                            let text = texts.element(boundBy: j)
                            if text.exists && !text.label.isEmpty && text.label.count > 3 {
                                print("  Cell[\(i)]: '\(text.label)'")
                                break
                            }
                        }
                    }
                }
            } else {
                print("❌ No task list found")
            }
        }
        
        // Pass only if we found the GitHub document
        XCTAssertTrue(foundDocument, "Should find GitHub-seeded document")
    }
    
    private func handlePermissionDialogs(app: XCUIApplication) {
        // Handle potential permission dialogs
        for i in 0..<3 {
            let allowButton = app.buttons["Allow"]
            let dontAllowButton = app.buttons["Don't Allow"] 
            let okButton = app.buttons["OK"]
            
            if allowButton.exists {
                print("📱 Handling permission: Allow")
                allowButton.tap()
                sleep(1)
            } else if dontAllowButton.exists {
                print("📱 Handling permission: Don't Allow") 
                dontAllowButton.tap()
                sleep(1)
            } else if okButton.exists {
                print("📱 Handling permission: OK")
                okButton.tap()
                sleep(1)
            } else {
                break // No more dialogs
            }
        }
    }
}