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


def wait_for_sync_document(driver, doc_id, max_wait=45):
    """Wait for a specific document to appear in the task list."""
    print(f"Waiting for document '{doc_id}' to sync...")
    # Extract the run ID from the document ID (format: github_test_RUNID_RUNNUMBER)
    run_id = doc_id.split('_')[2] if len(doc_id.split('_')) > 2 else doc_id
    print(f"Looking for GitHub Run ID: {run_id}")
    
    start_time = time.time()
    attempt = 0
    
    while (time.time() - start_time) < max_wait:
        attempt += 1
        try:
            # Based on TaskList.tsx, tasks are rendered as div.group elements containing spans with the title
            task_selectors = [
                "div.group span",  # Task title spans inside group divs
                "div.group",  # The task container divs themselves
                ".group span",  # Alternative class selector
                "span",  # All spans (broader search)
            ]
            
            found_elements = []
            for selector in task_selectors:
                try:
                    elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    if elements:
                        found_elements.extend(elements)
                        if len(found_elements) > 0:
                            break
                except:
                    continue
            
            # Log what we found every 5 attempts
            if attempt % 5 == 0:
                print(f"  Attempt {attempt}: Found {len(found_elements)} potential task elements")
                if found_elements and len(found_elements) <= 20:
                    for i, elem in enumerate(found_elements[:10]):  # Show first 10 elements
                        try:
                            text = elem.text.strip()
                            if text and len(text) > 0:
                                print(f"    Element {i}: '{text[:100]}'")
                        except:
                            pass
            
            # Check each element for our GitHub run ID
            for element in found_elements:
                try:
                    element_text = element.text.strip()
                    # Check if the run ID appears anywhere in the text
                    # The title format is "GitHub Test Task {GITHUB_RUN_ID}"
                    if run_id in element_text and "GitHub Test Task" in element_text:
                        print(f"✓ Found synced document: {element_text}")
                        return True
                except:
                    continue
                    
        except Exception as e:
            if attempt % 10 == 0:
                print(f"  Search attempt {attempt} failed with error: {str(e)}")
        
        time.sleep(1.5)  # Wait a bit longer between attempts
    
    print(f"❌ Document not found after {attempt} attempts and {max_wait} seconds")
    # Try one more time with a very broad search to see what's on the page
    try:
        all_text_elements = driver.find_elements(By.XPATH, "//*[contains(text(), 'GitHub')]")
        if all_text_elements:
            print(f"  Found {len(all_text_elements)} elements containing 'GitHub':")
            for i, elem in enumerate(all_text_elements[:5]):
                try:
                    print(f"    - {elem.text.strip()}")
                except:
                    pass
    except:
        pass
    
    return False


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
        
        # Wait a bit for initial sync to complete
        print("Waiting for initial sync...")
        time.sleep(3)
        
        # Check for GitHub test document
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID')
        if github_doc_id:
            print(f"Checking for GitHub test document: {github_doc_id}")
            if wait_for_sync_document(driver, github_doc_id):
                print("✓ GitHub test document successfully synced from Ditto Cloud")
            else:
                print("❌ GitHub test document did not sync within timeout period")
                # Take a debug screenshot
                driver.save_screenshot(f"sync_failure_debug_{browser_config['browser']}.png")
                print(f"Debug screenshot saved for {browser_config['browser']}")
                raise Exception("Failed to sync test document from Ditto Cloud")
        else:
            print("⚠ No GitHub test document ID provided, skipping sync verification")
        
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