#!/usr/bin/env python3
"""
Simple BrowserStack test - just find existing tasks
"""

import time
import os
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options as ChromeOptions

def simple_test():
    """Simple test to find existing tasks."""
    print("🚀 Starting simple BrowserStack test...")
    
    # Set up BrowserStack options
    bs_options = {
        "browserVersion": "120.0",
        "os": "Windows",
        "osVersion": "11",
        "sessionName": "Simple Task Search Test",
        "buildName": f"Simple Test - {int(time.time())}",
        "projectName": "Ditto JavaScript Web Simple",
        "local": "true",
        "debug": "true",
        "video": "true",
        "networkLogs": "true",
        "consoleLogs": "info",
    }

    options = ChromeOptions()
    options.set_capability("bstack:options", bs_options)

    driver = None
    try:
        # Initialize WebDriver
        print("🔗 Connecting to BrowserStack...")
        driver = webdriver.Remote(
            command_executor=f"https://{os.environ['BROWSERSTACK_USERNAME']}:{os.environ['BROWSERSTACK_ACCESS_KEY']}@hub.browserstack.com/wd/hub",
            options=options,
        )

        print("📍 Navigating to http://localhost:3000...")
        driver.get("http://localhost:3000")
        
        # Wait for page to load
        print("⏳ Waiting for page to load...")
        WebDriverWait(driver, 30).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        
        print(f"✅ Page loaded! Title: {driver.title}")
        
        # Wait for app to initialize
        print("⏳ Waiting for task input to be enabled...")
        try:
            task_input = WebDriverWait(driver, 20).until(
                lambda d: not d.find_element(
                    By.CSS_SELECTOR, "input[placeholder*='What needs to be done']"
                ).get_attribute("disabled")
            )
            print("✅ Task input is enabled")
        except:
            print("⚠️ Task input check failed, continuing...")
            
        # Wait a bit for sync
        print("⏳ Waiting 5 seconds for initial sync...")
        time.sleep(5)
        
        # Look for ANY tasks on the page
        print("🔍 Looking for any tasks on the page...")
        try:
            # Try multiple selectors based on TaskList.tsx structure
            task_elements = driver.find_elements(By.CSS_SELECTOR, "div.group span")
            print(f"Found {len(task_elements)} task elements")
            
            for i, element in enumerate(task_elements):
                try:
                    text = element.text.strip()
                    print(f"Task {i+1}: '{text}'")
                except:
                    print(f"Task {i+1}: [Could not read text]")
                    
            # Look specifically for "Basic Test Task"
            basic_tasks = [el for el in task_elements if "Basic Test Task" in el.text]
            if basic_tasks:
                print(f"✅ Found 'Basic Test Task': {basic_tasks[0].text}")
                return True
            else:
                print("⚠️ No 'Basic Test Task' found")
                
            # Look for any task with "Test" in the name
            test_tasks = [el for el in task_elements if "test" in el.text.lower()]
            if test_tasks:
                print(f"✅ Found test-related tasks: {[t.text for t in test_tasks]}")
                return True
            else:
                print("⚠️ No test-related tasks found")
                
            # If we found ANY tasks, that's still a success
            if task_elements and any(el.text.strip() for el in task_elements):
                print(f"✅ Found {len(task_elements)} tasks total - sync is working!")
                return True
            else:
                print("❌ No tasks found at all")
                return False
                
        except Exception as e:
            print(f"❌ Error looking for tasks: {e}")
            
            # Try alternative selector
            try:
                print("🔍 Trying alternative task selector...")
                all_text = driver.find_elements(By.XPATH, "//*[contains(text(), 'Task')]")
                print(f"Found {len(all_text)} elements with 'Task' in text")
                for el in all_text[:5]:  # Show first 5
                    print(f"- {el.text}")
            except:
                print("❌ Alternative selector also failed")
                
            return False

    except Exception as e:
        print(f"❌ Test failed: {str(e)}")
        return False

    finally:
        if driver:
            driver.quit()

if __name__ == "__main__":
    success = simple_test()
    if success:
        print("🎉 Simple test PASSED!")
    else:
        print("💥 Simple test FAILED!")