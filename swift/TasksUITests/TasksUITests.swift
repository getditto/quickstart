import XCTest

final class TasksUITests: XCTestCase {

    func testDidFindOnlyGitHubSeededDocument() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "App should launch successfully")

        // Get seeded GitHub identifiers from launch arguments or environment
        let args = ProcessInfo.processInfo.arguments
        let githubRunId = getArgumentValue(args: args, key: "-GITHUB_RUN_ID") ?? 
                          ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? ""
        let githubRunNumber = getArgumentValue(args: args, key: "-GITHUB_RUN_NUMBER") ??
                              ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? ""

        print("🔍 GitHub Run Info:")
        print("  Launch args: \(args)")
        print("  GITHUB_RUN_ID: '\(githubRunId)'")  
        print("  GITHUB_RUN_NUMBER: '\(githubRunNumber)'")

        guard !githubRunId.isEmpty, !githubRunNumber.isEmpty else {
            XCTFail("Missing GITHUB_RUN_ID or GITHUB_RUN_NUMBER - expected for BrowserStack validation")
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
    
    private func getArgumentValue(args: [String], key: String) -> String? {
        guard let index = args.firstIndex(of: key), index + 1 < args.count else {
            return nil
        }
        return args[index + 1]
    }
}
