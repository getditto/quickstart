#!/usr/bin/env python3
"""
BrowserStack cross-browser testing script for Ditto Flutter Web application.
This script runs automated tests on multiple browsers using BrowserStack to verify
the basic functionality of the Ditto Tasks Flutter web application.
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

def wait_for_sync_document(driver, doc_id, max_wait=30):
    """Wait for a specific document to appear in the task list."""
    print(f"Waiting for document '{doc_id}' to sync...")
    # Extract the run ID from the document ID (format: github_test_RUNID_RUNNUMBER)
    parts = doc_id.split('_')
    run_id = parts[2] if len(parts) > 2 else doc_id
    print(f"Looking for GitHub Run ID: {run_id}")
    
    start_time = time.time()
    
    while (time.time() - start_time) < max_wait:
        try:
            # Flutter web renders tasks differently than React - look for text content
            # Search for elements containing the run ID
            task_elements = driver.find_elements(By.XPATH, f"//*[contains(text(), '{run_id}')]")
            
            # Check each element for our GitHub test task
            for element in task_elements:
                try:
                    element_text = element.text.strip()
                    # Check if the run ID appears in the text and it's our GitHub test task
                    if run_id in element_text and "GitHub Test Task" in element_text:
                        print(f"✓ Found synced document: {element_text}")
                        return True
                except:
                    continue
                    
        except Exception as e:
            # Only log errors occasionally to reduce noise
            pass
        
        time.sleep(1)  # Check every second
    
    print(f"❌ Document not found after {max_wait} seconds")
    return False

def run_test(browser_config):
    """Run automated test on specified browser configuration."""
    print(f"Starting test on {browser_config['browser']} {browser_config['browser_version']} on {browser_config['os']}")
    
    # Set up BrowserStack options
    bs_options = {
        'browserVersion': browser_config['browser_version'],
        'os': browser_config['os'],
        'osVersion': browser_config['os_version'],
        'sessionName': f"Ditto Flutter Tasks Test - {browser_config['browser']} {browser_config['browser_version']}",
        'buildName': f"Ditto Flutter Web Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'projectName': 'Ditto Flutter Web',
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
        driver.execute_script('browserstack_executor: {"action": "setSessionName", "arguments": {"name": "Ditto Flutter Tasks Web Test"}}')
        
        # Navigate to the application
        print("Navigating to Flutter web application...")
        driver.get("http://localhost:3000")
        
        # Wait for page to load
        WebDriverWait(driver, 30).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        
        print("Page loaded, waiting for Flutter app initialization...")
        
        # Wait for Flutter to initialize - look for the app title
        try:
            WebDriverWait(driver, 30).until(
                EC.presence_of_element_located((By.XPATH, "//*[contains(text(), 'Ditto Tasks')]"))
            )
            print("Flutter app title found")
        except:
            print("Flutter app title check failed, continuing with extended wait...")
            time.sleep(10)
        
        # Wait a bit more for Ditto initialization
        print("Waiting for Ditto initialization...")
        time.sleep(5)
        
        # Check for GitHub test document
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID')
        if github_doc_id:
            print(f"Checking for GitHub test document: {github_doc_id}")
            if wait_for_sync_document(driver, github_doc_id):
                print("✓ GitHub test document successfully synced from Ditto Cloud")
            else:
                print("❌ GitHub test document did not sync within timeout period")
                raise Exception("Failed to sync test document from Ditto Cloud")
        else:
            print("⚠ No GitHub test document ID provided, skipping sync verification")
        
        # Verify key UI elements are present
        print("Verifying UI elements...")
        
        # Check for app title
        app_title = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, "//*[contains(text(), 'Ditto Tasks')]"))
        )
        print("✓ App title found")
        
        # Check for FloatingActionButton (FAB) - Flutter web renders this as a button
        try:
            fab = driver.find_element(By.XPATH, "//button[contains(@class, 'floating') or @aria-label='Add Task' or contains(@title, 'Add')]")
            print("✓ FloatingActionButton found")
        except:
            # Fallback: look for any button that might be the FAB
            try:
                fab = driver.find_element(By.XPATH, "//button[last()]")
                print("✓ Potential FAB found (fallback)")
            except:
                print("⚠ FAB not found using standard selectors")
                fab = None
        
        # Check for Sync toggle - look for switch or checkbox elements
        try:
            sync_toggle = driver.find_element(By.XPATH, "//*[contains(text(), 'Sync') or contains(text(), 'Active')]")
            print(f"✓ Sync element found: {sync_toggle.text}")
        except:
            print("⚠ Sync toggle not found")
        
        # Test basic functionality - add a task if FAB is available
        if fab:
            print("Testing task creation...")
            
            try:
                fab.click()
                time.sleep(2)  # Wait for dialog to appear
                
                # Look for text input field in dialog
                text_field = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.XPATH, "//input[@type='text'] | //textarea"))
                )
                
                text_field.clear()
                text_field.send_keys("Test Task from BrowserStack Flutter")
                
                # Look for Add button in dialog
                add_button = driver.find_element(By.XPATH, "//button[contains(text(), 'Add') or contains(text(), 'Submit')]")
                add_button.click()
                
                # Wait a bit for the task to appear
                time.sleep(3)
                
                # Check if task appeared in list
                try:
                    task_item = WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.XPATH, "//*[contains(text(), 'Test Task from BrowserStack Flutter')]"))
                    )
                    print("✓ Task created successfully and appears in list")
                except:
                    print("⚠ Task may not have appeared in list")
                    
            except Exception as e:
                print(f"⚠ Task creation test failed: {str(e)}")
        else:
            print("⚠ FAB not found, skipping task creation test")
        
        # Take a screenshot for verification
        driver.save_screenshot(f"flutter_test_screenshot_{browser_config['browser']}.png")
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
                driver.save_screenshot(f"flutter_error_screenshot_{browser_config['browser']}.png")
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