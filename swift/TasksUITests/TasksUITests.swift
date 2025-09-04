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
        
        // Wait up to 20 seconds to find ANY document
        print("🔍 Looking for ANY document (waiting up to 20 seconds)...")
        
        let maxWaitTime = 20.0
        let startTime = Date()
        var documentFound = false
        var foundDocuments: [String] = []
        
        while Date().timeIntervalSince(startTime) < maxWaitTime {
            handlePermissionDialogs(app: app)
            
            if app.tables.firstMatch.exists {
                let cells = app.tables.firstMatch.cells
                let cellCount = cells.count
                print("📱 Found \(cellCount) cells...")
                
                foundDocuments.removeAll()
                for i in 0..<cellCount {
                    let cell = cells.element(boundBy: i)
                    if cell.exists && !cell.label.isEmpty {
                        let cellText = cell.label
                        foundDocuments.append("[\(i)]: '\(cellText)'")
                    }
                }
                
                if !foundDocuments.isEmpty {
                    print("✅ Found \(foundDocuments.count) documents:")
                    for doc in foundDocuments.prefix(5) {
                        print("   \(doc)")
                    }
                    documentFound = true
                    break
                }
            }
            
            sleep(1)
        }
        
        if !documentFound {
            print("❌ No documents found after 20 seconds")
            XCTFail("No documents found after waiting 20 seconds")
        }
        
        XCTAssertTrue(documentFound, "Should find at least one document")
        
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