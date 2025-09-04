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

        // Handle any initial permission dialogs
        sleep(2)
        handlePermissionDialogs(app: app)

        // Look for "Clean the kitchen" in the CollectionView (SwiftUI List)
        print("🔍 Looking for 'Clean the kitchen' in the task list (CollectionView)...")
        
        let targetTask = "Clean the kitchen"
        var foundTask = false
        
        let collectionView = app.collectionViews.firstMatch
        if collectionView.exists {
            let cells = collectionView.cells
            print("📱 Task list has \(cells.count) cells")
            
            // Look through each cell for the target task
            for i in 0..<cells.count {
                let cell = cells.element(boundBy: i)
                if cell.exists {
                    // Check static texts within the cell
                    let texts = cell.staticTexts
                    for j in 0..<texts.count {
                        let text = texts.element(boundBy: j)
                        if text.exists && text.label == targetTask {
                            print("✅ Found target task!")
                            print("  Cell index: \(i)")
                            print("  Text label: '\(text.label)'")
                            foundTask = true
                            break
                        }
                    }
                    if foundTask { break }
                }
            }
            
            if !foundTask {
                // Show what tasks are in each cell
                print("\n📋 Tasks in the list:")
                for i in 0..<min(cells.count, 15) {
                    let cell = cells.element(boundBy: i)
                    if cell.exists {
                        let texts = cell.staticTexts
                        for j in 0..<texts.count {
                            let text = texts.element(boundBy: j)
                            if text.exists && !text.label.isEmpty && text.label.count > 3 {
                                print("  Cell[\(i)]: '\(text.label)'")
                                break // Only show first meaningful text per cell
                            }
                        }
                    }
                }
            }
        } else {
            print("❌ No CollectionView found")
        }
        
        XCTAssertTrue(foundTask, "Should find 'Clean the kitchen' in the task list")
    }
    
    private func handlePermissionDialogs(app: XCUIApplication) {
        // Handle potential permission dialogs aggressively
        for i in 0..<3 {  // Reduced iterations for faster handling
            let allowButton = app.buttons["Allow"]
            let dontAllowButton = app.buttons["Don't Allow"] 
            let okButton = app.buttons["OK"]
            
            if allowButton.exists {
                print("📱 Handling permission dialog \(i + 1): Allow")
                allowButton.tap()
                sleep(1)  // Reduced sleep time
            } else if dontAllowButton.exists {
                print("📱 Handling permission dialog \(i + 1): Don't Allow") 
                dontAllowButton.tap()
                sleep(1)  // Reduced sleep time
            } else if okButton.exists {
                print("📱 Handling permission dialog \(i + 1): OK")
                okButton.tap()
                sleep(1)  // Reduced sleep time
            } else {
                break // No more dialogs
            }
        }
    }
}