#!/usr/bin/env swift

import Foundation

/**
 * Local test runner for validating integration test structure
 * This validates the test setup without requiring real Ditto credentials
 */

print("🧪 Running Local Swift Integration Test Validation...")

// Test 1: Environment variable validation
func testEnvironmentSetup() -> Bool {
    print("📝 Testing environment variable setup...")
    
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
    
    return allPresent
}

// Test 2: Test document ID generation
func testDocumentIdGeneration() -> Bool {
    print("📝 Testing test document ID generation...")
    
    let githubRunId = ProcessInfo.processInfo.environment["GITHUB_RUN_ID"] ?? "local_test"
    let githubRunNumber = ProcessInfo.processInfo.environment["GITHUB_RUN_NUMBER"] ?? "1"
    let testDocumentId = "swift_github_test_\(githubRunId)_\(githubRunNumber)"
    
    print("✓ Generated test document ID: \(testDocumentId)")
    
    // Validate format
    let components = testDocumentId.split(separator: "_")
    if components.count >= 4 && components[0] == "swift" && components[1] == "github" {
        print("✓ Document ID has correct format")
        return true
    } else {
        print("❌ Document ID has invalid format")
        return false
    }
}

// Test 3: Basic Swift syntax validation for integration tests
func testIntegrationTestStructure() -> Bool {
    print("📝 Testing integration test structure...")
    
    // Check if our integration test file exists and is readable
    let testFilePath = "Tests/DittoSyncIntegrationTests.swift"
    let fileManager = FileManager.default
    
    if fileManager.fileExists(atPath: testFilePath) {
        print("✓ Integration test file exists: \(testFilePath)")
        
        do {
            let content = try String(contentsOfFile: testFilePath)
            
            // Check for key test methods
            let requiredMethods = [
                "testCloudSyncFromAPI",
                "testLocalToCloudSync", 
                "testCRUDWithSync"
            ]
            
            var allMethodsFound = true
            for method in requiredMethods {
                if content.contains(method) {
                    print("✓ Found test method: \(method)")
                } else {
                    print("❌ Missing test method: \(method)")
                    allMethodsFound = false
                }
            }
            
            // Check for imports
            if content.contains("import DittoSwift") {
                print("✓ Found DittoSwift import")
            } else {
                print("❌ Missing DittoSwift import")
                allMethodsFound = false
            }
            
            return allMethodsFound
            
        } catch {
            print("❌ Failed to read integration test file: \(error)")
            return false
        }
    } else {
        print("❌ Integration test file not found: \(testFilePath)")
        return false
    }
}

// Test 4: Validate CI pipeline integration
func testCIPipelineIntegration() -> Bool {
    print("📝 Testing CI pipeline integration...")
    
    let ciFile = "../.github/workflows/swift-ci-enhanced.yml"
    let fileManager = FileManager.default
    
    if fileManager.fileExists(atPath: ciFile) {
        do {
            let content = try String(contentsOfFile: ciFile)
            
            let requiredElements = [
                "Insert test document into Ditto Cloud",
                "Run real integration tests", 
                "GITHUB_TEST_DOC_ID",
                "swift TestRunner.swift"
            ]
            
            var allFound = true
            for element in requiredElements {
                if content.contains(element) {
                    print("✓ Found CI element: \(element)")
                } else {
                    print("❌ Missing CI element: \(element)")
                    allFound = false
                }
            }
            
            return allFound
            
        } catch {
            print("❌ Failed to read CI file: \(error)")
            return false
        }
    } else {
        print("❌ CI pipeline file not found")
        return false
    }
}

// Run all tests
let tests = [
    ("Environment Setup", testEnvironmentSetup),
    ("Document ID Generation", testDocumentIdGeneration),
    ("Integration Test Structure", testIntegrationTestStructure),
    ("CI Pipeline Integration", testCIPipelineIntegration)
]

var passedTests = 0
let totalTests = tests.count

print("\n🚀 Running \(totalTests) local validation tests...\n")

for (name, test) in tests {
    print("--- Testing: \(name) ---")
    if test() {
        print("✅ PASSED: \(name)\n")
        passedTests += 1
    } else {
        print("❌ FAILED: \(name)\n")
    }
}

print("=== Local Test Results ===")
print("Passed: \(passedTests)/\(totalTests)")

if passedTests == totalTests {
    print("🎉 All local validation tests passed!")
    print("📝 Integration tests are properly structured for CI/CD")
    print("⚠️  Note: Real Ditto cloud testing requires actual credentials")
    exit(0)
} else {
    print("💥 Some validation tests failed!")
    print("🔧 Please fix the issues before running in CI")
    exit(1)
}