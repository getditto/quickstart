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

        // Brief wait for app to settle
        sleep(2)

        // Look for GitHub-seeded document (should fail locally, pass on BrowserStack)
        print("🔍 Looking for GitHub-seeded document in task list...")
        
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""
        
        print("🔍 Environment variables:")
        print("  GITHUB_RUN_ID: '\(githubRunId)'")
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")
        
        // Only look for document if we have actual env vars
        if githubRunId.isEmpty || githubRunNumber.isEmpty {
            print("❌ No GitHub env vars - failing test as expected")
            XCTFail("No GITHUB_RUN_ID or GITHUB_RUN_NUMBER - test should fail locally")
            return
        }
        
        let expectedTitle = "GitHub Test Task \(githubRunId)"
        let expectedDocId = "000_github_test_\(githubRunId)_\(githubRunNumber)"
        
        print("📋 Looking for:")
        print("  Title: '\(expectedTitle)'")
        print("  Doc ID: '\(expectedDocId)'")
        
        var foundDocument = false
        
        let collectionView = app.collectionViews.firstMatch
        if collectionView.exists {
            let cells = collectionView.cells
            print("📱 Task list has \(cells.count) cells")
            
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
                            if label.contains(expectedTitle) || label.contains(expectedDocId) {
                                print("✅ Found GitHub document!")
                                print("  Cell index: \(i)")
                                print("  Text label: '\(label)'")
                                foundDocument = true
                                break
                            }
                        }
                    }
                    if foundDocument { break }
                }
            }
            
            if !foundDocument {
                print("❌ GitHub document not found (expected for local test)")
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
            }
        } else {
            print("❌ No task list found")
        }
        
        XCTAssertTrue(foundDocument, "Should find GitHub-seeded document (will fail locally, pass on BrowserStack)")
    }
    
}