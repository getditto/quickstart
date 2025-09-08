import XCTest

final class iosAppUITests: XCTestCase {

    func testGitHubSeededDocumentSync() throws {
        // Get the exact document title that must be provided via environment variable
        let expectedTitle = ProcessInfo.processInfo.environment["GITHUB_TEST_DOC_TITLE"] ?? ""
        
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "KMP iOS app should launch successfully")

        // Get seeded GitHub identifiers from environment (optional for local testing)
        let githubRunID = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""

        print("🔍 GitHub Run Info:")
        print("  GITHUB_RUN_ID: '\(githubRunID.isEmpty ? "not set (local testing)" : githubRunID)'")
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber.isEmpty ? "not set (local testing)" : githubRunNumber)'")

        if !githubRunID.isEmpty && !githubRunNumber.isEmpty {
            print("🏗️ Running in CI environment with GitHub identifiers")
        } else {
            print("🧪 Running locally - GitHub identifiers not required")
        }
        
        print("🔍 Environment Variables Debug:")
        print("  GITHUB_TEST_DOC_TITLE: '\(expectedTitle)'")
        print("  All environment variables with 'GITHUB' or 'TEST':")
        for (key, value) in ProcessInfo.processInfo.environment.sorted(by: { $0.key < $1.key }) {
            if key.contains("GITHUB") || key.contains("TEST") || key.contains("DITTO") {
                print("    \(key): '\(value)'")
            }
        }

        guard !expectedTitle.isEmpty else {
            print("❌ Missing GITHUB_TEST_DOC_TITLE - expected exact document title from GitHub Actions")
            XCTFail("Missing GITHUB_TEST_DOC_TITLE - expected exact document title from GitHub Actions")
            return
        }

        print("🔍 Looking for KMP document with title: '\(expectedTitle)'")

        // Make maxWaitTime configurable via environment variable for BrowserStack environments
        let maxWaitTimeEnv = ProcessInfo.processInfo.environment["SYNC_MAX_WAIT_SECONDS"]
        let maxWaitTime: TimeInterval = {
            if let envValue = maxWaitTimeEnv, let parsed = TimeInterval(envValue), parsed > 0 {
                return parsed
            }
            return 15 // KMP may need slightly longer for sync
        }()
        let start = Date()
        var found = false

        while Date().timeIntervalSince(start) < maxWaitTime, !found {
            let elapsed = Date().timeIntervalSince(start)
            print("📱 KMP UI search attempt at \(String(format: "%.1f", elapsed))s elapsed...")

            // For KMP Compose UI, look for LazyColumn items or similar
            // This may need adjustment based on your actual Compose UI structure
            let scrollViews = app.scrollViews
            let lazyColumns = app.otherElements.matching(identifier: "LazyColumn").firstMatch

            var itemsFound = 0
            var targetContainer: XCUIElement?

            if lazyColumns.exists {
                targetContainer = lazyColumns
                print("📋 Found LazyColumn container")
            } else if scrollViews.count > 0 {
                targetContainer = scrollViews.firstMatch
                print("📋 Found ScrollView container")
            } else {
                // Fallback to main app window
                targetContainer = app
                print("📋 Using main app container")
            }

            if let container = targetContainer {
                let textElements = container.staticTexts
                itemsFound = textElements.count
                print("📋 Found \(itemsFound) text elements in container")

                if itemsFound == 0 {
                    print("⚠️ No text elements found - KMP UI might not be loaded yet")
                } else {
                    print("📄 Examining KMP task items:")
                    for i in 0..<itemsFound {
                        let textElement = textElements.element(boundBy: i)
                        guard textElement.exists else {
                            continue
                        }

                        let label = textElement.label
                        print("   Item[\(i)]: '\(label)'")

                        if label == expectedTitle {
                            print("✅ FOUND EXACT MATCH! KMP document '\(label)' found at item[\(i)]")
                            print("🎉 Test should PASS - KMP Ditto sync working!")
                            found = true
                            break
                        } else if label.contains(expectedTitle) || expectedTitle.contains(label) {
                            print("🔍 Partial match found: '\(label)' vs expected '\(expectedTitle)'")
                        }
                    }
                }
            }

            if !found {
                print("💤 Waiting up to 1 second for KMP UI to refresh before retry...")
                sleep(1)
            }
        }

        // Final summary
        let finalElapsed = Date().timeIntervalSince(start)
        if found {
            print("🎉 SUCCESS: Found exact KMP document '\(expectedTitle)' after \(String(format: "%.1f", finalElapsed))s")
            print("✅ This proves GitHub Actions → Ditto Cloud → BrowserStack → KMP sync is working!")
            print("🏆 Kotlin Multiplatform iOS integration validated!")
        } else {
            print("❌ FAILURE: KMP document '\(expectedTitle)' not found after \(String(format: "%.1f", finalElapsed))s")
            print("💡 This means either:")
            print("   1. GitHub Actions didn't seed the document")
            print("   2. Ditto Cloud sync is not working")
            print("   3. KMP Compose UI structure differs from expected")
            print("   4. Environment variable GITHUB_TEST_DOC_TITLE is incorrect")
        }

        XCTAssertTrue(found, "GitHub-seeded KMP document '\(expectedTitle)' not found")
    }
}
