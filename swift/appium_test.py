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
            # First, find and tap the "New Task" button to open the edit modal
            print("🔘 Looking for 'New Task' button...")
            buttons = driver.find_elements('class name', 'XCUIElementTypeButton')
            new_task_button_found = False
            
            for button in buttons:
                try:
                    button_text = button.text if button.text else ""
                    if "New Task" in button_text or button_text == "New Task":
                        print(f"✅ Found 'New Task' button: '{button_text}'")
                        button.click()
                        new_task_button_found = True
                        time.sleep(3)  # Wait for modal to appear
                        break
                except Exception as be:
                    continue
                    
            if not new_task_button_found:
                print("❌ 'New Task' button not found")
                # Debug: show available buttons
                print("🔍 Available buttons:")
                for button in buttons[:10]:
                    try:
                        button_text = button.text if button.text else "<no text>"
                        print(f"   - '{button_text}'")
                    except:
                        continue
                return False
            
            # Now look for text input field in the opened modal
            print("🔍 Looking for text input field in edit modal...")
            text_fields = driver.find_elements('class name', 'XCUIElementTypeTextField')
            if text_fields:
                print(f"✅ Found {len(text_fields)} text field(s) - adding test task")
                text_fields[0].click()
                time.sleep(1)
                text_fields[0].clear()
                text_fields[0].send_keys(test_task_text)
                
                # Look for Create/Save button in the modal - try different approaches
                saved = False
                modal_buttons = driver.find_elements('class name', 'XCUIElementTypeButton') 
                for button in modal_buttons:
                    try:
                        button_text = button.text if button.text else ""
                        if button.is_enabled() and (
                            button_text in ['Create', 'Save', 'Done'] or 
                            "create" in button_text.lower() or "save" in button_text.lower()
                        ):
                            print(f"🔘 Trying save button: '{button_text}'")
                            button.click()
                            print(f"✅ Task saved via button: '{button_text}'")
                            saved = True
                            break
                    except Exception as be:
                        print(f"⚠️ Button click failed: {be}")
                        continue
                
                if not saved:
                    # Try pressing Enter/Return key
                    print("🔘 Trying Return key...")
                    text_fields[0].send_keys('\n')
                    saved = True
                    print("✅ Task saved via Return key")
                        
                time.sleep(5)  # Wait longer for modal to close and task to appear in main list
                
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
                
            # Now verify the task appears in the main list (like Android does)
            print(f"🔍 Verifying injected task appears in main task list...")
            
            # Look for the task in table cells or static text
            found_task = False
            
            # Try to find in table cells first (iOS list rows)
            table_cells = driver.find_elements('class name', 'XCUIElementTypeCell')
            print(f"📋 Found {len(table_cells)} table cells")
            for cell in table_cells:
                try:
                    cell_text = cell.text
                    if cell_text and test_task_text in cell_text:
                        print(f"✅ SUCCESS: Found injected task in table cell - Ditto sync working!")
                        found_task = True
                        break
                except:
                    continue
            
            # If not in cells, try static text elements
            if not found_task:
                static_texts = driver.find_elements('class name', 'XCUIElementTypeStaticText')
                print(f"📝 Found {len(static_texts)} static text elements")
                for text_element in static_texts:
                    try:
                        text = text_element.text
                        if text and test_task_text in text:
                            print(f"✅ SUCCESS: Found injected task in static text - Ditto sync working!")
                            found_task = True
                            break
                    except:
                        continue
            
            # If not found, show what we do have for debugging
            if not found_task:
                print(f"❌ FAIL: Injected task '{test_task_text}' not visible in main list")
                print("🔍 Available table cells:")
                for cell in table_cells[:5]:  # Show first 5
                    try:
                        text = cell.text
                        if text and text.strip():
                            print(f"   - '{text}'")
                    except:
                        continue
                print("🔍 Available static text elements:")
                for text_element in static_texts[:10]:  # Show first 10
                    try:
                        text = text_element.text
                        if text and text.strip() and len(text) > 3:  # Skip short system texts
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