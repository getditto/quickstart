import XCTest

final class TasksUITests: XCTestCase {

    func testDidFindOnlyGitHubSeededDocument() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "App should launch successfully")

        // Get seeded GitHub identifiers
        let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""

        guard !githubRunId.isEmpty, !githubRunNumber.isEmpty else {
            XCTFail("Missing GITHUB_RUN_ID or GITHUB_RUN_NUMBER")
            return
        }

        let expectedTitle = "000_ci_test_\(githubRunId)_\(githubRunNumber)"

        let maxWaitTime: TimeInterval = 10
        let start = Date()
        var found = false

        while Date().timeIntervalSince(start) < maxWaitTime, !found {
            let cells = app.collectionViews.firstMatch.cells
            print("Checking \(cells.count) cells...")

            for i in 0..<cells.count {
                let cell = cells.element(boundBy: i)
                guard cell.exists else { continue }

                let label = cell.staticTexts.firstMatch.label
                print("Cell[\(i)] label: '\(label)'")
                if label == expectedTitle {
                    found = true
                    break
                }
            }

            if !found {
                sleep(1)
            }
        }

        XCTAssertTrue(found, "GitHub-seeded document '\(expectedTitle)' not found")
    }
}
