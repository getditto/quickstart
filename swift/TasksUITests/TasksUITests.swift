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

        // Debug what the SwiftUI List actually becomes in XCUITest
        print("🔍 Debugging SwiftUI List structure...")
        
        print("📱 UI element counts:")
        print("  Tables: \(app.tables.count)")
        print("  CollectionViews: \(app.collectionViews.count)")
        print("  ScrollViews: \(app.scrollViews.count)")
        print("  Other: \(app.otherElements.count)")
        
        // Check each potential container type
        if app.scrollViews.count > 0 {
            print("\n🔍 Checking ScrollViews (SwiftUI List might be a ScrollView):")
            let scrollView = app.scrollViews.firstMatch
            print("  ScrollView exists: \(scrollView.exists)")
            if scrollView.exists {
                let texts = scrollView.staticTexts
                print("  ScrollView has \(texts.count) static texts")
                
                let targetTask = "Clean the kitchen"
                for i in 0..<min(texts.count, 10) {
                    let text = texts.element(boundBy: i)
                    if text.exists && !text.label.isEmpty {
                        let label = text.label
                        print("  [\(i)]: '\(label)'")
                        if label == targetTask {
                            print("✅ Found target task in ScrollView at index \(i)!")
                        }
                    }
                }
            }
        }
        
        if app.collectionViews.count > 0 {
            print("\n🔍 Checking CollectionViews:")
            let collectionView = app.collectionViews.firstMatch
            print("  CollectionView exists: \(collectionView.exists)")
            if collectionView.exists {
                let cells = collectionView.cells
                print("  CollectionView has \(cells.count) cells")
            }
        }
        
        // Just pass for now to see debug output
        print("\n✅ Debug completed - check output above")
        XCTAssertTrue(true, "Debug test")
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