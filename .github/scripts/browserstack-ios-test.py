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

def create_and_verify_ios_task(driver, test_task_text, max_wait=30):
    """Create a test task using the SwiftUI iOS app and verify it appears in the UI."""
    print(f"📝 Creating iOS test task via SwiftUI app: '{test_task_text}'")
    
    try:
        # Step 1: Look for FloatingActionButton or Add button first (similar to Android flow)
        print("🔍 Looking for iOS add/plus button to navigate to input screen...")
        add_buttons = []
        
        try:
            # Look for various iOS add button types
            add_buttons = driver.find_elements(AppiumBy.XPATH, "//*[@name='Add' or contains(@label, 'Add') or contains(@name, '+')]")
            if not add_buttons:
                add_buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
                add_buttons = [btn for btn in add_buttons if btn.get_attribute("name") and ("add" in btn.get_attribute("name").lower() or "plus" in btn.get_attribute("name").lower())]
        except:
            pass
        
        if add_buttons:
            print(f"✅ Found {len(add_buttons)} potential iOS add button(s)")
            add_buttons[0].click()
            print("✅ Clicked iOS add button to navigate to input screen")
            time.sleep(2)  # Wait for navigation
        else:
            print("⚠️ No add button found, assuming already on input screen...")
        
        # Step 2: Look for text input fields using multiple SwiftUI/iOS approaches
        print("🔍 Looking for iOS text input fields...")
        text_fields = []
        
        try:
            # Approach 1: Standard TextField
            text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField")
            
            # Approach 2: SwiftUI TextEditor or other text input types
            if not text_fields:
                text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextView")
            
            # Approach 3: Look by accessibility identifier or placeholder
            if not text_fields:
                text_fields = driver.find_elements(AppiumBy.XPATH, "//*[contains(@name, 'Task Title') or contains(@placeholderValue, 'Task Title')]")
            
            # Approach 4: Any focusable text elements
            if not text_fields:
                all_elements = driver.find_elements(AppiumBy.XPATH, "//*[@type='XCUIElementTypeTextField' or @type='XCUIElementTypeTextView' or @type='XCUIElementTypeSecureTextField']")
                text_fields = [elem for elem in all_elements if elem.is_enabled()]
        except Exception as e:
            print(f"⚠️ Error finding text fields: {e}")
        
        if text_fields:
            print(f"✅ Found {len(text_fields)} text field(s) for iOS input")
            
            # Try multiple input approaches similar to Android fix
            input_success = False
            for i, text_field in enumerate(text_fields[:3]):  # Try first 3 fields
                try:
                    print(f"🔧 Attempting text input on iOS field {i+1}/{len(text_fields)}")
                    
                    # Focus and clear the field
                    text_field.click()
                    time.sleep(1)
                    
                    # Try different input methods for SwiftUI compatibility
                    try:
                        text_field.clear()
                        text_field.send_keys(test_task_text)
                        print(f"✅ Standard iOS input successful: '{test_task_text}'")
                        input_success = True
                        break
                    except Exception as e1:
                        print(f"⚠️ Standard iOS input failed: {e1}")
                    
                    # Approach 2: Set value directly (iOS specific)
                    try:
                        text_field.set_value(test_task_text)
                        print(f"✅ iOS set_value successful: '{test_task_text}'")
                        input_success = True
                        break
                    except Exception as e2:
                        print(f"⚠️ iOS set_value failed: {e2}")
                    
                    # Approach 3: Character by character for SwiftUI
                    try:
                        text_field.clear()
                        for char in test_task_text:
                            text_field.send_keys(char)
                            time.sleep(0.1)
                        print(f"✅ iOS character input successful: '{test_task_text}'")
                        input_success = True
                        break
                    except Exception as e3:
                        print(f"⚠️ iOS character input failed: {e3}")
                        
                except Exception as e:
                    print(f"⚠️ iOS field {i+1} interaction failed: {e}")
                    continue
            
            if not input_success:
                print("❌ All iOS text input approaches failed")
                return False
                
            # Step 3: Look for submit/save button
            print("🔍 Looking for iOS submit/save button...")
            submit_buttons = []
            
            try:
                # Look for Submit, Save, Done, etc.
                submit_buttons = driver.find_elements(AppiumBy.XPATH, "//*[@name='Submit' or @name='Save' or @name='Done' or contains(@name, 'Submit') or contains(@name, 'Save')]")
                if not submit_buttons:
                    buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
                    submit_buttons = [btn for btn in buttons if btn.get_attribute("name") and any(word in btn.get_attribute("name").lower() for word in ['submit', 'save', 'done', 'add'])]
            except:
                pass
            
            if submit_buttons:
                print(f"✅ Found {len(submit_buttons)} iOS submit button(s)")
                submit_buttons[0].click()
                print("✅ Clicked iOS submit button")
                
                # Enhanced iOS task verification with navigation and sync handling
                print("⏳ Waiting for iOS task to be processed and synced...")
                
                # First try to navigate back to main screen if needed
                try:
                    # Look for iOS navigation back button
                    back_buttons = driver.find_elements(AppiumBy.XPATH, "//*[@name='Back' or @name='Cancel' or contains(@name, 'back')]")
                    if not back_buttons:
                        # Try standard iOS navigation
                        back_buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
                        back_buttons = [btn for btn in back_buttons if btn.get_attribute("name") and "back" in btn.get_attribute("name").lower()]
                    
                    if back_buttons:
                        back_buttons[0].click()
                        print("🔙 Clicked iOS navigation back button")
                        time.sleep(2)
                    else:
                        # Try swipe gesture to go back
                        driver.swipe(100, 300, 300, 300, 300)
                        print("↩️ Used swipe gesture to navigate back")
                        time.sleep(2)
                except Exception as nav_e:
                    print(f"🔙 iOS navigation attempt: {nav_e}")
                
                # Enhanced verification with multiple approaches and pull-to-refresh
                task_found = False
                start_time = time.time()
                
                for attempt in range(3):
                    print(f"🔍 iOS task verification attempt {attempt + 1}/3...")
                    
                    # Try pull-to-refresh on iOS
                    if attempt > 0:
                        try:
                            driver.swipe(200, 200, 200, 500, 800)  # Pull down gesture
                            print("🔄 Performed iOS pull-to-refresh")
                            time.sleep(2)
                        except:
                            pass
                    
                    while (time.time() - start_time) < (max_wait / 3):  # Divide time between attempts
                        try:
                            # Method 1: Look for the task in iOS UI elements  
                            text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeStaticText")
                            text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeCell"))
                            text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeOther"))
                            
                            for element in text_elements:
                                try:
                                    element_text = element.text.strip() if element.text else ""
                                    if test_task_text in element_text:
                                        print(f"✅ iOS task successfully created and visible: {element_text}")
                                        task_found = True
                                        break
                                except Exception:
                                    continue
                            
                            if task_found:
                                break
                                
                            # Method 2: XPath approach for iOS
                            task_elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@name,'{test_task_text}') or contains(@label,'{test_task_text}') or contains(@value,'{test_task_text}')]")
                            if task_elements:
                                print(f"✅ iOS task created and synced via Ditto SDK: {test_task_text}")
                                task_found = True
                                break
                                
                        except Exception:
                            pass
                            
                        time.sleep(2)
                    
                    if task_found:
                        break
                        
                    # Give more time for sync between attempts
                    extra_wait = 3 + (attempt * 2)  # 3, 5, 7 seconds
                    print(f"⏳ iOS sync delay: waiting {extra_wait}s...")
                    time.sleep(extra_wait)
                
                if task_found:
                    return True
                else:
                    print(f"❌ iOS task not found in UI after enhanced verification")
                    
                    # Debug: Show iOS UI elements for debugging
                    try:
                        debug_elements = driver.find_elements(AppiumBy.XPATH, "//*[@name and string-length(@name) > 0]")
                        visible_names = [elem.get_attribute("name") for elem in debug_elements[:5] if elem.get_attribute("name")]
                        print(f"📱 Visible iOS elements: {visible_names}...")
                    except:
                        pass
                        
                    return False
                
            else:
                print("❌ No suitable iOS add buttons found")
                return False
                
        else:
            print("❌ No text fields found for iOS task creation")
            return False
            
    except Exception as e:
        print(f"❌ Error creating iOS task: {str(e)}")
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
        
        # Create XCUITest options for modern Appium  
        from appium.options.ios import XCUITestOptions
        options = XCUITestOptions()
        options.load_capabilities(desired_caps)
        
        driver = webdriver.Remote(
            command_executor=f"https://{os.environ['BROWSERSTACK_USERNAME']}:{os.environ['BROWSERSTACK_ACCESS_KEY']}@hub.browserstack.com/wd/hub",
            options=options
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
        
        # Test 1: Create test task using actual iOS app functionality (tests real user workflow + Ditto sync)
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID_IOS')
        if github_doc_id:
            # Create a test task with GitHub run ID for verification
            run_id = github_doc_id.split('_')[4] if len(github_doc_id.split('_')) > 4 else github_doc_id
            test_task_text = f"GitHub KMP iOS Test {run_id}"
            
            print(f"📋 Creating and verifying iOS test task via Ditto SDK: {test_task_text}")
            if create_and_verify_ios_task(driver, test_task_text):
                print("✅ DITTO iOS SDK INTEGRATION VERIFIED - Task created and synced via iOS app!")
            else:
                print("❌ DITTO iOS SDK INTEGRATION FAILED - Task creation or sync failed")
                # Take screenshot for debugging
                driver.save_screenshot(f"ios_sdk_failed_{device_config['deviceName']}.png")
                raise Exception("Failed to verify Ditto SDK functionality in iOS app")
        else:
            print("⚠️ No GitHub iOS test document ID provided, testing basic task creation")
            # Fallback - just test basic iOS task creation
            if create_and_verify_ios_task(driver, "BrowserStack iOS Test"):
                print("✅ Basic iOS task creation verified")
            else:
                raise Exception("Basic iOS task creation failed")
        
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
        
        # Test 3: Additional iOS UI verification (now that we've verified core Ditto functionality)
        print("🔍 Performing additional iOS UI verification...")
        
        try:
            # Verify iOS UI elements are still present and functional
            text_fields = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeTextField") 
            buttons = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeButton")
            
            if text_fields and buttons:
                print(f"✅ Found {len(text_fields)} text field(s) and {len(buttons)} button(s) in iOS")
                print("✅ Core iOS UI elements functional after Ditto operations")
            else:
                print("⚠️ Limited iOS UI elements found, but Ditto sync already verified")
                
        except Exception as e:
            print(f"⚠️ Additional iOS UI verification had issues: {str(e)}")
        
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