from appium import webdriver
from appium.options.ios import XCUITestOptions
import time
import sys
import os

def test_ditto_app():
    """Simple Ditto Swift app test on BrowserStack - just verify sync is working"""
    options = XCUITestOptions()
    options.platform_name = 'iOS'
    options.device_name = 'iPhone 15 Pro'
    options.platform_version = '17'
    options.app = os.environ['BROWSERSTACK_APP_URL']
    
    # BrowserStack specific capabilities
    options.set_capability('bstack:options', {
        'userName': os.environ['BROWSERSTACK_USERNAME'],
        'accessKey': os.environ['BROWSERSTACK_ACCESS_KEY'],
        'projectName': 'Ditto Swift App',
        'buildName': f"Build #{os.environ.get('GITHUB_RUN_NUMBER', 'local')}",
        'sessionName': 'Ditto Swift App Test',
        'debug': True,
        'networkLogs': True,
        'deviceLogs': True,
        'video': True
    })
    
    driver = None
    try:
        print("🚀 Starting BrowserStack test...")
        driver = webdriver.Remote('https://hub-cloud.browserstack.com/wd/hub', options=options)
        
        print("✅ App launched on real device")
        time.sleep(5)
        
        # Verify app is running
        assert driver.query_app_state('live.ditto.quickstart.Tasks') == 4
        print("✅ App is running in foreground")
        
        # Handle permission dialogs quickly
        try:
            buttons = driver.find_elements('class name', 'XCUIElementTypeButton')
            for button in buttons:
                if button.text and ('Allow' in button.text or "Don't Allow" in button.text):
                    print(f"📱 Handling permission: {button.text}")
                    button.click()
                    break
        except:
            pass
            
        time.sleep(8)  # Wait for app to settle and sync
        
        # Look for our specific seeded test document
        run_number = os.environ.get('GITHUB_RUN_NUMBER', 'unknown')
        expected_task = f"Test Task from BrowserStack #{run_number}"
        
        print(f"🔍 Looking for seeded test document: {expected_task}")
        
        static_texts = driver.find_elements('class name', 'XCUIElementTypeStaticText')
        found_seeded_task = False
        
        print(f"📱 Checking {len(static_texts)} UI elements...")
        
        for text_element in static_texts:
            try:
                text = text_element.text
                if text:
                    print(f"   Found: {text}")
                    if expected_task in text:
                        print(f"✅ SUCCESS: Found our seeded test document!")
                        found_seeded_task = True
                        break
            except:
                continue
        
        if found_seeded_task:
            print(f"✅ PASS: Ditto sync working - found seeded document on real device!")
            return True
        else:
            print(f"❌ FAIL: Expected document '{expected_task}' not found on device")
            print("⚠️  This indicates Ditto sync is not working properly")
            return False
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        return False
        
    finally:
        if driver:
            driver.quit()
            print("🔚 Test session ended")

if __name__ == "__main__":
    success = test_ditto_app()
    sys.exit(0 if success else 1)