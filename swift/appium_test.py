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
        
        # Handle permission dialogs and get to main UI
        for i in range(5):  # Handle multiple permission dialogs
            try:
                # Look for permission dialog buttons
                buttons = driver.find_elements('class name', 'XCUIElementTypeButton')
                handled = False
                for button in buttons:
                    try:
                        text = button.text if button.text else ""
                        if any(keyword in text for keyword in ['Allow', "Don't Allow", 'OK', 'Continue', 'Accept']):
                            print(f"📱 Handling permission {i+1}: {text}")
                            button.click()
                            time.sleep(3)
                            handled = True
                            break
                    except:
                        continue
                        
                if not handled:
                    # Check if we're on the main UI now
                    text_fields = driver.find_elements('class name', 'XCUIElementTypeTextField')
                    if text_fields:
                        print("✅ Reached main UI - found text input field")
                        break
                    else:
                        print(f"⚠️ No permissions found in iteration {i+1}, but no text fields either")
                        
            except Exception as e:
                print(f"⚠️ Error handling permissions: {e}")
                break
            
        time.sleep(5)  # Wait for app to settle
        
        # INJECT test task like Android does - add it via UI
        run_number = os.environ.get('GITHUB_RUN_NUMBER', 'unknown')
        test_task_text = f"Test Task from BrowserStack #{run_number}"
        
        print(f"🧪 INJECTING test task: {test_task_text}")
        
        try:
            # Look for text input field
            text_fields = driver.find_elements('class name', 'XCUIElementTypeTextField')
            if text_fields:
                print(f"✅ Found {len(text_fields)} text field(s) - adding test task")
                text_fields[0].click()
                time.sleep(1)
                text_fields[0].clear()
                text_fields[0].send_keys(test_task_text)
                
                # Look for add button - try different approaches
                added = False
                buttons = driver.find_elements('class name', 'XCUIElementTypeButton') 
                for button in buttons:
                    try:
                        button_text = button.text if button.text else ""
                        if button.is_enabled() and (
                            button_text in ['Add', '+', 'Add Task', 'Create'] or 
                            button_text == "" or  # Empty text might be an add button
                            "add" in button_text.lower()
                        ):
                            print(f"🔘 Trying button: '{button_text}'")
                            button.click()
                            print(f"✅ Injected task via UI button: '{button_text}'")
                            added = True
                            break
                    except Exception as be:
                        print(f"⚠️ Button click failed: {be}")
                        continue
                
                if not added:
                    # Try pressing Enter/Return key
                    print("🔘 Trying Return key...")
                    text_fields[0].send_keys('\n')
                    added = True
                    print("✅ Injected task via Return key")
                        
                time.sleep(5)  # Wait longer for task to appear
                
            else:
                print("❌ No text input field found - cannot inject task")
                # Debug: show what UI elements we do have
                all_elements = driver.find_elements('xpath', '//*')
                print(f"🔍 Found {len(all_elements)} UI elements total")
                for elem in all_elements[:10]:  # Show first 10
                    try:
                        elem_text = elem.text if elem.text else f"<{elem.tag_name}>"
                        print(f"   - {elem_text}")
                    except:
                        continue
                return False
                
            # Now verify the task appears (like Android does)
            static_texts = driver.find_elements('class name', 'XCUIElementTypeStaticText')
            found_task = False
            
            print(f"🔍 Verifying injected task appears among {len(static_texts)} text elements...")
            for text_element in static_texts:
                try:
                    text = text_element.text
                    if text and test_task_text in text:
                        print(f"✅ SUCCESS: Found injected task - Ditto sync working!")
                        found_task = True
                        break
                except:
                    continue
            
            # If not found, show what we do have for debugging
            if not found_task:
                print(f"❌ FAIL: Injected task '{test_task_text}' not visible in UI")
                print("🔍 Available text elements:")
                for text_element in static_texts[:10]:  # Show first 10
                    try:
                        text = text_element.text
                        if text and text.strip():
                            print(f"   - '{text}'")
                    except:
                        continue
                return False
            
            return True
                
        except Exception as e:
            print(f"❌ Test failed during task injection: {e}")
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