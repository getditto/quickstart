#!/usr/bin/env python3
"""
Debug BrowserStack test - single Chrome session with live viewing
"""

import time
import os
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options as ChromeOptions

def run_debug_test():
    """Run single Chrome test for debugging with live session."""
    print("🚀 Starting BrowserStack Chrome debug session...")
    
    # Set up BrowserStack options for debugging
    bs_options = {
        "browserVersion": "120.0",
        "os": "Windows",
        "osVersion": "11",
        "sessionName": "Ditto Tasks Debug - Live Chrome Session",
        "buildName": f"Debug Build - {int(time.time())}",
        "projectName": "Ditto JavaScript Web Debug",
        "local": "true",
        "debug": "true",
        "video": "true",
        "networkLogs": "true",
        "consoleLogs": "info",
        "resolution": "1920x1080",
        "seleniumVersion": "4.0.0",
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

        session_id = driver.session_id
        print(f"✅ BrowserStack session created!")
        print(f"🆔 Session ID: {session_id}")
        print(f"🌐 Live Session URL: https://automate.browserstack.com/dashboard/v2/builds/{bs_options['buildName']}")
        print(f"📺 You can watch the session live at: https://automate.browserstack.com/")
        
        # Navigate to the application
        print("📍 Navigating to http://localhost:3000...")
        driver.get("http://localhost:3000")
        
        # Wait for page to load
        print("⏳ Waiting for page to load...")
        WebDriverWait(driver, 30).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        
        print("✅ Page loaded successfully!")
        print(f"📄 Page title: {driver.title}")
        print(f"🔗 Current URL: {driver.current_url}")
        
        # Take a screenshot
        driver.save_screenshot("debug_screenshot.png")
        print("📸 Screenshot saved as debug_screenshot.png")
        
        # Wait for user input to continue
        input("\n🎮 Chrome is now running on BrowserStack! Press ENTER to continue with tests or Ctrl+C to exit and keep session open...")
        
        # Look for key UI elements
        print("🔍 Looking for task input field...")
        try:
            task_input = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, "input[placeholder*='What needs to be done']"))
            )
            print(f"✅ Task input found: {task_input.get_attribute('placeholder')}")
            
            # Check if input is enabled
            if task_input.get_attribute("disabled"):
                print("⚠️  Task input is DISABLED - this might be the issue!")
            else:
                print("✅ Task input is enabled and ready")
                
        except Exception as e:
            print(f"❌ Task input not found: {e}")
            
        # Look for Add Task button
        print("🔍 Looking for Add Task button...")
        try:
            add_button = driver.find_element(By.XPATH, "//button[contains(text(), 'Add Task')]")
            print("✅ Add Task button found")
        except Exception as e:
            print(f"❌ Add Task button not found: {e}")
            
        # Check page source for any errors
        page_source = driver.page_source
        if "error" in page_source.lower() or "exception" in page_source.lower():
            print("⚠️  Found errors in page source:")
            # Show first 500 chars of page source
            print(page_source[:500] + "...")
        else:
            print("✅ No obvious errors in page source")
            
        # Try to add a test task
        input("\n🧪 Press ENTER to test adding a task (or Ctrl+C to skip)...")
        
        try:
            test_task = f"BrowserStack Debug Task - {int(time.time())}"
            task_input.clear()
            task_input.send_keys(test_task)
            add_button.click()
            
            print(f"⏳ Waiting for task '{test_task}' to appear...")
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.XPATH, f"//*[contains(text(), '{test_task}')]"))
            )
            print("✅ Task added successfully!")
            
        except Exception as e:
            print(f"❌ Failed to add task: {e}")
            
        # Keep session alive for manual inspection
        input("\n👀 Session is live! Go to https://automate.browserstack.com/ to watch. Press ENTER when done...")
        
        print("✅ Debug session completed successfully!")
        return True

    except Exception as e:
        print(f"❌ Debug test failed: {str(e)}")
        if driver:
            try:
                driver.save_screenshot("error_debug_screenshot.png")
                print("📸 Error screenshot saved")
            except:
                pass
        return False

    finally:
        if driver:
            input("\n🛑 Press ENTER to close the browser session (or Ctrl+C to keep it open longer)...")
            driver.quit()
            print("🔚 Browser session closed")

if __name__ == "__main__":
    run_debug_test()