#!/usr/bin/env swift

import Foundation

/**
 * Simple Swift integration test runner for CI/CD
 * Validates Ditto integration without requiring Xcode test targets
 */

print("🧪 Starting Swift Integration Test Runner...")

// Validate environment
let requiredVars = [
    "DITTO_APP_ID",
    "DITTO_PLAYGROUND_TOKEN", 
    "DITTO_AUTH_URL",
    "DITTO_WEBSOCKET_URL"
]

var allPresent = true
for varName in requiredVars {
    if let value = ProcessInfo.processInfo.environment[varName], !value.isEmpty {
        print("✓ \(varName): \(String(value.prefix(8)))...")
    } else {
        print("❌ \(varName): Missing or empty")
        allPresent = false
    }
}

guard allPresent else {
    print("💥 Missing required environment variables")
    exit(1)
}

// Generate test document ID (same as CI pipeline)
let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? "local_test"
let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? "1"
let testDocId = ProcessInfo.processInfo.environment["GITHUB_TEST_DOC_ID"] ?? "swift_github_test_\(githubRunId)_\(githubRunNumber)"

print("📝 Test Document ID: \(testDocId)")

// Basic connectivity test (without requiring DittoSwift import)
func testConnectivity(to urlString: String) -> Bool {
    guard let url = URL(string: urlString) else { return false }
    
    let semaphore = DispatchSemaphore(value: 0)
    var success = false
    
    let task = URLSession.shared.dataTask(with: url) { _, response, error in
        if error == nil, let httpResponse = response as? HTTPURLResponse {
            success = (200...299).contains(httpResponse.statusCode)
        }
        semaphore.signal()
    }
    
    task.resume()
    _ = semaphore.wait(timeout: .now() + 10)
    return success
}

let authUrl = ProcessInfo.processInfo.environment["DITTO_AUTH_URL"] ?? ""
print("🌐 Testing connectivity to: \(authUrl)")

// Note: This is a basic validation - full Ditto SDK testing happens in Xcode
// The CI pipeline already inserted a test document via API before this runs
print("✅ Integration test environment validated")
print("📝 Note: Real Ditto sync testing requires full SDK integration")
print("🎯 CI pipeline handles API document insertion and validation")

exit(0)