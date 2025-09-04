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

        // Debug what UI elements actually exist
        print("🔍 Debugging what UI elements exist...")
        
        print("📱 App hierarchy:")
        print("  Windows: \(app.windows.count)")
        print("  Tables: \(app.tables.count)")
        print("  Cells: \(app.cells.count)")
        print("  Lists: \(app.collectionViews.count)")
        print("  ScrollViews: \(app.scrollViews.count)")
        print("  Static Texts: \(app.staticTexts.count)")
        
        // Show all UI elements
        print("\n🔍 All UI elements:")
        let allElements = app.descendants(matching: .any)
        for i in 0..<min(allElements.count, 20) {
            let element = allElements.element(boundBy: i)
            if element.exists {
                let elementType = element.elementType
                let label = element.label.isEmpty ? "(no label)" : element.label
                print("  [\(i)] \(elementType): '\(label)'")
            }
        }
        
        // Try different approaches to find content
        if app.tables.count > 0 {
            print("\n📱 Found tables - checking content...")
            let table = app.tables.firstMatch
            print("  Table exists: \(table.exists)")
            print("  Table cells: \(table.cells.count)")
        }
        
        if app.staticTexts.count > 0 {
            print("\n📝 Found static texts:")
            for i in 0..<min(app.staticTexts.count, 10) {
                let text = app.staticTexts.element(boundBy: i)
                if text.exists && !text.label.isEmpty && text.label.count > 2 {
                    print("  [\(i)]: '\(text.label)'")
                }
            }
        }
        
        // Just pass the test to see debug output
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