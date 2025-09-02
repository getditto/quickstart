#!/usr/bin/env python3
"""
BrowserStack Android testing script for Ditto Kotlin Multiplatform application.
This script verifies that documents inserted via Ditto HTTP API sync to the Android app,
proving that Ditto sync functionality works correctly.
"""
import time
import json
import sys
import os
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy

def wait_for_sync_document(driver, test_text, max_wait=60):
    """Wait for the HTTP API inserted document to appear in the Android app."""
    print(f"📋 Waiting for HTTP API document to sync to Android app...")
    print(f"🔍 Looking for: {test_text}")
    
    start_time = time.time()
    
    while (time.time() - start_time) < max_wait:
        try:
            # Method 1: Check page source for the test text
            page_source = driver.page_source
            if test_text in page_source:
                print(f"✅ SUCCESS: Document synced from Ditto Cloud to Android app!")
                print(f"📄 Found: {test_text}")
                return True
            
            # Method 2: Look for UI elements containing the text
            try:
                # Try to find elements with matching text
                elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@text, '{test_text}')]")
                if elements:
                    print(f"✅ SUCCESS: Document synced and visible in Android UI!")
                    return True
                    
                # Also check for partial matches with the run ID
                run_id = test_text.split()[-1] if test_text.split() else test_text
                elements = driver.find_elements(AppiumBy.XPATH, f"//*[contains(@text, '{run_id}')]")
                if elements:
                    element_text = elements[0].text
                    if "GitHub" in element_text and "Test" in element_text:
                        print(f"✅ SUCCESS: GitHub test document found in Android app!")
                        print(f"📄 Element text: {element_text}")
                        return True
            except Exception as e:
                # Continue with page source check
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
    print("🔍 Final page source check...")
    try:
        final_source = driver.page_source
        print(f"📄 Page contains: {len(final_source)} characters")
    except:
        print("⚠️ Could not retrieve final page source")
        
    return False

def run_android_test(device_config):
    """Run sync verification test on specified Android device."""
    device_name = f"{device_config['deviceName']} (Android {device_config['platformVersion']})"
    print(f"📱 Starting Ditto sync verification on {device_name}")
    
    # BrowserStack Android Appium capabilities
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = device_config['deviceName']
    options.platform_version = device_config['platformVersion']
    options.app = os.environ.get('BROWSERSTACK_APP_URL')
    options.new_command_timeout = 300
    
    # BrowserStack specific capabilities
    options.set_capability('browserstack.user', os.environ['BROWSERSTACK_USERNAME'])
    options.set_capability('browserstack.key', os.environ['BROWSERSTACK_ACCESS_KEY'])
    options.set_capability('project', 'Ditto KMP Android')
    options.set_capability('build', f"KMP Android Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}")
    options.set_capability('name', f"Ditto Android Sync Verification - {device_name}")
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
        
        # Wait for app to launch and initialize
        print("⏳ Waiting for Android app to initialize...")
        time.sleep(15)  # Give app time to start
        
        # Check if app launched successfully
        try:
            # Look for any UI elements indicating the app loaded
            app_elements = driver.find_elements(AppiumBy.XPATH, "//*")
            if not app_elements:
                raise Exception("No UI elements found - Android app may have crashed")
            print(f"✅ Android app launched successfully with {len(app_elements)} UI elements")
        except Exception as e:
            raise Exception(f"Android app launch verification failed: {str(e)}")
        
        # Wait for Ditto to initialize and sync
        print("🔄 Allowing time for Ditto SDK initialization and sync...")
        time.sleep(10)  # Give Ditto time to initialize and sync
        
        # Get the test document info from environment
        github_doc_id = os.environ.get('GITHUB_TEST_DOC_ID')
        if not github_doc_id:
            raise Exception("No GitHub test document ID provided")
        
        # Extract run ID and create expected text
        run_id = github_doc_id.split('_')[4] if len(github_doc_id.split('_')) > 4 else github_doc_id.split('_')[-1]
        test_text = f"GitHub KMP Android Test {run_id}"
        
        print(f"🔍 Verifying Ditto sync for document: {github_doc_id}")
        print(f"📋 Expected text: {test_text}")
        
        # Main test: Verify the HTTP API document synced to the app
        if wait_for_sync_document(driver, test_text):
            print("🎉 DITTO SYNC VERIFICATION PASSED!")
            print("✅ Document inserted via HTTP API successfully synced to Android app")
            print("✅ Ditto SDK initialization and sync functionality verified")
            return True
        else:
            print("❌ DITTO SYNC VERIFICATION FAILED!")
            print("💥 Document inserted via HTTP API did not sync to Android app")
            # Take screenshot for debugging
            try:
                driver.save_screenshot(f"android_sync_failed_{device_config['deviceName']}.png")
                print("📸 Failure screenshot saved")
            except:
                pass
            return False
        
    except Exception as e:
        print(f"❌ Test FAILED on {device_name}: {str(e)}")
        if driver:
            try:
                driver.save_screenshot(f"android_error_{device_config['deviceName']}.png")
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
    """Main function to run Android BrowserStack sync verification tests."""
    print("🤖 DITTO KMP ANDROID BROWSERSTACK SYNC VERIFICATION")
    print("=" * 60)
    
    # Android device configurations to test
    android_devices = [
        {'deviceName': 'Google Pixel 8', 'platformVersion': '14.0'},
        {'deviceName': 'Samsung Galaxy S23', 'platformVersion': '13.0'},
        {'deviceName': 'Google Pixel 6', 'platformVersion': '12.0'},
        {'deviceName': 'OnePlus 9', 'platformVersion': '11.0'}
    ]
    
    # Run tests on all Android devices
    results = []
    for device_config in android_devices:
        print(f"\n🤖 Starting Ditto sync test on {device_config['deviceName']} (Android {device_config['platformVersion']})")
        success = run_android_test(device_config)
        device_name = f"{device_config['deviceName']} (Android {device_config['platformVersion']})"
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
    print("🏁 DITTO KMP ANDROID BROWSERSTACK TEST SUMMARY")
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
        print("🎉 ALL BROWSERSTACK ANDROID TESTS PASSED!")
        print("✅ Ditto sync verified on real Android devices")
        print("✅ App functionality confirmed on Pixel 8, Galaxy S23, Pixel 6, OnePlus 9")
        sys.exit(0)
    else:
        print("💥 SOME TESTS FAILED! Issues detected with Ditto KMP Android app!")
        print(f"❌ {total - passed} device(s) failed testing")
        sys.exit(1)

if __name__ == "__main__":
    main()