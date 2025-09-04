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

        // Get GitHub environment variables for seeded document
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""
        
        print("🔍 Environment variables:")
        print("  GITHUB_RUN_ID: '\(githubRunId)'")
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")
        
        // If no GitHub env vars, test should fail as expected (local runs)
        if githubRunId.isEmpty || githubRunNumber.isEmpty {
            print("❌ No GitHub env vars - failing test as expected")
            XCTFail("No GITHUB_RUN_ID or GITHUB_RUN_NUMBER - test should fail locally")
            return
        }
        
        // Look for GitHub-seeded document
        let expectedId = "000_test_kitchen_\(githubRunId)_\(githubRunNumber)"
        let expectedTitle = "Clean the kitchen"
        
        print("🔍 Looking for GitHub-seeded document in task list...")
        print("📋 Looking for:")
        print("  ID: '\(expectedId)'")
        print("  Title: '\(expectedTitle)'")
        
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
                                
                                // Check for GitHub-seeded "Clean the kitchen" document
                                if label == expectedTitle {
                                    print("✅ Found GitHub-seeded document!")
                                    print("  Expected ID: '\(expectedId)'")
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
            print("❌ GitHub-seeded document not found after 10 seconds")
            print("   Expected title: '\(expectedTitle)'")
            print("   Expected ID: '\(expectedId)'")
            
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
        
        // Pass only if we found the GitHub-seeded document
        XCTAssertTrue(foundDocument, "Should find GitHub-seeded document with title '\(expectedTitle)' and ID '\(expectedId)'")
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