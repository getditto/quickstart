#!/usr/bin/env python3
"""
Local Appium test script for debugging React Native app selectors
"""

import os
import sys

# Check if we have the React Native Expo APK built
APK_PATH = "react-native-expo/android/app/build/outputs/apk/debug/app-debug.apk"

if not os.path.exists(APK_PATH):
    print("❌ APK not found. Please build it first:")
    print("cd react-native-expo")
    print("cd android && ./gradlew assembleDebug")
    sys.exit(1)

print("✅ APK found:", APK_PATH)

# Check if we have dependencies
try:
    from appium import webdriver
    from appium.options.android import UiAutomator2Options
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    import requests
except ImportError as e:
    print(f"❌ Missing dependencies: {e}")
    print("Install with: pip3 install requests Appium-Python-Client selenium")
    sys.exit(1)

print("✅ Dependencies available")
print("📝 This script requires:")
print("1. Android device/emulator running")
print("2. Appium server running on localhost:4723")
print("3. APK installed on device")
print()
print("To set up:")
print("1. Start emulator or connect device")
print("2. Start Appium server: appium")
print("3. Install APK: adb install react-native-expo/android/app/build/outputs/apk/debug/app-debug.apk")
print()
print("This script will test the element selectors locally before running on BrowserStack")