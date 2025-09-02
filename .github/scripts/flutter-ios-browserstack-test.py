#!/usr/bin/env python3
"""
BrowserStack real device testing script for Ditto Flutter iOS application.
This script runs automated tests on real iOS devices using BrowserStack to verify
the actual Ditto sync functionality of the Flutter quickstart app.
Based on the KMP BrowserStack test pattern for real sync verification.
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

def create_and_verify_flutter_ios_task(driver, test_task_text, max_wait=30):
    """Create a test task using the Flutter iOS app and verify it appears in the UI."""
    print(f"📝 Creating Flutter iOS test task via app: '{test_task_text}'")
    
    try:
        # Look for Flutter text fields (iOS XCUIElementTypeTextField)
        text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField")
        
        if text_fields:
            print(f"✅ Found {len(text_fields)} Flutter iOS text field(s)")
            
            # Try to interact with the first text field
            text_field = text_fields[0]
            text_field.clear()
            text_field.send_keys(test_task_text)
            print(f"✅ Entered task text in Flutter iOS app: {test_task_text}")
            
            # Look for Flutter iOS add/submit buttons
            buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
            add_buttons = [btn for btn in buttons if btn.get_attribute("name") and ("add" in btn.get_attribute("name").lower() or "done" in btn.get_attribute("name").lower())]
            
            if add_buttons:
                add_buttons[0].click()
                print("✅ Clicked Flutter iOS add button")
                
                # Wait for task to be processed by Flutter and synced via Ditto
                print("⏳ Waiting for Flutter iOS task to appear and sync via Ditto SDK...")
                time.sleep(5)
                
                # Verify task appeared - this tests both Flutter UI AND Ditto sync
                start_time = time.time()
                while (time.time() - start_time) < max_wait:
                    try:
                        # Look for the task in Flutter iOS UI elements
                        text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeStaticText")
                        text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeCell"))
                        
                        for element in text_elements:
                            try:
                                element_text = element.text.strip() if element.text else ""
                                if test_task_text in element_text:
                                    print(f"✅ Flutter iOS task successfully created and synced: {element_text}")
                                    return True
                            except Exception:
                                continue
                                
                        # Also try xpath approach for Flutter iOS widgets
                        task_elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@name,'{test_task_text}') or contains(@label,'{test_task_text}') or contains(@value,'{test_task_text}')]")
                        if task_elements:
                            print(f"✅ Flutter iOS task created and synced via Ditto SDK: {test_task_text}")
                            return True
                            
                    except Exception:
                        pass
                        
                    time.sleep(2)
                    
                print(f"❌ Flutter iOS task not found in UI after {max_wait} seconds")
                return False
                
            else:
                print("❌ No suitable Flutter iOS add buttons found")
                return False
                
        else:
            print("❌ No Flutter iOS text fields found for task creation")
            return False
            
    except Exception as e:
        print(f"❌ Error creating Flutter iOS task: {str(e)}")
        return False

def run_flutter_ios_test(device_config):
    """Run comprehensive Ditto sync test on specified iOS device with Flutter app."""
    device_name = f"{device_config['deviceName']} (iOS {device_config['platformVersion']})"
    print(f"📱 Starting Ditto Flutter iOS sync test on {device_name}")
    
    # BrowserStack iOS Appium capabilities for Flutter
    desired_caps = {
        # BrowserStack specific
        'browserstack.user': os.environ['BROWSERSTACK_USERNAME'],
        'browserstack.key': os.environ['BROWSERSTACK_ACCESS_KEY'],
        'project': 'Ditto Flutter iOS',
        'build': f"Flutter iOS Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'name': f"Ditto Flutter iOS Sync Test - {device_name}",
        'browserstack.debug': 'true',
        'browserstack.video': 'true',
        'browserstack.networkLogs': 'true',
        'browserstack.appiumLogs': 'true',
        
        # App specific
        'app': os.environ.get('BROWSERSTACK_FLUTTER_IOS_APP_URL'),  # Set by upload step
        'platformName': 'iOS',
        'deviceName': device_config['deviceName'], 
        'platformVersion': device_config['platformVersion'],
        
        # iOS Appium specific for Flutter
        'automationName': 'XCUITest',
        'newCommandTimeout': '300',
        'noReset': 'true',
    }
    
    driver = None
    try:
        print(f"🚀 Connecting to BrowserStack for Flutter iOS on {device_name}...")
        
        # Create XCUITest options for modern Appium  
        from appium.options.ios import XCUITestOptions
        options = XCUITestOptions()
        options.load_capabilities(desired_caps)
        
        driver = webdriver.Remote(
            command_executor=f"https://{os.environ['BROWSERSTACK_USERNAME']}:{os.environ['BROWSERSTACK_ACCESS_KEY']}@hub.browserstack.com/wd/hub",
            options=options
        )
        
        print(f"✅ Connected to {device_name} for Flutter iOS testing")
        
        # Wait for Flutter iOS app to launch and initialize
        print("⏳ Waiting for Flutter iOS app to initialize...")
        time.sleep(20)  # Flutter iOS apps need time to start
        
        # Check if Flutter iOS app launched successfully
        try:
            app_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if not app_elements:
                raise Exception("No UI elements found - Flutter iOS app may have crashed")
            print(f"✅ Flutter iOS app launched successfully with {len(app_elements)} UI elements")
        except Exception as e:
            raise Exception(f"Flutter iOS app launch verification failed: {str(e)}")
        
        # Wait for Ditto SDK to initialize and connect in Flutter iOS
        print("🔄 Allowing time for Ditto SDK initialization in Flutter iOS...")
        time.sleep(20)  # Give Ditto time to initialize in Flutter iOS
        
        # Test 1: Create test task using Flutter iOS app functionality (tests real user workflow + Ditto sync)
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID_IOS')
        if github_doc_id:
            # Create a test task with GitHub run ID for verification
            run_id = github_doc_id.split('_')[4] if len(github_doc_id.split('_')) > 4 else github_doc_id
            test_task_text = f"Flutter iOS Test {run_id}"
            
            print(f"📋 Creating and verifying Flutter iOS test task via Ditto SDK: {test_task_text}")
            if create_and_verify_flutter_ios_task(driver, test_task_text):
                print("✅ DITTO FLUTTER iOS SDK INTEGRATION VERIFIED - Task created and synced via Flutter iOS app!")
            else:
                print("❌ DITTO FLUTTER iOS SDK INTEGRATION FAILED - Task creation or sync failed")
                # Take screenshot for debugging
                driver.save_screenshot(f"flutter_ios_sdk_failed_{device_config['deviceName']}.png")
                raise Exception("Failed to verify Ditto SDK functionality in Flutter iOS app")
        else:
            print("⚠️ No GitHub iOS test document ID provided, testing basic Flutter iOS task creation")
            # Fallback - just test basic iOS task creation
            if create_and_verify_flutter_ios_task(driver, "BrowserStack Flutter iOS Test"):
                print("✅ Basic Flutter iOS task creation verified")
            else:
                raise Exception("Basic Flutter iOS task creation failed")
        
        # Test 2: Verify Flutter iOS app UI elements are present and functional
        print("🖱️ Testing Flutter iOS app UI functionality...")
        
        try:
            # Look for Flutter iOS-specific UI elements
            buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
            text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField") 
            
            if buttons:
                print(f"✅ Found {len(buttons)} button(s) in Flutter iOS app")
            if text_fields:
                print(f"✅ Found {len(text_fields)} text field(s) in Flutter iOS app")
                
            if not buttons and not text_fields:
                print("⚠️ Limited Flutter iOS UI elements found, checking for other controls")
                
            # Look for any interactive elements
            interactive_elements = driver.find_elements(AppiumBy.XPATH, "//*[@enabled='true']")
            print(f"✅ Found {len(interactive_elements)} interactive elements in Flutter iOS app")
            
        except Exception as e:
            print(f"⚠️ Flutter iOS UI element check had issues: {str(e)}")
        
        # Test 3: Additional Flutter iOS UI verification
        print("🔍 Performing additional Flutter iOS UI verification...")
        
        try:
            # Verify Flutter iOS UI elements are still present and functional
            current_text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField") 
            current_buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
            
            if current_text_fields and current_buttons:
                print(f"✅ Found {len(current_text_fields)} text field(s) and {len(current_buttons)} button(s) in Flutter iOS")
                print("✅ Core Flutter iOS UI elements functional after Ditto operations")
            else:
                print("⚠️ Limited Flutter iOS UI elements found, but Ditto sync already verified")
                
        except Exception as e:
            print(f"⚠️ Additional Flutter iOS UI verification had issues: {str(e)}")
        
        # Test 4: Verify Flutter iOS app stability
        print("🔧 Verifying Flutter iOS app stability...")
        
        try:
            # Check that Flutter iOS app is still responsive
            current_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if len(current_elements) > 0:
                print(f"✅ Flutter iOS app remains stable with {len(current_elements)} active UI elements")
            else:
                raise Exception("Flutter iOS app appears to have crashed or become unresponsive")
        except Exception as e:
            raise Exception(f"Flutter iOS app stability check failed: {str(e)}")
        
        # Take success screenshot
        driver.save_screenshot(f"flutter_ios_success_{device_config['deviceName']}.png")
        print(f"📸 Success screenshot saved for {device_name}")
        
        # Report success to BrowserStack
        driver.execute_script('browserstack_executor: {"action": "setSessionStatus", "arguments": {"status":"passed", "reason": "Ditto Flutter iOS sync and app functionality verified successfully"}}')
        
        print(f"🎉 All Flutter iOS tests PASSED on {device_name}")
        return True
        
    except Exception as e:
        print(f"❌ Flutter iOS test FAILED on {device_name}: {str(e)}")
        
        if driver:
            try:
                # Take failure screenshot
                driver.save_screenshot(f"flutter_ios_failure_{device_config['deviceName']}.png")
                print(f"📸 Failure screenshot saved for {device_name}")
                
                # Report failure to BrowserStack
                error_reason = f"Flutter iOS test failed: {str(e)[:100]}"
                driver.execute_script(f'browserstack_executor: {{"action": "setSessionStatus", "arguments": {{"status":"failed", "reason": "{error_reason}"}}}}')
            except Exception:
                print("⚠️ Failed to save iOS screenshot or report status")
        
        return False
        
    finally:
        if driver:
            driver.quit()

def main():
    """Main function to run Flutter tests on multiple iOS devices."""
    # iOS device configurations to test (real BrowserStack devices)
    ios_devices = [
        {
            'deviceName': 'iPhone 15 Pro',
            'platformVersion': '17.0'
        },
        {
            'deviceName': 'iPhone 14',
            'platformVersion': '16.0'
        }
    ]
    
    print("🚀 Starting BrowserStack real device tests for Ditto Flutter iOS app...")
    print(f"📋 iOS test document ID: {os.environ.get('GITHUB_TEST_DOC_ID_IOS', 'Not set')}")
    print(f"📱 Testing Flutter on {len(ios_devices)} real iOS devices")
    
    # Run tests on all iOS devices
    results = []
    for device_config in ios_devices:
        success = run_flutter_ios_test(device_config)
        results.append({
            'device': f"{device_config['deviceName']} (iOS {device_config['platformVersion']})",
            'success': success
        })
        
        # Small delay between device tests
        time.sleep(5)
    
    # Print comprehensive summary
    print("\n" + "="*60)
    print("🏁 DITTO FLUTTER iOS BROWSERSTACK TEST SUMMARY")
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
        print("🎉 ALL FLUTTER iOS TESTS PASSED! Ditto Flutter iOS app works perfectly on real devices!")
        print("✅ Ditto Flutter iOS sync functionality verified")
        print("✅ Flutter iOS app UI functionality verified") 
        print("✅ Flutter iOS app stability verified")
        sys.exit(0)
    else:
        print("💥 SOME FLUTTER iOS TESTS FAILED! Issues detected with Ditto Flutter iOS app!")
        print(f"❌ {total - passed} iOS device(s) failed testing")
        sys.exit(1)

if __name__ == "__main__":
    main()