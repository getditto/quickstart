import XCTest

final class TasksUITests: XCTestCase {

    func testDidFindOnlyGitHubSeededDocument() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "App should launch successfully")

        // Get seeded GitHub identifiers from environment (set by BrowserStack setEnvVariables)
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""

        print("🔍 GitHub Run Info:")
        print("  GITHUB_RUN_ID: '\(githubRunId)'")  
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")

        guard !githubRunId.isEmpty, !githubRunNumber.isEmpty else {
            XCTFail("Missing GITHUB_RUN_ID or GITHUB_RUN_NUMBER - expected for BrowserStack validation")
            return
        }

        let expectedTitle = "000_ci_test_\(githubRunId)_\(githubRunNumber)"
        
        print("🔍 Looking for document with title: '\(expectedTitle)'")

        let maxWaitTime: TimeInterval = 10
        let start = Date()
        var found = false

        while Date().timeIntervalSince(start) < maxWaitTime, !found {
            let elapsed = Date().timeIntervalSince(start)
            print("📱 Search attempt at \(String(format: "%.1f", elapsed))s elapsed...")
            
            let cells = app.collectionViews.firstMatch.cells
            print("📋 Found \(cells.count) cells in collection view")

            if cells.count == 0 {
                print("⚠️ No cells found - UI might not be loaded yet")
            } else {
                print("📄 Examining all documents:")
                for i in 0..<cells.count {
                    let cell = cells.element(boundBy: i)
                    guard cell.exists else { 
                        print("   Cell[\(i)]: NOT EXISTS")
                        continue 
                    }

                    let label = cell.staticTexts.firstMatch.label
                    print("   Cell[\(i)]: '\(label)'")
                    
                    if label == expectedTitle {
                        print("✅ FOUND MATCH! Document '\(expectedTitle)' found at cell[\(i)]")
                        print("🎉 Test should PASS - document sync working!")
                        found = true
                        break
                    } else {
                        print("   ❌ No match (expected: '\(expectedTitle)')")
                    }
                }
            }

            if !found {
                print("💤 Waiting 1 second before retry...")
                sleep(1)
            }
        }

        // Final summary
        let finalElapsed = Date().timeIntervalSince(start)
        if found {
            print("🎉 SUCCESS: Found document '\(expectedTitle)' after \(String(format: "%.1f", finalElapsed))s")
            print("✅ This proves GitHub Actions → Ditto Cloud → BrowserStack sync is working!")
        } else {
            print("❌ FAILURE: Document '\(expectedTitle)' not found after \(String(format: "%.1f", finalElapsed))s")
            print("💡 This means either:")
            print("   1. GitHub Actions didn't seed the document")
            print("   2. Ditto Cloud sync is not working") 
            print("   3. Expected title is wrong (in this case, intentionally broken with 'abc')")
        }
        
        XCTAssertTrue(found, "GitHub-seeded document '\(expectedTitle)' not found")
    }
}
