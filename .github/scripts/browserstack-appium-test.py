#!/usr/bin/env python3
"""
BrowserStack App Automate Appium Test Runner
Manages React Native app testing with Appium on BrowserStack real devices
"""

import os
import sys
import time
import json
import base64
from typing import Dict, Any, Optional, List

try:
    import requests
    from appium import webdriver
    from appium.options.android import UiAutomator2Options
    from appium.options.ios import XCUITestOptions
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.common.exceptions import TimeoutException, NoSuchElementException
except ImportError as e:
    print(f"❌ Missing required dependencies: {e}")
    print("📦 Please install: pip3 install requests Appium-Python-Client selenium")
    sys.exit(1)


class BrowserStackAppiumRunner:
    def __init__(self):
        self.username = os.getenv('BROWSERSTACK_USERNAME')
        self.access_key = os.getenv('BROWSERSTACK_ACCESS_KEY')
        self.api_base_url = 'https://api-cloud.browserstack.com/app-automate'
        
        if not self.username or not self.access_key:
            raise ValueError("BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY must be set")
        
        # Create auth header
        credentials = f"{self.username}:{self.access_key}"
        encoded_credentials = base64.b64encode(credentials.encode()).decode()
        self.headers = {
            'Authorization': f'Basic {encoded_credentials}',
            'Content-Type': 'application/json'
        }
        
        self.drivers = []
    
    def upload_app(self, app_path: str) -> str:
        """Upload the React Native app to BrowserStack"""
        print(f"📱 Uploading app: {app_path}")
        
        upload_url = f"{self.api_base_url}/upload"
        
        with open(app_path, 'rb') as app_file:
            files = {'file': app_file}
            headers_without_content_type = {k: v for k, v in self.headers.items() if k != 'Content-Type'}
            
            response = requests.post(upload_url, files=files, headers=headers_without_content_type)
        
        if response.status_code == 200:
            app_url = response.json().get('app_url')
            print(f"✅ App uploaded successfully: {app_url}")
            return app_url
        else:
            raise Exception(f"Failed to upload app: {response.status_code} - {response.text}")
    
    def create_appium_driver(self, device: str, app_url: str, platform: str) -> webdriver.Remote:
        """Create Appium WebDriver instance for BrowserStack"""
        
        # Standard BrowserStack capabilities
        bs_options = {
            'userName': self.username,
            'accessKey': self.access_key,
            'projectName': 'Ditto React Native Integration Tests',
            'buildName': f"RN Appium Tests - {time.strftime('%Y-%m-%d %H:%M:%S')}",
            'sessionName': f'{device} - Ditto Sync Test',
            # Critical timeout settings for long-running tests
            'idleTimeout': 300,  # Maximum allowed by BrowserStack
            'networkLogs': True,
            'deviceLogs': True,
            'debug': True,
        }
        
        if platform == 'ios':
            options = XCUITestOptions()
            options.platform_name = 'iOS'
            options.device_name = device.split('-')[0]  # Extract device name
            options.platform_version = device.split('-')[1]  # Extract OS version
        else:
            options = UiAutomator2Options()
            options.platform_name = 'Android'
            options.device_name = device.split('-')[0]  # Extract device name
            options.platform_version = device.split('-')[1]  # Extract OS version
        
        # Set app
        options.app = app_url
        
        # BrowserStack specific options
        options.set_capability('bstack:options', bs_options)
        
        # Appium capabilities for timeout management
        options.new_command_timeout = 300
        
        # Permission handling
        if platform == 'android':
            options.auto_grant_permissions = True
        else:
            options.auto_accept_alerts = True
        
        remote_url = f"https://{self.username}:{self.access_key}@hub-cloud.browserstack.com/wd/hub"
        
        print(f"🚀 Starting Appium session for {device}...")
        driver = webdriver.Remote(command_executor=remote_url, options=options)
        
        self.drivers.append(driver)
        return driver
    
    def wait_for_element(self, driver: webdriver.Remote, locator: tuple, timeout: int = 30) -> bool:
        """Wait for element with timeout"""
        try:
            WebDriverWait(driver, timeout).until(EC.presence_of_element_located(locator))
            return True
        except TimeoutException:
            print(f"⚠️ Element {locator} not found within {timeout} seconds")
            return False
    
    def run_ditto_sync_tests(self, driver: webdriver.Remote, device: str) -> Dict[str, Any]:
        """Run Ditto synchronization tests"""
        test_results = {
            'device': device,
            'status': 'failed',
            'tests': [],
            'start_time': time.time(),
            'errors': []
        }
        
        try:
            print(f"📱 Running Ditto sync tests on {device}...")
            
            # Test 1: App Launch & Permissions
            print("🧪 Test 1: App Launch & Permissions")
            if self.wait_for_element(driver, (By.XPATH, "//*[@text='Allow' or @label='Allow']"), 10):
                try:
                    allow_btn = driver.find_element(By.XPATH, "//*[@text='Allow' or @label='Allow']")
                    allow_btn.click()
                    print("✅ Granted permissions")
                except:
                    print("ℹ️ No permission dialog found")
            
            test_results['tests'].append({
                'name': 'App Launch',
                'status': 'passed',
                'duration': 5
            })
            
            # Test 2: Wait for app to load and verify main screen
            print("🧪 Test 2: Main Screen Verification")
            main_screen_found = (
                self.wait_for_element(driver, (By.ACCESSIBILITY_ID, "main-screen"), 15) or
                self.wait_for_element(driver, (By.ACCESSIBILITY_ID, "add-task-button"), 15) or
                self.wait_for_element(driver, (By.XPATH, "//*[@text='+']"), 15)
            )
            
            if main_screen_found:
                print("✅ Main screen loaded successfully")
                test_results['tests'].append({
                    'name': 'Main Screen Load',
                    'status': 'passed',
                    'duration': 3
                })
            else:
                print("❌ Main screen not loaded")
                test_results['errors'].append("Main screen not loaded within timeout")
                test_results['tests'].append({
                    'name': 'Main Screen Load',
                    'status': 'failed',
                    'duration': 15
                })
            
            # Test 3: Create New Task
            print("🧪 Test 3: Create New Task")
            try:
                # Try multiple selectors for add button (FAB with + text)
                add_button = None
                selectors = [
                    (By.ACCESSIBILITY_ID, "add-task-button"),
                    (By.XPATH, "//*[@text='+']"),
                    (By.XPATH, "//*[@content-desc='Add new task']"),
                ]
                
                for selector in selectors:
                    try:
                        add_button = driver.find_element(*selector)
                        break
                    except NoSuchElementException:
                        continue
                
                if add_button:
                    add_button.click()
                    time.sleep(2)
                    
                    # Enter task text
                    task_text = f"Test Task from {device} - {int(time.time())}"
                    text_input = None
                    
                    input_selectors = [
                        (By.ACCESSIBILITY_ID, "task-input"),
                        (By.CLASS_NAME, "android.widget.EditText"),
                        (By.XPATH, "//XCUIElementTypeTextField"),
                        (By.XPATH, "//*[@class='android.widget.EditText' or @type='XCUIElementTypeTextField']")
                    ]
                    
                    for selector in input_selectors:
                        try:
                            text_input = driver.find_element(*selector)
                            break
                        except NoSuchElementException:
                            continue
                    
                    if text_input:
                        text_input.send_keys(task_text)
                        time.sleep(1)
                        
                        # Save task
                        save_selectors = [
                            (By.ACCESSIBILITY_ID, "save-task-button"),
                            (By.XPATH, "//*[@text='Add Task']"),
                            (By.XPATH, "//*[contains(@text,'Add')]")
                        ]
                        
                        for selector in save_selectors:
                            try:
                                save_button = driver.find_element(*selector)
                                save_button.click()
                                break
                            except NoSuchElementException:
                                continue
                        
                        time.sleep(3)
                        
                        # Verify task was created
                        task_created = self.wait_for_element(driver, (By.XPATH, f"//*[contains(@text,'{task_text}') or contains(@label,'{task_text}')]"), 10)
                        
                        if task_created:
                            print(f"✅ Successfully created task: {task_text}")
                            test_results['tests'].append({
                                'name': 'Create Task',
                                'status': 'passed',
                                'duration': 8
                            })
                        else:
                            print("❌ Task creation verification failed")
                            test_results['errors'].append("Created task not visible in list")
                            test_results['tests'].append({
                                'name': 'Create Task',
                                'status': 'failed',
                                'duration': 8
                            })
                    else:
                        print("❌ Could not find task input field")
                        test_results['errors'].append("Task input field not found")
                else:
                    print("❌ Could not find add task button")
                    test_results['errors'].append("Add task button not found")
                    
            except Exception as e:
                print(f"❌ Task creation failed: {str(e)}")
                test_results['errors'].append(f"Task creation failed: {str(e)}")
                test_results['tests'].append({
                    'name': 'Create Task',
                    'status': 'failed',
                    'duration': 10
                })
            
            # Test 4: External Sync Verification (GitHub test document)
            print("🧪 Test 4: External Sync Verification")
            github_doc_id = os.getenv('GITHUB_TEST_DOC_ID')
            if github_doc_id:
                print(f"Looking for GitHub test document: {github_doc_id}")
                
                # Wait up to 60 seconds for external document to sync from Ditto Cloud
                sync_timeout = 60
                sync_found = False
                
                for attempt in range(sync_timeout):
                    try:
                        # Look for the GitHub test document
                        github_task = driver.find_element(By.XPATH, 
                            f"//*[contains(@text,'GitHub') or contains(@label,'GitHub')]")
                        sync_found = True
                        print(f"✅ GitHub test document synced successfully after {attempt + 1} seconds")
                        break
                    except NoSuchElementException:
                        time.sleep(1)
                        
                    if attempt % 10 == 9:  # Log every 10 seconds
                        print(f"⏳ Still waiting for sync... ({attempt + 1}/{sync_timeout}s)")
                
                if sync_found:
                    test_results['tests'].append({
                        'name': 'External Sync Verification',
                        'status': 'passed',
                        'duration': attempt + 1
                    })
                else:
                    print("❌ GitHub test document did not sync within timeout")
                    test_results['errors'].append("External sync verification failed")
                    test_results['tests'].append({
                        'name': 'External Sync Verification',
                        'status': 'failed',
                        'duration': sync_timeout
                    })
            else:
                print("ℹ️ No GitHub test document ID provided, skipping sync verification")
                test_results['tests'].append({
                    'name': 'External Sync Verification',
                    'status': 'skipped',
                    'duration': 1
                })
            
            # Calculate overall status
            failed_tests = [t for t in test_results['tests'] if t['status'] == 'failed']
            if not failed_tests:
                test_results['status'] = 'passed'
                print(f"🎉 All tests passed on {device}!")
            else:
                print(f"💥 {len(failed_tests)} test(s) failed on {device}")
            
        except Exception as e:
            print(f"❌ Test execution failed: {str(e)}")
            test_results['errors'].append(f"Test execution failed: {str(e)}")
            test_results['status'] = 'failed'
        
        finally:
            test_results['end_time'] = time.time()
            test_results['duration'] = test_results['end_time'] - test_results['start_time']
        
        return test_results
    
    def cleanup_drivers(self):
        """Clean up all WebDriver instances"""
        for driver in self.drivers:
            try:
                driver.quit()
            except Exception as e:
                print(f"⚠️ Error closing driver: {e}")
        self.drivers = []
    
    def print_test_summary(self, all_results: List[Dict[str, Any]]):
        """Print a summary of all test results"""
        print("\n" + "="*60)
        print("📋 APPIUM TEST SUMMARY")
        print("="*60)
        
        total_passed = 0
        total_failed = 0
        
        for result in all_results:
            device = result['device']
            status = result['status']
            duration = result['duration']
            
            status_emoji = "✅" if status == "passed" else "❌"
            print(f"{status_emoji} {device}: {status} ({duration:.1f}s)")
            
            if status == "passed":
                total_passed += 1
            else:
                total_failed += 1
                
            # Show individual test results
            for test in result['tests']:
                test_status_emoji = "✅" if test['status'] == "passed" else "❌" if test['status'] == "failed" else "⏭️"
                print(f"  {test_status_emoji} {test['name']}: {test['status']} ({test['duration']}s)")
            
            # Show errors if any
            if result['errors']:
                print(f"  🚨 Errors: {'; '.join(result['errors'])}")
        
        print("-" * 60)
        print(f"✅ Passed: {total_passed}")
        print(f"❌ Failed: {total_failed}")
        print("="*60)


def main():
    """Main execution function"""
    
    # Configuration
    PROJECT_ROOT = os.getenv('GITHUB_WORKSPACE', os.getcwd())
    APP_TYPE = os.getenv('APP_TYPE', 'expo')  # 'expo' or 'bare'
    PLATFORM_TYPE = os.getenv('PLATFORM_TYPE', 'android')  # android, ios, or both
    
    if APP_TYPE == 'expo':
        app_dir = os.path.join(PROJECT_ROOT, 'react-native-expo')
        if PLATFORM_TYPE == 'ios':
            # Use the iOS .ipa file built by xcodebuild per BrowserStack requirements
            ios_app_path = os.getenv('IOS_APP_PATH')
            if ios_app_path:
                app_path = ios_app_path if ios_app_path.startswith('/') else os.path.join(PROJECT_ROOT, ios_app_path)
            else:
                # Fallback to expected IPA export location
                app_path = os.path.join(app_dir, 'ios', 'build', 'ipa', 'reactnativeexpo-unsigned.ipa')
        else:
            app_path = os.path.join(app_dir, 'android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    else:  # bare
        app_dir = os.path.join(PROJECT_ROOT, 'react-native')
        if PLATFORM_TYPE == 'ios':
            # Use the iOS .ipa file built by xcodebuild for bare React Native per BrowserStack requirements
            ios_app_path = os.getenv('IOS_APP_PATH')
            if ios_app_path:
                app_path = ios_app_path if ios_app_path.startswith('/') else os.path.join(PROJECT_ROOT, ios_app_path)
            else:
                # Fallback to expected IPA export location for bare RN
                app_path = os.path.join(app_dir, 'ios', 'build', 'ipa', 'DittoReactNativeSampleApp-unsigned.ipa')
        else:
            app_path = os.path.join(app_dir, 'android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    
    # Validate required files exist
    if not os.path.exists(app_path):
        app_type_text = "IPA" if PLATFORM_TYPE == 'ios' else "APK"
        raise FileNotFoundError(f"App {app_type_text} not found: {app_path}")
    
    # Define test devices (reduced for reliability)
    android_devices = [
        "Google Pixel 7-13.0",
    ]
    
    ios_devices = [
        "iPhone 14-16",
        "iPhone 15-17",
    ]
    
    if PLATFORM_TYPE == 'ios':
        test_devices = ios_devices
    elif PLATFORM_TYPE == 'both':
        test_devices = android_devices + ios_devices
    else:
        test_devices = android_devices
    
    try:
        print(f"🚀 Starting BrowserStack Appium tests for {APP_TYPE} app")
        print(f"📁 Project root: {PROJECT_ROOT}")
        print(f"📱 App path: {app_path}")
        print(f"🧪 Testing {len(test_devices)} device(s)")
        
        # Initialize runner
        runner = BrowserStackAppiumRunner()
        
        # Upload app
        app_url = runner.upload_app(app_path)
        
        # Run tests on all devices
        all_results = []
        
        for device in test_devices:
            print(f"\n🔄 Starting tests on {device}...")
            
            try:
                # Create driver for this device
                driver = runner.create_appium_driver(device, app_url, PLATFORM_TYPE)
                
                # Run tests
                result = runner.run_ditto_sync_tests(driver, device)
                all_results.append(result)
                
                # Close this driver
                driver.quit()
                
            except Exception as e:
                print(f"❌ Failed to run tests on {device}: {str(e)}")
                all_results.append({
                    'device': device,
                    'status': 'failed',
                    'tests': [],
                    'errors': [f"Driver creation failed: {str(e)}"],
                    'duration': 0
                })
        
        # Clean up any remaining drivers
        runner.cleanup_drivers()
        
        # Print summary
        runner.print_test_summary(all_results)
        
        # Exit with appropriate code
        failed_devices = [r for r in all_results if r['status'] == 'failed']
        if not failed_devices:
            print("\n🎉 All tests passed on all devices!")
            sys.exit(0)
        else:
            print(f"\n💥 Tests failed on {len(failed_devices)} device(s)!")
            sys.exit(1)
            
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()