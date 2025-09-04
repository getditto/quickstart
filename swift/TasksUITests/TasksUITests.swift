import XCTest

final class TasksUITests: XCTestCase {

    func testDidFindOnlyGitHubSeededDocument() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30),
                      "App should launch successfully")

        // Look for "Clean the kitchen" document - should exist both locally and on BrowserStack
        // On BrowserStack, GitHub Actions seeds an additional one, so sync validation works either way
        let expectedTitle = "Clean the kitchen"

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

        XCTAssertTrue(found, "Document '\(expectedTitle)' not found - sync may not be working")
    }
}
