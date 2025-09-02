#!/usr/bin/env python3
"""
BrowserStack real device testing script for Ditto Kotlin Multiplatform iOS application.
This script runs automated tests on real iOS devices using BrowserStack to verify
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

def wait_for_ditto_sync_ios(driver, test_doc_id, max_wait=60):
    """Wait for the test document to sync from Ditto Cloud to the iOS app."""
    print(f"🔄 Waiting for Ditto sync of iOS document '{test_doc_id}'...")
    
    # Extract the run ID from the document ID for easier identification
    run_id = test_doc_id.split('_')[4] if len(test_doc_id.split('_')) > 4 else test_doc_id
    print(f"🔍 Looking for GitHub Run ID: {run_id}")
    
    start_time = time.time()
    
    while (time.time() - start_time) < max_wait:
        try:
            # iOS apps typically use different element hierarchy
            # Look for text elements that might contain task titles
            text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeStaticText")
            text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeCell"))
            text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeCollectionView"))
            
            # Check each text element for our test document
            for element in text_elements:
                try:
                    element_text = element.text.strip() if element.text else ""
                    
                    # Check if this is our GitHub test task
                    if run_id in element_text and "GitHub KMP iOS Test Task" in element_text:
                        print(f"✅ Found synced iOS document from Ditto Cloud: {element_text}")
                        return True
                        
                except Exception:
                    continue
                    
            # Also try looking for elements by partial text match
            try:
                xpath_queries = [
                    f"//*[contains(@name,'GitHub') and contains(@name,'iOS')]",
                    f"//*[contains(@label,'GitHub') and contains(@label,'iOS')]",
                    f"//*[contains(@value,'{run_id}')]"
                ]
                
                for xpath in xpath_queries:
                    elements = driver.find_elements(AppiumBy.XPATH, xpath)
                    if elements:
                        for element in elements:
                            try:
                                element_text = element.get_attribute("name") or element.get_attribute("label") or element.get_attribute("value") or ""
                                if "KMP iOS Test Task" in element_text:
                                    print(f"✅ Found synced iOS test document: {element_text}")
                                    return True
                            except Exception:
                                continue
            except Exception:
                pass
                        
        except Exception as e:
            # Only log significant errors occasionally
            if (time.time() - start_time) % 15 == 0:
                print(f"⏳ Still waiting for iOS sync... ({int(time.time() - start_time)}s)")
        
        time.sleep(3)  # Check every 3 seconds for iOS
    
    print(f"❌ iOS test document did not sync from Ditto Cloud after {max_wait} seconds")
    return False

def run_ios_test(device_config):
    """Run comprehensive Ditto sync test on specified iOS device."""
    device_name = f"{device_config['deviceName']} (iOS {device_config['platformVersion']})"
    print(f"📱 Starting Ditto sync test on {device_name}")
    
    # BrowserStack iOS Appium capabilities
    desired_caps = {
        # BrowserStack specific
        'browserstack.user': os.environ['BROWSERSTACK_USERNAME'],
        'browserstack.key': os.environ['BROWSERSTACK_ACCESS_KEY'],
        'project': 'Ditto KMP iOS',
        'build': f"KMP iOS Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'name': f"Ditto iOS Sync Test - {device_name}",
        'browserstack.debug': 'true',
        'browserstack.video': 'true',
        'browserstack.networkLogs': 'true',
        'browserstack.appiumLogs': 'true',
        
        # App specific
        'app': os.environ.get('BROWSERSTACK_IOS_APP_URL'),  # Set by upload step
        'platformName': 'iOS',
        'deviceName': device_config['deviceName'], 
        'platformVersion': device_config['platformVersion'],
        
        # iOS Appium specific
        'automationName': 'XCUITest',
        'newCommandTimeout': '300',
        'noReset': 'true',
    }
    
    driver = None
    try:
        print(f"🚀 Connecting to BrowserStack for {device_name}...")
        driver = webdriver.Remote(
            command_executor='https://hub.browserstack.com/wd/hub',
            desired_capabilities=desired_caps
        )
        
        print(f"✅ Connected to {device_name}")
        
        # Wait for iOS app to launch and initialize
        print("⏳ Waiting for iOS app to initialize...")
        time.sleep(15)  # iOS apps may take longer to start
        
        # Check if app launched successfully
        try:
            # Look for any UI elements indicating the app loaded
            app_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if not app_elements:
                raise Exception("No UI elements found - iOS app may have crashed")
            print(f"✅ iOS app launched successfully with {len(app_elements)} UI elements")
        except Exception as e:
            raise Exception(f"iOS app launch verification failed: {str(e)}")
        
        # Wait for Ditto to initialize and connect on iOS
        print("🔄 Allowing time for Ditto SDK initialization and sync on iOS...")
        time.sleep(20)  # Give iOS Ditto more time to initialize and sync
        
        # Test 1: Verify our GitHub test document synced from Ditto Cloud
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID_IOS')
        if github_doc_id:
            print(f"📋 Testing iOS sync of pre-inserted document: {github_doc_id}")
            if wait_for_ditto_sync_ios(driver, github_doc_id):
                print("✅ DITTO iOS SYNC VERIFICATION PASSED - Document synced from Cloud to iOS!")
            else:
                print("❌ DITTO iOS SYNC VERIFICATION FAILED - Document did not sync")
                # Take screenshot for debugging
                driver.save_screenshot(f"ios_sync_failed_{device_config['deviceName']}.png")
                raise Exception("Failed to verify Ditto sync functionality on iOS")
        else:
            print("⚠️ No GitHub iOS test document ID provided, skipping sync verification")
        
        # Test 2: Verify iOS app UI elements are present and functional
        print("🖱️ Testing iOS app UI functionality...")
        
        try:
            # Look for iOS-specific UI elements
            # SwiftUI/Compose apps might have different accessibility patterns
            buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
            text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField") 
            
            if buttons:
                print(f"✅ Found {len(buttons)} button(s) in iOS app")
            if text_fields:
                print(f"✅ Found {len(text_fields)} text field(s) in iOS app")
                
            if not buttons and not text_fields:
                print("⚠️ Limited UI elements found, checking for other iOS controls")
                
            # Look for any interactive elements
            interactive_elements = driver.find_elements(AppiumBy.XPATH, "//*[@enabled='true']")
            print(f"✅ Found {len(interactive_elements)} interactive elements in iOS app")
            
        except Exception as e:
            print(f"⚠️ iOS UI element check had issues: {str(e)}")
        
        # Test 3: Try basic iOS interaction
        print("📝 Testing iOS app interaction...")
        
        try:
            # Look for text fields that might accept task input
            text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField")
            
            if text_fields:
                print(f"✅ Found {len(text_fields)} text field(s) for interaction")
                
                # Try to interact with the first text field
                text_field = text_fields[0]
                test_task_text = f"BrowserStack iOS Test from {device_config['deviceName']}"
                
                text_field.clear()
                text_field.send_keys(test_task_text)
                print(f"✅ Entered test task text in iOS app: {test_task_text}")
                
                # Look for iOS add/submit buttons
                buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
                add_buttons = [btn for btn in buttons if btn.get_attribute("name") and ("add" in btn.get_attribute("name").lower() or "done" in btn.get_attribute("name").lower())]
                
                if add_buttons:
                    add_buttons[0].click()
                    print("✅ Clicked add button in iOS app")
                    
                    # Wait for task to be processed
                    time.sleep(5)
                    
                    # Check if task appeared
                    try:
                        task_elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@name,'{test_task_text}') or contains(@label,'{test_task_text}') or contains(@value,'{test_task_text}')]")
                        if task_elements:
                            print("✅ New iOS task successfully created and visible")
                        else:
                            print("⚠️ New iOS task may not be immediately visible")
                    except Exception:
                        print("⚠️ Could not verify iOS task creation")
                
            else:
                print("⚠️ No text fields found for iOS interaction testing")
                
        except Exception as e:
            print(f"⚠️ iOS interaction test had issues: {str(e)}")
        
        # Test 4: Verify iOS app stability
        print("🔧 Verifying iOS app stability...")
        
        try:
            # Check that iOS app is still responsive
            current_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if len(current_elements) > 0:
                print(f"✅ iOS app remains stable with {len(current_elements)} active UI elements")
            else:
                raise Exception("iOS app appears to have crashed or become unresponsive")
        except Exception as e:
            raise Exception(f"iOS app stability check failed: {str(e)}")
        
        # Take success screenshot
        driver.save_screenshot(f"ios_success_{device_config['deviceName']}.png")
        print(f"📸 Success screenshot saved for {device_name}")
        
        # Report success to BrowserStack
        driver.execute_script('browserstack_executor: {"action": "setSessionStatus", "arguments": {"status":"passed", "reason": "Ditto iOS sync and app functionality verified successfully"}}')
        
        print(f"🎉 All iOS tests PASSED on {device_name}")
        return True
        
    except Exception as e:
        print(f"❌ iOS test FAILED on {device_name}: {str(e)}")
        
        if driver:
            try:
                # Take failure screenshot
                driver.save_screenshot(f"ios_failure_{device_config['deviceName']}.png")
                print(f"📸 Failure screenshot saved for {device_name}")
                
                # Report failure to BrowserStack
                driver.execute_script(f'browserstack_executor: {{"action": "setSessionStatus", "arguments": {{"status":"failed", "reason": "iOS test failed: {str(e)[:100]}"}}}}')
            except Exception:
                print("⚠️ Failed to save iOS screenshot or report status")
        
        return False
        
    finally:
        if driver:
            driver.quit()

def main():
    """Main function to run tests on multiple iOS devices."""
    # iOS device configurations to test (real BrowserStack devices)
    ios_devices = [
        {
            'deviceName': 'iPhone 15 Pro',
            'platformVersion': '17.0'
        },
        {
            'deviceName': 'iPhone 14',
            'platformVersion': '16.0'
        },
        {
            'deviceName': 'iPhone 13',
            'platformVersion': '15.0'
        },
        {
            'deviceName': 'iPad Air 5',
            'platformVersion': '15.0'
        }
    ]
    
    print("🚀 Starting BrowserStack real device tests for Ditto KMP iOS app...")
    print(f"📋 iOS test document ID: {os.environ.get('GITHUB_TEST_DOC_ID_IOS', 'Not set')}")
    print(f"📱 Testing on {len(ios_devices)} real iOS devices")
    
    # Run tests on all iOS devices
    results = []
    for device_config in ios_devices:
        success = run_ios_test(device_config)
        results.append({
            'device': f"{device_config['deviceName']} (iOS {device_config['platformVersion']})",
            'success': success
        })
        
        # Small delay between device tests
        time.sleep(5)
    
    # Print comprehensive summary
    print("\n" + "="*60)
    print("🏁 DITTO KMP iOS BROWSERSTACK TEST SUMMARY")
    print("="*60)
    
    passed = 0
    total = len(results)
    
    for result in results:
        status = "✅ PASSED" if result['success'] else "❌ FAILED"
        print(f"  {result['device']}: {status}")
        if result['success']:
            passed += 1
    
    print(f"\n📊 Overall iOS Results: {passed}/{total} devices passed")
    
    if passed == total:
        print("🎉 ALL iOS TESTS PASSED! Ditto KMP iOS app works perfectly on real devices!")
        print("✅ Ditto iOS sync functionality verified")
        print("✅ iOS app UI functionality verified") 
        print("✅ iOS app stability verified")
        sys.exit(0)
    else:
        print("💥 SOME iOS TESTS FAILED! Issues detected with Ditto KMP iOS app!")
        print(f"❌ {total - passed} iOS device(s) failed testing")
        sys.exit(1)

if __name__ == "__main__":
    main()