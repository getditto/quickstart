import XCTest

final class TasksUITests: XCTestCase {

    func testDidFindOnlyGitHubSeededDocument() throws {
        let app = XCUIApplication()

        // Handle permission dialogs that may appear
        addUIInterruptionMonitor(withDescription: "Local Network Permission") { alert in
            let allowButton = alert.buttons["Allow"]
            if allowButton.exists {
                allowButton.tap()
                return true
            }
            return false
        }

        app.launch()

        // Wait a moment for permission dialog to appear and be handled
        sleep(2)

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "App should launch successfully")

        // Get seeded GitHub identifiers from environment (set by BrowserStack setEnvVariables)
        let githubRunID = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""

        print("🔍 GitHub Run Info:")
        print("  GITHUB_RUN_ID: '\(githubRunID)'")
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")

        guard !githubRunID.isEmpty, !githubRunNumber.isEmpty else {
            XCTFail("Missing GITHUB_RUN_ID or GITHUB_RUN_NUMBER - expected for BrowserStack validation")
            return
        }

        // Get the exact document title that GitHub Actions seeded
        let expectedTitle = ProcessInfo.processInfo.environment["DITTO_CLOUD_TASK_TITLE"] ?? ""

        guard !expectedTitle.isEmpty else {
            XCTFail("Missing DITTO_CLOUD_TASK_TITLE - expected exact document title from GitHub Actions")
            return
        }

        print("🔍 Looking for exact document with title: '\(expectedTitle)'")

        // Make maxWaitTime configurable via environment variable for BrowserStack environments
        let maxWaitTimeEnv = ProcessInfo.processInfo.environment["SYNC_MAX_WAIT_SECONDS"]
        let maxWaitTime: TimeInterval = {
            if let envValue = maxWaitTimeEnv, let parsed = TimeInterval(envValue), parsed > 0 {
                return parsed
            }
            return 10
        }()
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
                        print("✅ FOUND EXACT MATCH! Document '\(label)' found at cell[\(i)]")
                        print("🎉 Test should PASS - document sync working!")
                        found = true
                        break
                    } else {
                        print("   ❌ No match (expected exact: '\(expectedTitle)')")
                    }
                }
            }

            if !found {
                print("💤 Waiting up to 1 second for collection view to refresh before retry...")
                _ = app.collectionViews.firstMatch.waitForExistence(timeout: 1)
            }
        }

        // Final summary
        let finalElapsed = Date().timeIntervalSince(start)
        if found {
            print("🎉 SUCCESS: Found exact GitHub-seeded document '\(expectedTitle)' after \(String(format: "%.1f", finalElapsed))s")
            print("✅ This proves GitHub Actions → Ditto Cloud → BrowserStack sync is working!")
            print("🏆 Inverted timestamp ensured document appeared at top of list!")
        } else {
            print("❌ FAILURE: Exact document '\(expectedTitle)' not found after \(String(format: "%.1f", finalElapsed))s")
            print("💡 This means either:")
            print("   1. GitHub Actions didn't seed the document")
            print("   2. Ditto Cloud sync is not working")
            print("   3. Environment variable DITTO_CLOUD_TASK_TITLE is incorrect")
        }

        XCTAssertTrue(found, "GitHub-seeded document '\(expectedTitle)' not found")
    }
}
