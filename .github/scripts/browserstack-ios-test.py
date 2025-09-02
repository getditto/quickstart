#!/usr/bin/env python3
"""
BrowserStack iOS testing script for Ditto Kotlin Multiplatform application.
This script verifies that documents inserted via Ditto HTTP API sync to the iOS app,
proving that Ditto sync functionality works correctly.
"""
import time
import json
import sys
import os
from appium import webdriver
from appium.options.ios import XCUITestOptions
from appium.webdriver.common.appiumby import AppiumBy

def wait_for_sync_document(driver, test_text, max_wait=60):
    """Wait for the HTTP API inserted document to appear in the iOS app."""
    print(f"📋 Waiting for HTTP API document to sync to iOS app...")
    print(f"🔍 Looking for: {test_text}")
    
    start_time = time.time()
    
    while (time.time() - start_time) < max_wait:
        try:
            # Method 1: Look for iOS UI elements containing the text
            try:
                # Standard iOS text elements
                text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeStaticText")
                text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeCell"))
                text_elements.extend(driver.find_elements(AppiumBy.CLASS_NAME, "XCUIElementTypeOther"))
                
                for element in text_elements:
                    try:
                        element_text = element.text.strip() if element.text else ""
                        if test_text in element_text:
                            print(f"✅ SUCCESS: Document synced from Ditto Cloud to iOS app!")
                            print(f"📄 Found: {element_text}")
                            return True
                    except Exception:
                        continue
                
                # Also check for partial matches with the run ID
                run_id = test_text.split()[-1] if test_text.split() else test_text
                for element in text_elements:
                    try:
                        element_text = element.text.strip() if element.text else ""
                        if run_id in element_text and "GitHub" in element_text and "Test" in element_text:
                            print(f"✅ SUCCESS: GitHub test document found in iOS app!")
                            print(f"📄 Element text: {element_text}")
                            return True
                    except Exception:
                        continue
                        
                # XPath approach for iOS
                xpath_elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@name,'{test_text}') or contains(@label,'{test_text}') or contains(@value,'{test_text}')]")
                if xpath_elements:
                    print(f"✅ SUCCESS: Document synced and visible in iOS UI!")
                    return True
                    
            except Exception as e:
                # Continue with other approaches
                pass
            
            # Method 2: Check page source (less reliable but comprehensive)
            try:
                page_source = driver.page_source
                if test_text in page_source:
                    print(f"✅ SUCCESS: Document found in iOS page source!")
                    print(f"📄 Found: {test_text}")
                    return True
            except Exception as e:
                pass
                
        except Exception as e:
            print(f"⚠️ Check attempt error: {e}")
        
        # Wait 2 seconds before next check
        time.sleep(2)
        
        # Print progress every 15 seconds
        elapsed = time.time() - start_time
        if int(elapsed) % 15 == 0 and elapsed > 10:
            print(f"⏳ Still waiting... {int(elapsed)}s elapsed")
    
    print(f"❌ Document not found after {max_wait} seconds")
    print("🔍 Final iOS elements check...")
    try:
        # Show what iOS elements we can see for debugging
        debug_elements = driver.find_elements(AppiumBy.XPATH, "//*[@name and string-length(@name) > 0]")
        visible_names = [elem.get_attribute("name") for elem in debug_elements[:5] if elem.get_attribute("name")]
        print(f"📱 Visible iOS elements: {visible_names}...")
    except:
        print("⚠️ Could not retrieve iOS debug info")
        
    return False

def run_ios_test(device_config):
    """Run sync verification test on specified iOS device."""
    device_name = f"{device_config['deviceName']} (iOS {device_config['platformVersion']})"
    print(f"📱 Starting Ditto sync verification on {device_name}")
    
    # BrowserStack iOS Appium capabilities
    options = XCUITestOptions()
    options.platform_name = "iOS"
    options.device_name = device_config['deviceName']
    options.platform_version = device_config['platformVersion']
    options.app = os.environ.get('BROWSERSTACK_IOS_APP_URL')
    options.new_command_timeout = 300
    options.no_reset = True
    
    # BrowserStack specific capabilities
    options.set_capability('browserstack.user', os.environ['BROWSERSTACK_USERNAME'])
    options.set_capability('browserstack.key', os.environ['BROWSERSTACK_ACCESS_KEY'])
    options.set_capability('project', 'Ditto KMP iOS')
    options.set_capability('build', f"KMP iOS Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}")
    options.set_capability('name', f"Ditto iOS Sync Verification - {device_name}")
    options.set_capability('browserstack.debug', 'true')
    options.set_capability('browserstack.video', 'true')
    options.set_capability('browserstack.networkLogs', 'true')
    options.set_capability('browserstack.appiumLogs', 'true')
    
    driver = None
    try:
        print(f"🚀 Connecting to BrowserStack for {device_name}...")
        # Create authenticated WebDriver URL for BrowserStack
        bs_username = os.environ['BROWSERSTACK_USERNAME']
        bs_access_key = os.environ['BROWSERSTACK_ACCESS_KEY']
        hub_url = f"https://{bs_username}:{bs_access_key}@hub.browserstack.com/wd/hub"
        
        driver = webdriver.Remote(hub_url, options=options)
        print(f"✅ Connected to {device_name}")
        
        # Wait for iOS app to launch and initialize
        print("⏳ Waiting for iOS app to initialize...")
        time.sleep(20)  # iOS apps may take longer to start
        
        # Check if app launched successfully
        try:
            # Look for any UI elements indicating the app loaded
            app_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if not app_elements:
                raise Exception("No UI elements found - iOS app may have crashed")
            print(f"✅ iOS app launched successfully with {len(app_elements)} UI elements")
        except Exception as e:
            raise Exception(f"iOS app launch verification failed: {str(e)}")
        
        # Wait for Ditto to initialize and sync on iOS
        print("🔄 Allowing time for Ditto SDK initialization and sync on iOS...")
        time.sleep(15)  # Give iOS Ditto more time to initialize and sync
        
        # Get the test document info from environment
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID_IOS')
        if not github_doc_id:
            raise Exception("No GitHub iOS test document ID provided")
        
        # Extract run ID and create expected text
        run_id = github_doc_id.split('_')[4] if len(github_doc_id.split('_')) > 4 else github_doc_id.split('_')[-1]
        test_text = f"GitHub KMP iOS Test {run_id}"
        
        print(f"🔍 Verifying Ditto sync for iOS document: {github_doc_id}")
        print(f"📋 Expected text: {test_text}")
        
        # Main test: Verify the HTTP API document synced to the iOS app
        if wait_for_sync_document(driver, test_text):
            print("🎉 DITTO iOS SYNC VERIFICATION PASSED!")
            print("✅ Document inserted via HTTP API successfully synced to iOS app")
            print("✅ Ditto SDK initialization and sync functionality verified on iOS")
            return True
        else:
            print("❌ DITTO iOS SYNC VERIFICATION FAILED!")
            print("💥 Document inserted via HTTP API did not sync to iOS app")
            # Take screenshot for debugging
            try:
                driver.save_screenshot(f"ios_sync_failed_{device_config['deviceName']}.png")
                print("📸 Failure screenshot saved")
            except:
                pass
            return False
        
    except Exception as e:
        print(f"❌ Test FAILED on {device_name}: {str(e)}")
        if driver:
            try:
                driver.save_screenshot(f"ios_error_{device_config['deviceName']}.png")
                print("📸 Error screenshot saved")
            except:
                pass
        return False
        
    finally:
        if driver:
            try:
                driver.quit()
            except:
                pass

def main():
    """Main function to run iOS BrowserStack sync verification tests."""
    print("📱 DITTO KMP iOS BROWSERSTACK SYNC VERIFICATION")
    print("=" * 60)
    
    # iOS device configurations to test
    ios_devices = [
        {'deviceName': 'iPhone 15 Pro', 'platformVersion': '17.0'},
        {'deviceName': 'iPhone 14', 'platformVersion': '16.0'},
        {'deviceName': 'iPhone 13', 'platformVersion': '15.0'},
        {'deviceName': 'iPad Air 5', 'platformVersion': '15.0'}
    ]
    
    # Run tests on all iOS devices
    results = []
    for device_config in ios_devices:
        print(f"\n📱 Starting Ditto sync test on {device_config['deviceName']} (iOS {device_config['platformVersion']})")
        success = run_ios_test(device_config)
        device_name = f"{device_config['deviceName']} (iOS {device_config['platformVersion']})"
        results.append({
            'device': device_name,
            'success': success
        })
        
        if success:
            print(f"✅ Test PASSED on {device_name}: Ditto sync verified")
        else:
            print(f"❌ Test FAILED on {device_name}: Failed to verify Ditto sync functionality in app")
        
        # Screenshot saved for debugging
        print(f"📸 Failure screenshot saved for {device_name}")
    
    # Print summary
    print("\n" + "=" * 60)
    print("🏁 DITTO KMP iOS BROWSERSTACK TEST SUMMARY")
    print("=" * 60)
    passed = 0
    total = len(results)
    for result in results:
        status = "✅ PASSED" if result['success'] else "❌ FAILED"
        print(f"  {result['device']}: {status}")
        if result['success']:
            passed += 1
    
    print(f"\n📊 Overall Results: {passed}/{total} devices passed")
    
    # Exit with appropriate code
    if passed == total:
        print("🎉 ALL BROWSERSTACK iOS TESTS PASSED!")
        print("✅ Ditto sync verified on real iOS devices")
        print("✅ iOS app functionality confirmed on iPhone 15 Pro, iPhone 14, iPhone 13, iPad Air 5")
        sys.exit(0)
    else:
        print("💥 SOME TESTS FAILED! Issues detected with Ditto KMP iOS app!")
        print(f"❌ {total - passed} device(s) failed testing")
        sys.exit(1)

if __name__ == "__main__":
    main()