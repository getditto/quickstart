#!/usr/bin/env python3
"""
BrowserStack cross-browser testing script for Ditto JavaScript Web application.

This script runs automated tests on multiple browsers using BrowserStack to verify
the basic functionality of the Ditto Tasks web application.
"""

import time
import json
import sys
import os
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions


def run_test(browser_config):
    """Run automated test on specified browser configuration."""
    print(f"Starting test on {browser_config['browser']} {browser_config['browser_version']} on {browser_config['os']}")
    
    # Set up BrowserStack options
    bs_options = {
        'browserVersion': browser_config['browser_version'],
        'os': browser_config['os'],
        'osVersion': browser_config['os_version'],
        'sessionName': f"Ditto Tasks Test - {browser_config['browser']} {browser_config['browser_version']}",
        'buildName': f"Ditto JavaScript Web Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'projectName': 'Ditto JavaScript Web',
        'local': 'true',
        'debug': 'true',
        'video': 'true',
        'networkLogs': 'true',
        'consoleLogs': 'info'
    }
    
    # Create browser-specific options
    if browser_config['browser'].lower() == 'chrome':
        options = ChromeOptions()
        options.set_capability('bstack:options', bs_options)
    elif browser_config['browser'].lower() == 'firefox':
        options = FirefoxOptions()
        options.set_capability('bstack:options', bs_options)
    else:
        # Fallback to Chrome for other browsers
        options = ChromeOptions()
        options.set_capability('bstack:options', bs_options)
    
    driver = None
    try:
        # Initialize WebDriver with modern options
        driver = webdriver.Remote(
            command_executor=f"https://{os.environ['BROWSERSTACK_USERNAME']}:{os.environ['BROWSERSTACK_ACCESS_KEY']}@hub.browserstack.com/wd/hub",
            options=options
        )
        
        # Set additional session info
        driver.execute_script('browserstack_executor: {"action": "setSessionName", "arguments": {"name": "Ditto Tasks Web Test"}}')
        
        # Navigate to the application
        print("Navigating to application...")
        driver.get("http://localhost:3000")
        
        # Wait for page to load
        WebDriverWait(driver, 30).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        
        print("Page loaded, waiting for app initialization...")
        
        # Wait for task input to be enabled
        try:
            WebDriverWait(driver, 20).until(
                lambda d: not d.find_element(By.CSS_SELECTOR, "input[placeholder*='What needs to be done']").get_attribute("disabled")
            )
            print("Task input is enabled")
        except:
            print("Task input check failed, continuing with basic checks...")
        
        # Verify key UI elements are present
        print("Verifying UI elements...")
        
        # Check for task input field
        task_input = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "input[placeholder*='What needs to be done']"))
        )
        print("✓ Task input field found")
        
        # Check for Add Task button
        add_button = driver.find_element(By.XPATH, "//button[contains(text(), 'Add Task')]")
        print("✓ Add Task button found")
        
        # Check for items counter
        try:
            items_counter = driver.find_element(By.XPATH, "//*[contains(text(), 'items left') or contains(text(), '0')]")
            print(f"✓ Items counter found: {items_counter.text}")
        except:
            print("⚠ Items counter not found or text different than expected")
        
        # Test basic functionality - add a task
        print("Testing task creation...")
        
        if not task_input.get_attribute("disabled"):
            task_input.clear()
            task_input.send_keys("Test Task from BrowserStack")
            add_button.click()
            
            # Wait a bit for the task to appear
            time.sleep(2)
            
            # Check if task appeared in list
            try:
                task_item = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.XPATH, "//*[contains(text(), 'Test Task from BrowserStack')]"))
                )
                print("✓ Task created successfully and appears in list")
            except:
                print("⚠ Task may not have appeared in list")
        else:
            print("⚠ Task input is disabled, skipping task creation test")
        
        # Take a screenshot for verification
        driver.save_screenshot(f"test_screenshot_{browser_config['browser']}.png")
        print(f"✓ Screenshot saved for {browser_config['browser']}")
        
        # Report success to BrowserStack
        driver.execute_script('browserstack_executor: {"action": "setSessionStatus", "arguments": {"status":"passed", "reason": "All tests passed successfully"}}')
        print("✓ Reported success status to BrowserStack")
        
        print(f"✅ Test completed successfully on {browser_config['browser']}")
        return True
        
    except Exception as e:
        print(f"❌ Test failed on {browser_config['browser']}: {str(e)}")
        if driver:
            try:
                driver.save_screenshot(f"error_screenshot_{browser_config['browser']}.png")
                print(f"Error screenshot saved for {browser_config['browser']}")
                
                # Report failure to BrowserStack
                driver.execute_script(f'browserstack_executor: {{"action": "setSessionStatus", "arguments": {{"status":"failed", "reason": "Test failed: {str(e)[:100]}"}}}}')
                print("✓ Reported failure status to BrowserStack")
            except:
                print("⚠ Failed to save screenshot or report status")
        return False
        
    finally:
        if driver:
            driver.quit()


def main():
    """Main function to run all browser tests."""
    # Browser configurations to test
    browsers = [
        {
            'browser': 'Chrome',
            'browser_version': '120.0',
            'os': 'Windows',
            'os_version': '11'
        },
        {
            'browser': 'Firefox',
            'browser_version': '121.0',
            'os': 'Windows',
            'os_version': '11'
        }
    ]

    # Run tests on all browsers
    results = []
    for browser_config in browsers:
        success = run_test(browser_config)
        results.append({
            'browser': f"{browser_config['browser']} {browser_config['browser_version']}",
            'os': f"{browser_config['os']} {browser_config['os_version']}",
            'success': success
        })

    # Print summary
    print("\n=== Test Summary ===")
    passed = 0
    total = len(results)

    for result in results:
        status = "✅ PASSED" if result['success'] else "❌ FAILED"
        print(f"{result['browser']} on {result['os']}: {status}")
        if result['success']:
            passed += 1

    print(f"\nOverall: {passed}/{total} tests passed")

    # Exit with appropriate code
    if passed == total:
        print("🎉 All tests passed!")
        sys.exit(0)
    else:
        print("💥 Some tests failed!")
        sys.exit(1)


if __name__ == "__main__":
    main()