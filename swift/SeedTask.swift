#!/usr/bin/env swift

import Foundation
import Ditto

/**
 * Simple script to seed a test task for BrowserStack verification
 */

print("🌱 Starting task seeding for BrowserStack test...")

// Read environment variables
guard let appId = ProcessInfo.processInfo.environment["DITTO_APP_ID"],
      let token = ProcessInfo.processInfo.environment["DITTO_PLAYGROUND_TOKEN"],
      let authUrl = ProcessInfo.processInfo.environment["DITTO_AUTH_URL"],
      let runNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] else {
    print("❌ Missing required environment variables")
    exit(1)
}

let testTaskText = "Test Task from BrowserStack #\(runNumber)"
print("📝 Seeding task: \(testTaskText)")

do {
    // Initialize Ditto
    let ditto = Ditto(identity: .onlinePlayground(appId: appId, token: token))
    try ditto.disableSyncWithV3()
    
    // Insert test task
    let taskId = "browserstack_test_\(runNumber)_\(Int(Date().timeIntervalSince1970))"
    
    try ditto.store.execute(
        "INSERT INTO tasks (_id, text, isCompleted) VALUES (?, ?, ?)",
        arguments: [taskId, testTaskText, false]
    )
    
    // Start sync to push to cloud
    try ditto.startSync()
    
    // Give it time to sync
    print("⏰ Waiting for sync to complete...")
    Thread.sleep(forTimeInterval: 10)
    
    print("✅ Task seeded successfully!")
    print("🎯 BrowserStack test should find: \(testTaskText)")
    
} catch {
    print("❌ Failed to seed task: \(error)")
    exit(1)
}

exit(0)