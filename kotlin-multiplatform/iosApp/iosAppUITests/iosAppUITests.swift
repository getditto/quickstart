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
            print("🎯 BrowserStack Log: Searching for document '\(expectedTitle)' - attempt \(Int(elapsed) + 1)")

            // FIXME: KMP Compose UI accessibility labels were not working reliably for iOS automation
            // at the time of implementation. Using generic element detection instead of
            // accessibility identifiers or labels. Future improvement could investigate
            // proper accessibility support for Compose Multiplatform iOS targets.
            
            // For KMP Compose UI, look for LazyColumn items or similar
            // This may need adjustment based on your actual Compose UI structure
            let scrollViews = app.scrollViews
            let lazyColumns = app.otherElements.matching(identifier: "LazyColumn").firstMatch

            var itemsFound = 0
            var targetContainer: XCUIElement?

            if lazyColumns.exists {
                targetContainer = lazyColumns
                print("📋 Found LazyColumn container")
                print("🎯 BrowserStack Log: Using LazyColumn container for document search")
            } else if scrollViews.count > 0 {
                targetContainer = scrollViews.firstMatch
                print("📋 Found ScrollView container")
                print("🎯 BrowserStack Log: Using ScrollView container for document search")
            } else {
                // Fallback to main app window
                targetContainer = app
                print("📋 Using main app container")
                print("🎯 BrowserStack Log: Using main app window for document search")
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

                        // Try to tap the element to expand it in case text is truncated
                        if textElement.exists && textElement.isHittable {
                            textElement.tap()
                            // Brief pause to allow text expansion
                            usleep(200000) // 0.2 seconds
                        }
                        
                        let label = textElement.label
                        print("   Item[\(i)]: '\(label)'")
                        
                        // Also check the value property in case it contains full text
                        let value = textElement.value as? String ?? ""
                        if !value.isEmpty && value != label {
                            print("   Item[\(i)] value: '\(value)'")
                        }

                        // Check both label and value for matches
                        let textToCheck = [label, value].filter { !$0.isEmpty }
                        
                        for text in textToCheck {
                            if text == expectedTitle {
                                print("✅ FOUND EXACT MATCH! KMP document '\(text)' found at item[\(i)]")
                                print("🎯 BrowserStack Log: Document found - exact match!")
                                print("📍 Location: Item[\(i)] in KMP task list")
                                print("🔍 Expected: '\(expectedTitle)'")
                                print("✅ Actual: '\(text)'")
                                
                                // 3 second sleep to see the list of elements in BrowserStack video
                                print("⏱️ Sleeping 3 seconds to capture UI state in BrowserStack...")
                                sleep(3)
                                print("🎉 Test will PASS - KMP Ditto sync working!")
                                
                                found = true
                                break
                            } else if text.contains(expectedTitle) || expectedTitle.contains(text) {
                                print("🔍 Partial match found: '\(text)' vs expected '\(expectedTitle)'")
                                print("📝 BrowserStack Log: Partial match detected")
                            }
                        }
                        
                        if found { break }
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
            print("🎯 BrowserStack Log: TEST PASSED - Document successfully found!")
            print("✅ This proves GitHub Actions → Ditto Cloud → BrowserStack → KMP sync is working!")
            print("🏆 Kotlin Multiplatform iOS integration validated!")
            print("📊 BrowserStack Result: PASS - End-to-end sync validated")
        } else {
            print("❌ FAILURE: KMP document '\(expectedTitle)' not found after \(String(format: "%.1f", finalElapsed))s")
            print("🎯 BrowserStack Log: TEST FAILED - Document not found in UI")
            print("💡 This means either:")
            print("   1. GitHub Actions didn't seed the document")
            print("   2. Ditto Cloud sync is not working") 
            print("   3. KMP Compose UI structure differs from expected")
            print("   4. Environment variable GITHUB_TEST_DOC_TITLE is incorrect")
            print("📊 BrowserStack Result: FAIL - Sync or UI issue detected")
        }

        XCTAssertTrue(found, "GitHub-seeded KMP document '\(expectedTitle)' not found")
    }
}
