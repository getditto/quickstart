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

        guard !expectedTitle.isEmpty else {
            XCTFail("Missing GITHUB_TEST_DOC_TITLE - expected exact document title from GitHub Actions")
            return
        }

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
            } else if scrollViews.count > 0 {
                targetContainer = scrollViews.firstMatch
            } else {
                // Fallback to main app window
                targetContainer = app
            }

            if let container = targetContainer {
                let textElements = container.staticTexts
                itemsFound = textElements.count

                if itemsFound > 0 {
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
                        
                        // Also check the value property in case it contains full text
                        let value = textElement.value as? String ?? ""

                        // Check both label and value for matches
                        let textToCheck = [label, value].filter { !$0.isEmpty }
                        
                        for text in textToCheck {
                            if text == expectedTitle {
                                found = true
                                break
                            }
                        }
                        
                        if found { break }
                    }
                }
            }

            if !found {
                sleep(1)
            }
        }


        XCTAssertTrue(found, "GitHub-seeded KMP document '\(expectedTitle)' not found")
    }
}
