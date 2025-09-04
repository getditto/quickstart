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

        // Handle permission dialogs 
        sleep(3)
        handlePermissionDialogs(app: app)

        // Wait for initial sync to complete
        print("⏳ Waiting for initial sync...")
        sleep(5)
        
        // Wait to detect specific document: "Clean the kitchen"
        print("⏳ Waiting up to 20 seconds to detect 'Clean the kitchen' document...")
        
        let targetDocument = "Clean the kitchen"
        let maxWaitTime = 20.0
        let startTime = Date()
        var documentFound = false
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            handlePermissionDialogs(app: app)
            
            if app.tables.firstMatch.exists {
                let cells = app.tables.firstMatch.cells
                print("📱 Checking \(cells.count) cells...")
                
                for i in 0..<cells.count {
                    let cell = cells.element(boundBy: i)
                    if cell.exists && cell.label.contains(targetDocument) {
                        print("✅ Found target document: '\(cell.label)'")
                        documentFound = true
                        break
                    }
                }
                
                if documentFound {
                    break
                }
            }
            
            sleep(1)
        }
        
        if !documentFound {
            print("❌ Target document '\(targetDocument)' not found after 20 seconds")
            
            // Show what documents we did find
            if app.tables.firstMatch.exists {
                let cells = app.tables.firstMatch.cells
                print("📱 Available documents:")
                for i in 0..<min(cells.count, 10) {
                    let cell = cells.element(boundBy: i)
                    if cell.exists && !cell.label.isEmpty {
                        print("   [\(i)]: '\(cell.label)'")
                    }
                }
            }
            
            XCTFail("Target document '\(targetDocument)' not found")
        }
        
        XCTAssertTrue(documentFound, "Should find '\(targetDocument)' document")
        
        // Verify app stability
        sleep(3)
        XCTAssertTrue(app.state == .runningForeground, 
                     "App should remain stable after sync verification")
        
        print("✅ Ditto sync verification completed successfully")
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