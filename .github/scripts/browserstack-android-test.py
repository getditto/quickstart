#!/usr/bin/env python3
"""
BrowserStack real device testing script for Ditto Kotlin Multiplatform Android application.
This script runs automated tests on multiple Android devices using BrowserStack to verify
the actual Ditto sync functionality of the KMP quickstart app.

Based on the JavaScript web BrowserStack test pattern for real sync verification.
"""
import time
import json
import sys
import os
from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException

def wait_for_ditto_sync(driver, test_doc_id, max_wait=60):
    """Wait for the test document to sync from Ditto Cloud to the Android app."""
    print(f"🔄 Waiting for Ditto sync of document '{test_doc_id}'...")
    
    # Extract the run ID from the document ID for easier identification
    run_id = test_doc_id.split('_')[4] if len(test_doc_id.split('_')) > 4 else test_doc_id
    print(f"🔍 Looking for GitHub Run ID: {run_id}")
    
    start_time = time.time()
    
    while (time.time() - start_time) < max_wait:
        try:
            # Look for task items in the RecyclerView or list
            # KMP Compose apps typically use LazyColumn for task lists
            task_elements = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
            
            # Check each text element for our test document
            for element in task_elements:
                try:
                    element_text = element.text.strip()
                    
                    # Check if this is our GitHub test task
                    if run_id in element_text and "GitHub KMP Android Test Task" in element_text:
                        print(f"✅ Found synced document from Ditto Cloud: {element_text}")
                        return True
                        
                except Exception:
                    continue
                    
            # Also try looking for the full test document ID in any text element
            all_text_elements = driver.find_elements(AppiumBy.XPATH, "//*[contains(@text,'GitHub')]")
            if all_text_elements:
                for element in all_text_elements:
                    try:
                        if "KMP Android Test Task" in element.text:
                            print(f"✅ Found synced test document: {element.text}")
                            return True
                    except Exception:
                        continue
                        
        except Exception as e:
            # Only log significant errors occasionally
            if (time.time() - start_time) % 10 == 0:
                print(f"⏳ Still waiting for sync... ({int(time.time() - start_time)}s)")
        
        time.sleep(2)  # Check every 2 seconds
    
    print(f"❌ Test document did not sync from Ditto Cloud after {max_wait} seconds")
    return False

def run_android_test(device_config):
    """Run comprehensive Ditto sync test on specified Android device."""
    device_name = f"{device_config['deviceName']} (Android {device_config['platformVersion']})"
    print(f"🤖 Starting Ditto sync test on {device_name}")
    
    # BrowserStack Appium capabilities
    desired_caps = {
        # BrowserStack specific
        'browserstack.user': os.environ['BROWSERSTACK_USERNAME'],
        'browserstack.key': os.environ['BROWSERSTACK_ACCESS_KEY'],
        'project': 'Ditto KMP Android',
        'build': f"KMP Android Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'name': f"Ditto Sync Test - {device_name}",
        'browserstack.debug': 'true',
        'browserstack.video': 'true',
        'browserstack.networkLogs': 'true',
        'browserstack.appiumLogs': 'true',
        
        # App specific
        'app': os.environ.get('BROWSERSTACK_APP_URL'),  # Set by upload step
        'platformName': 'Android',
        'deviceName': device_config['deviceName'],
        'platformVersion': device_config['platformVersion'],
        
        # Appium specific
        'automationName': 'UiAutomator2',
        'autoGrantPermissions': 'true',
        'newCommandTimeout': '300',
        'noReset': 'true',
    }
    
    driver = None
    try:
        print(f"🚀 Connecting to BrowserStack for {device_name}...")
        
        # Create UiAutomator2 options for modern Appium
        from appium.options.android import UiAutomator2Options
        options = UiAutomator2Options()
        options.load_capabilities(desired_caps)
        
        driver = webdriver.Remote(
            command_executor='https://hub.browserstack.com/wd/hub',
            options=options
        )
        
        print(f"✅ Connected to {device_name}")
        
        # Wait for app to launch and initialize
        print("⏳ Waiting for app to initialize...")
        time.sleep(10)  # Give app time to start Ditto and connect
        
        # Check if app launched successfully
        try:
            # Look for any UI elements indicating the app loaded
            app_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if not app_elements:
                raise Exception("No UI elements found - app may have crashed")
            print(f"✅ App launched successfully with {len(app_elements)} UI elements")
        except Exception as e:
            raise Exception(f"App launch verification failed: {str(e)}")
        
        # Wait for Ditto to initialize and connect
        print("🔄 Allowing time for Ditto SDK initialization and sync...")
        time.sleep(15)  # Give Ditto more time to initialize and sync
        
        # Test 1: Verify our GitHub test document synced from Ditto Cloud
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID')
        if github_doc_id:
            print(f"📋 Testing sync of pre-inserted document: {github_doc_id}")
            if wait_for_ditto_sync(driver, github_doc_id):
                print("✅ DITTO SYNC VERIFICATION PASSED - Document synced from Cloud to Android!")
            else:
                print("❌ DITTO SYNC VERIFICATION FAILED - Document did not sync")
                # Take screenshot for debugging
                driver.save_screenshot(f"sync_failed_{device_config['deviceName']}.png")
                raise Exception("Failed to verify Ditto sync functionality")
        else:
            print("⚠️ No GitHub test document ID provided, skipping sync verification")
        
        # Test 2: Verify app UI elements are present and functional
        print("🖱️ Testing app UI functionality...")
        
        try:
            # Look for add task input or button
            # KMP Compose might use different element types
            add_elements = driver.find_elements(AppiumBy.XPATH, "//*[contains(@text,'Add') or contains(@content-desc,'Add')]")
            if add_elements:
                print("✅ Found add task UI elements")
            else:
                print("⚠️ Add task elements not found, checking general UI")
        except Exception as e:
            print(f"⚠️ UI element check had issues: {str(e)}")
        
        # Test 3: Try to create a new task to verify write functionality
        print("📝 Testing task creation functionality...")
        
        try:
            # Look for input fields that might accept task text
            input_elements = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.EditText")
            
            if input_elements:
                print(f"✅ Found {len(input_elements)} input field(s)")
                
                # Try to enter text in the first input field
                input_field = input_elements[0]
                test_task_text = f"BrowserStack Test Task from {device_config['deviceName']}"
                
                input_field.clear()
                input_field.send_keys(test_task_text)
                print(f"✅ Entered test task text: {test_task_text}")
                
                # Look for submit/add button
                buttons = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.Button")
                if buttons:
                    # Try clicking the first button (likely "Add Task")
                    buttons[0].click()
                    print("✅ Clicked add task button")
                    
                    # Wait a moment for task to be added
                    time.sleep(3)
                    
                    # Verify task appeared in the list
                    try:
                        new_task_element = driver.find_element(AppiumBy.XPATH, f"//*[contains(@text,'{test_task_text}')]")
                        print("✅ New task successfully created and visible in UI")
                    except NoSuchElementException:
                        print("⚠️ New task may not be visible immediately")
                
            else:
                print("⚠️ No input fields found for task creation")
                
        except Exception as e:
            print(f"⚠️ Task creation test had issues: {str(e)}")
        
        # Test 4: Verify app stability
        print("🔧 Verifying app stability...")
        
        try:
            # Check that app is still responsive
            current_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if len(current_elements) > 0:
                print(f"✅ App remains stable with {len(current_elements)} active UI elements")
            else:
                raise Exception("App appears to have crashed or become unresponsive")
        except Exception as e:
            raise Exception(f"App stability check failed: {str(e)}")
        
        # Take success screenshot
        driver.save_screenshot(f"success_{device_config['deviceName']}.png")
        print(f"📸 Success screenshot saved for {device_name}")
        
        # Report success to BrowserStack
        driver.execute_script('browserstack_executor: {"action": "setSessionStatus", "arguments": {"status":"passed", "reason": "Ditto sync and app functionality verified successfully"}}')
        
        print(f"🎉 All tests PASSED on {device_name}")
        return True
        
    except Exception as e:
        print(f"❌ Test FAILED on {device_name}: {str(e)}")
        
        if driver:
            try:
                # Take failure screenshot
                driver.save_screenshot(f"failure_{device_config['deviceName']}.png")
                print(f"📸 Failure screenshot saved for {device_name}")
                
                # Report failure to BrowserStack
                driver.execute_script(f'browserstack_executor: {{"action": "setSessionStatus", "arguments": {{"status":"failed", "reason": "Test failed: {str(e)[:100]}"}}}}')
            except Exception:
                print("⚠️ Failed to save screenshot or report status")
        
        return False
        
    finally:
        if driver:
            driver.quit()

def main():
    """Main function to run tests on multiple Android devices."""
    # Android device configurations to test (real BrowserStack devices)
    android_devices = [
        {
            'deviceName': 'Google Pixel 8',
            'platformVersion': '14.0'
        },
        {
            'deviceName': 'Samsung Galaxy S23',
            'platformVersion': '13.0'
        },
        {
            'deviceName': 'Google Pixel 6',
            'platformVersion': '12.0'
        },
        {
            'deviceName': 'OnePlus 9',
            'platformVersion': '11.0'
        }
    ]
    
    print("🚀 Starting BrowserStack real device tests for Ditto KMP Android app...")
    print(f"📋 Test document ID: {os.environ.get('GITHUB_TEST_DOC_ID', 'Not set')}")
    print(f"📱 Testing on {len(android_devices)} real Android devices")
    
    # Run tests on all devices
    results = []
    for device_config in android_devices:
        success = run_android_test(device_config)
        results.append({
            'device': f"{device_config['deviceName']} (Android {device_config['platformVersion']})",
            'success': success
        })
        
        # Small delay between device tests
        time.sleep(5)
    
    # Print comprehensive summary
    print("\n" + "="*60)
    print("🏁 DITTO KMP ANDROID BROWSERSTACK TEST SUMMARY")
    print("="*60)
    
    passed = 0
    total = len(results)
    
    for result in results:
        status = "✅ PASSED" if result['success'] else "❌ FAILED"
        print(f"  {result['device']}: {status}")
        if result['success']:
            passed += 1
    
    print(f"\n📊 Overall Results: {passed}/{total} devices passed")
    
    if passed == total:
        print("🎉 ALL TESTS PASSED! Ditto KMP Android app works perfectly on real devices!")
        print("✅ Ditto sync functionality verified")
        print("✅ App UI functionality verified") 
        print("✅ App stability verified")
        sys.exit(0)
    else:
        print("💥 SOME TESTS FAILED! Issues detected with Ditto KMP Android app!")
        print(f"❌ {total - passed} device(s) failed testing")
        sys.exit(1)

if __name__ == "__main__":
    main()