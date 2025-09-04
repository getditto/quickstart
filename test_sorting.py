#!/usr/bin/env python3

import requests
import json
import os

# Load environment variables from .env file
def load_env():
    env_vars = {}
    if os.path.exists('.env'):
        with open('.env', 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    env_vars[key] = value
    return env_vars

def add_task_via_api(title, api_key, api_url):
    """Add a task using Ditto HTTP API"""
    headers = {
        'Content-type': 'application/json',
        'Authorization': f'Bearer {api_key}'
    }
    
    payload = {
        "statement": "INSERT INTO tasks DOCUMENTS (:newTask) ON ID CONFLICT DO UPDATE",
        "args": {
            "newTask": {
                "_id": f"task_{title.lower().replace(' ', '_').replace('_', '')}_{hash(title) % 1000}",
                "title": title,
                "done": False,
                "deleted": False
            }
        }
    }
    
    response = requests.post(
        f"https://{api_url}/api/v4/store/execute",
        headers=headers,
        json=payload
    )
    
    return response.status_code, response.text

def main():
    print("🔧 Adding test tasks to demonstrate alphabetical sorting...")
    
    # Load environment
    env_vars = load_env()
    api_key = env_vars.get('DITTO_API_KEY', os.getenv('DITTO_API_KEY'))
    api_url = env_vars.get('DITTO_API_URL', os.getenv('DITTO_API_URL'))
    
    if not api_key or not api_url:
        print("❌ Missing DITTO_API_KEY or DITTO_API_URL in .env file")
        return
    
    # Test tasks to demonstrate sorting
    test_tasks = [
        "000_Priority Task (should be first)",
        "001_Another priority task",
        "Zebra task (should be last)",
        "Apple task (should be alphabetical)",
        "Buy groceries",
        "Call dentist", 
        "Draft presentation",
        "Exercise daily",
        "Fix bugs",
        "002_Third priority task"
    ]
    
    print(f"📤 Adding {len(test_tasks)} test tasks...")
    
    for task_title in test_tasks:
        status_code, response = add_task_via_api(task_title, api_key, api_url)
        if status_code in [200, 201]:
            print(f"✅ Added: '{task_title}'")
        else:
            print(f"❌ Failed to add '{task_title}': {status_code} - {response}")
    
    print("\n🎯 Expected sorting order in the app:")
    print("1. 000_Priority Task (should be first)")
    print("2. 001_Another priority task") 
    print("3. 002_Third priority task")
    print("4. Apple task (should be alphabetical)")
    print("5. Buy groceries")
    print("6. Call dentist")
    print("7. Draft presentation")
    print("8. Exercise daily")
    print("9. Fix bugs")
    print("10. Zebra task (should be last)")
    
    print(f"\n📱 Check the KMP app - tasks should now be sorted alphabetically with 000_, 001_, 002_ at the top!")

if __name__ == "__main__":
    main()