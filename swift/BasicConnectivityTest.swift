#!/usr/bin/env swift

import Foundation
import Network

/**
 * Basic connectivity test for Ditto cloud endpoints
 * This tests network connectivity to Ditto cloud services without requiring the SDK
 */

print("🌐 Testing connectivity to Ditto Cloud endpoints...")

func testHTTPSConnectivity(to urlString: String) -> Bool {
    guard let url = URL(string: urlString) else {
        print("❌ Invalid URL: \(urlString)")
        return false
    }
    
    let semaphore = DispatchSemaphore(value: 0)
    var success = false
    
    let task = URLSession.shared.dataTask(with: url) { data, response, error in
        if let error = error {
            print("❌ Connection failed to \(urlString): \(error.localizedDescription)")
        } else if let httpResponse = response as? HTTPURLResponse {
            print("✅ Connected to \(urlString) - Status: \(httpResponse.statusCode)")
            success = true
        }
        semaphore.signal()
    }
    
    task.resume()
    _ = semaphore.wait(timeout: .now() + 10)
    
    return success
}

func testWebSocketConnectivity(to urlString: String) -> Bool {
    guard let url = URL(string: urlString) else {
        print("❌ Invalid WebSocket URL: \(urlString)")
        return false
    }
    
    if #available(macOS 10.15, *) {
        let semaphore = DispatchSemaphore(value: 0)
        var success = false
        
        let task = URLSession.shared.webSocketTask(with: url)
        task.resume()
        
        task.send(.string("ping")) { error in
            if let error = error {
                print("❌ WebSocket connection failed to \(urlString): \(error.localizedDescription)")
            } else {
                print("✅ WebSocket connected to \(urlString)")
                success = true
            }
            task.cancel()
            semaphore.signal()
        }
        
        _ = semaphore.wait(timeout: .now() + 10)
        return success
    } else {
        print("⚠️  WebSocket testing requires macOS 10.15+, skipping...")
        return true
    }
}

// Read environment variables
let appId = ProcessInfo.processInfo.environment["DITTO_APP_ID"] ?? ""
let token = ProcessInfo.processInfo.environment["DITTO_PLAYGROUND_TOKEN"] ?? ""
let authUrl = ProcessInfo.processInfo.environment["DITTO_AUTH_URL"] ?? ""
let websocketUrl = ProcessInfo.processInfo.environment["DITTO_WEBSOCKET_URL"] ?? ""

print("📝 Testing with credentials:")
print("  App ID: \(String(appId.prefix(8)))...")
print("  Token: \(String(token.prefix(8)))...")
print("  Auth URL: \(authUrl)")
print("  WebSocket URL: \(websocketUrl)")
print()

var allTestsPassed = true

// Test 1: Basic HTTPS connectivity to auth URL
print("🔐 Testing HTTPS connectivity to auth endpoint...")
if !testHTTPSConnectivity(to: authUrl) {
    allTestsPassed = false
}

// Test 2: WebSocket connectivity
print("\n🔌 Testing WebSocket connectivity...")
if !testWebSocketConnectivity(to: websocketUrl) {
    allTestsPassed = false
}

// Test 3: Validate credential format
print("\n📋 Validating credential formats...")
if appId.count >= 8 && appId.contains("-") {
    print("✅ App ID format looks valid")
} else {
    print("❌ App ID format invalid")
    allTestsPassed = false
}

if !token.isEmpty && token.count >= 8 {
    print("✅ Token format looks valid")
} else {
    print("❌ Token format invalid")
    allTestsPassed = false
}

if authUrl.hasPrefix("https://") {
    print("✅ Auth URL format valid")
} else {
    print("❌ Auth URL format invalid")
    allTestsPassed = false
}

if websocketUrl.hasPrefix("wss://") {
    print("✅ WebSocket URL format valid")
} else {
    print("❌ WebSocket URL format invalid")
    allTestsPassed = false
}

print("\n=== Connectivity Test Results ===")
if allTestsPassed {
    print("🎉 All connectivity tests passed!")
    print("✅ Ready for real Ditto cloud integration testing")
    exit(0)
} else {
    print("💥 Some connectivity tests failed!")
    print("🔧 Check network connection and credentials")
    exit(1)
}