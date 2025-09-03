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
        
        # Simple test: Just verify we can see tasks (proves sync is working)
        static_texts = driver.find_elements('class name', 'XCUIElementTypeStaticText')
        
        tasks_found = 0
        for text_element in static_texts:
            try:
                text = text_element.text
                if text and 'Task' in text and text not in ['Ditto Tasks', 'New Task']:
                    tasks_found += 1
                    print(f"✅ Found task: {text}")
            except:
                continue
        
        if tasks_found > 0:
            print(f"✅ SUCCESS: Found {tasks_found} synced tasks on device - Ditto sync is working!")
            return True
        else:
            print("❌ No tasks found on device - sync may not be working")
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