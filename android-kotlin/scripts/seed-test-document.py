#!/usr/bin/env python3
"""
Local test document seeding script for Android Kotlin Ditto integration tests.

This script reads credentials from the .env file and creates a test document
in Ditto Cloud that can be verified by the Android integration tests.

Usage:
    python3 scripts/seed-test-document.py
    python3 scripts/seed-test-document.py --doc-id custom_test_123
    python3 scripts/seed-test-document.py --title "Custom Test Task"

Prerequisites:
    pip3 install requests
    # OR if using externally managed environment:
    pip3 install --break-system-packages requests
"""

import os
import sys
import argparse
import json
import time
from datetime import datetime
from pathlib import Path

try:
    import requests
except ImportError:
    print("❌ Python 'requests' library is required but not installed")
    print("   Please install it with: pip3 install requests")
    print("   Or if using externally managed environment:")
    print("   pip3 install --break-system-packages requests")
    sys.exit(1)

def load_env_file(env_path=".env"):
    """Load environment variables from .env file."""
    env_vars = {}
    
    # Look for .env file in current directory and parent directories
    current_dir = Path.cwd()
    for path in [current_dir] + list(current_dir.parents):
        env_file = path / env_path
        if env_file.exists():
            print(f"📁 Loading environment from: {env_file}")
            with open(env_file, 'r') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#') and '=' in line:
                        key, value = line.split('=', 1)
                        # Remove quotes if present
                        value = value.strip().strip('"').strip("'")
                        env_vars[key] = value
            break
    else:
        print("❌ No .env file found in current directory or parent directories")
        print("   Please ensure .env file exists with required Ditto credentials")
        return None
    
    # Check required variables
    required_vars = ['DITTO_APP_ID', 'DITTO_PLAYGROUND_TOKEN', 'DITTO_AUTH_URL', 'DITTO_WEBSOCKET_URL']
    
    # Also check for API credentials that might be in secrets
    if 'DITTO_API_KEY' in env_vars and 'DITTO_API_URL' in env_vars:
        required_vars.extend(['DITTO_API_KEY', 'DITTO_API_URL'])
    else:
        print("⚠️  DITTO_API_KEY and DITTO_API_URL not found in .env")
        print("   These are typically stored as GitHub secrets for CI/CD")
        print("   For local testing, you can add them to your .env file")
        print("   Contact your team for the API credentials")
        return None
    
    missing_vars = [var for var in required_vars if not env_vars.get(var)]
    if missing_vars:
        print(f"❌ Missing required environment variables: {', '.join(missing_vars)}")
        return None
    
    return env_vars

def create_test_document(env_vars, doc_id=None, title=None):
    """Create a test document in Ditto Cloud."""
    
    # Generate document ID and title if not provided
    if not doc_id:
        timestamp = int(time.time())
        doc_id = f"local_test_{timestamp}"
    
    if not title:
        current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        title = f"Local Test Task - {current_time}"
    
    print(f"🌱 Creating test document:")
    print(f"   ID: {doc_id}")
    print(f"   Title: {title}")
    
    # Prepare the request
    api_key = env_vars['DITTO_API_KEY']
    api_url = env_vars['DITTO_API_URL']
    
    headers = {
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {api_key}'
    }
    
    payload = {
        "statement": "INSERT INTO tasks DOCUMENTS (:newTask) ON ID CONFLICT DO UPDATE",
        "args": {
            "newTask": {
                "_id": doc_id,
                "title": title,
                "done": False,
                "deleted": False
            }
        }
    }
    
    # Make the API request
    url = f"https://{api_url}/api/v4/store/execute"
    
    print(f"📡 Making request to: {url}")
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        
        print(f"📊 Response status: {response.status_code}")
        
        if response.status_code in [200, 201]:
            print("✅ Successfully created test document in Ditto Cloud!")
            
            try:
                response_data = response.json()
                print(f"📋 Response: {json.dumps(response_data, indent=2)}")
            except:
                print(f"📋 Response text: {response.text}")
            
            return doc_id
            
        else:
            print(f"❌ Failed to create document. Status: {response.status_code}")
            print(f"📋 Response: {response.text}")
            return None
            
    except requests.exceptions.Timeout:
        print("❌ Request timed out after 30 seconds")
        return None
    except requests.exceptions.ConnectionError:
        print("❌ Connection error - check your internet connection and API URL")
        return None
    except Exception as e:
        print(f"❌ Unexpected error: {str(e)}")
        return None

def verify_document_creation(env_vars, doc_id):
    """Verify the document was created by querying Ditto Cloud."""
    print(f"🔍 Verifying document creation for ID: {doc_id}")
    
    api_key = env_vars['DITTO_API_KEY']
    api_url = env_vars['DITTO_API_URL']
    
    headers = {
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {api_key}'
    }
    
    payload = {
        "statement": "SELECT * FROM tasks WHERE _id = :docId",
        "args": {
            "docId": doc_id
        }
    }
    
    url = f"https://{api_url}/api/v4/store/execute"
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            if result.get('items') and len(result['items']) > 0:
                document = result['items'][0]
                print("✅ Document verification successful!")
                print(f"📄 Document data: {json.dumps(document, indent=2)}")
                return True
            else:
                print("❌ Document not found in query results")
                return False
        else:
            print(f"❌ Verification failed. Status: {response.status_code}")
            print(f"📋 Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Verification error: {str(e)}")
        return False

def print_integration_test_instructions(doc_id):
    """Print instructions for running the integration test with the seeded document."""
    print("\n" + "="*60)
    print("📱 INTEGRATION TEST INSTRUCTIONS")
    print("="*60)
    print(f"1. Test document created with ID: {doc_id}")
    print(f"2. Set the test document ID as a system property:")
    print(f"   export GITHUB_TEST_DOC_ID={doc_id}")
    print()
    print("3. Run the Android integration test:")
    print("   cd android-kotlin/QuickStartTasks")
    print("   ./gradlew connectedDebugAndroidTest")
    print()
    print("4. Or run a specific test:")
    print("   ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=live.ditto.quickstart.tasks.TasksSyncIntegrationTest#testGitHubDocumentSyncFromCloud")
    print()
    print("5. The test will:")
    print("   - Launch the Android app")
    print("   - Wait for Ditto to sync")
    print(f"   - Look for your test document: '{doc_id}'")
    print("   - Verify it appears in the UI")
    print()
    print("💡 Make sure you have:")
    print("   - Android device/emulator connected")
    print("   - App installed and running")
    print("   - Ditto sync enabled in the app")
    print("="*60)

def main():
    parser = argparse.ArgumentParser(description='Seed test document for Android Kotlin integration tests')
    parser.add_argument('--doc-id', help='Custom document ID (auto-generated if not provided)')
    parser.add_argument('--title', help='Custom document title (auto-generated if not provided)')
    parser.add_argument('--verify', action='store_true', help='Verify document creation by querying it back')
    parser.add_argument('--env-file', default='.env', help='Path to .env file (default: .env)')
    
    args = parser.parse_args()
    
    print("🌱 Ditto Android Kotlin - Local Test Document Seeder")
    print("="*50)
    
    # Load environment variables
    env_vars = load_env_file(args.env_file)
    if not env_vars:
        sys.exit(1)
    
    print(f"✅ Loaded environment variables for App ID: {env_vars['DITTO_APP_ID']}")
    
    # Create test document
    doc_id = create_test_document(env_vars, args.doc_id, args.title)
    if not doc_id:
        print("\n❌ Failed to create test document")
        sys.exit(1)
    
    # Verify document creation if requested
    if args.verify:
        print("\n" + "-"*30)
        if not verify_document_creation(env_vars, doc_id):
            print("⚠️  Document creation couldn't be verified")
    
    # Print instructions for running integration tests
    print_integration_test_instructions(doc_id)
    
    print(f"\n🎉 Test document seeding completed!")
    print(f"Document ID: {doc_id}")

if __name__ == "__main__":
    main()