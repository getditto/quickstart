#!/usr/bin/env python3
import os
import time
from appium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

def test_ios_app():
    # BrowserStack capabilities for iOS
    desired_caps = {
        'platformName': 'iOS',
        'platformVersion': '17',
        'deviceName': 'iPhone 15',
        'app': os.environ.get('IOS_APP_URL'),
        'browserstack.user': os.environ.get('BROWSERSTACK_USERNAME'),
        'browserstack.key': os.environ.get('BROWSERSTACK_ACCESS_KEY'),
        'project': 'Ditto Flutter iOS',
        'build': f"iOS Build #{os.environ.get('GITHUB_RUN_NUMBER', '0')}",
        'name': 'Ditto Flutter iOS Integration Test'
    }

    driver = webdriver.Remote('https://hub-cloud.browserstack.com/wd/hub', desired_caps)
    
    try:
        # Wait for app to load
        print("Waiting for app to initialize...")
        time.sleep(10)
        
        # Try to find key UI elements
        try:
            # Look for app title or main UI elements
            title_element = WebDriverWait(driver, 30).until(
                lambda d: d.find_element(By.XPATH, "//*[contains(@name, 'Ditto') or contains(@label, 'Tasks')]")
            )
            print(f"✓ Found app UI element: {title_element.get_attribute('name') or title_element.get_attribute('label')}")
        except:
            print("⚠ Could not find specific app title, checking for any interactive elements...")
            
            # Fallback - look for any button or interactive element
            elements = driver.find_elements(By.XPATH, "//XCUIElementTypeButton | //XCUIElementTypeTextField | //XCUIElementTypeSwitch")
            if elements:
                print(f"✓ Found {len(elements)} interactive elements - app loaded successfully")
            else:
                print("❌ No interactive elements found - app may not have loaded properly")
                raise Exception("App did not load interactive elements")
        
        # Mark test as passed
        driver.execute_script('browserstack_executor: {"action": "setSessionStatus", "arguments": {"status":"passed", "reason": "iOS app loaded and basic UI verified"}}')
        print("✅ iOS app test completed successfully")
        return True
        
    except Exception as e:
        print(f"❌ iOS app test failed: {str(e)}")
        driver.execute_script(f'browserstack_executor: {{"action": "setSessionStatus", "arguments": {{"status":"failed", "reason": "Test failed: {str(e)[:100]}"}}}}')
        return False
    finally:
        driver.quit()

if __name__ == "__main__":
    success = test_ios_app()
    exit(0 if success else 1)