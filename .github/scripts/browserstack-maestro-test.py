#!/usr/bin/env python3
"""
BrowserStack App Automate Maestro Test Runner
Manages React Native app testing with Maestro on BrowserStack real devices
"""

import os
import sys
import time
import json
import requests
import zipfile
import tempfile
from typing import Dict, Any, Optional, List
import base64


class BrowserStackMaestroRunner:
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
    
    def create_maestro_suite_zip(self, maestro_config_path: str, flows_dir: str) -> str:
        """Create a zip file containing Maestro configuration and flows"""
        print(f"📦 Creating Maestro suite zip from: {flows_dir}")
        
        # Create temporary zip file
        temp_dir = tempfile.mkdtemp()
        zip_path = os.path.join(temp_dir, 'maestro-suite.zip')
        
        with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            # Add config file
            if os.path.exists(maestro_config_path):
                zipf.write(maestro_config_path, 'config.yaml')
                print(f"  ✓ Added config: config.yaml")
            
            # Add all flow files
            flows_added = 0
            for root, dirs, files in os.walk(flows_dir):
                for file in files:
                    if file.endswith('.yaml') or file.endswith('.yml'):
                        file_path = os.path.join(root, file)
                        # Preserve directory structure in zip
                        arcname = os.path.relpath(file_path, os.path.dirname(flows_dir))
                        zipf.write(file_path, arcname)
                        flows_added += 1
                        print(f"  ✓ Added flow: {arcname}")
            
            print(f"📦 Created Maestro suite with {flows_added} flow files")
        
        return zip_path
    
    def upload_maestro_suite(self, zip_path: str) -> str:
        """Upload Maestro test suite to BrowserStack"""
        print(f"🧪 Uploading Maestro test suite: {zip_path}")
        
        upload_url = f"{self.api_base_url}/maestro/v2/test-suite"
        
        with open(zip_path, 'rb') as suite_file:
            files = {'file': suite_file}
            headers_without_content_type = {k: v for k, v in self.headers.items() if k != 'Content-Type'}
            
            response = requests.post(upload_url, files=files, headers=headers_without_content_type)
        
        if response.status_code == 200:
            response_data = response.json()
            # Try different possible response field names
            suite_url = response_data.get('test_url') or response_data.get('suite_url') or response_data.get('test_suite_url')
            print(f"✅ Maestro suite uploaded successfully: {suite_url}")
            print(f"📝 Full response: {response_data}")
            return suite_url
        else:
            raise Exception(f"Failed to upload Maestro suite: {response.status_code} - {response.text}")
    
    def execute_maestro_build(self, app_url: str, suite_url: str, 
                             devices: List[Dict[str, str]], 
                             build_name: str = None,
                             tags: List[str] = None) -> str:
        """Execute Maestro tests on BrowserStack devices"""
        
        if build_name is None:
            build_name = f"RN Maestro Tests - {time.strftime('%Y-%m-%d %H:%M:%S')}"
        
        print(f"🚀 Starting Maestro build: {build_name}")
        print(f"📱 Testing on {len(devices)} device(s)")
        
        # Platform-specific execution URLs (default to android for now)
        execute_url = f"{self.api_base_url}/maestro/v2/android/build"
        
        payload = {
            "app": app_url,
            "suite": suite_url,
            "devices": devices,
            "buildName": build_name,
        }
        
        # Add optional parameters
        if tags:
            payload["tags"] = tags
        
        # Add environment variables for Ditto configuration
        env_vars = {}
        if os.getenv('DITTO_APP_ID'):
            env_vars['DITTO_APP_ID'] = os.getenv('DITTO_APP_ID')
        if os.getenv('DITTO_PLAYGROUND_TOKEN'):
            env_vars['DITTO_PLAYGROUND_TOKEN'] = os.getenv('DITTO_PLAYGROUND_TOKEN')
        if os.getenv('GITHUB_TEST_DOC_ID'):
            env_vars['GITHUB_TEST_DOC_ID'] = os.getenv('GITHUB_TEST_DOC_ID')
        if os.getenv('GITHUB_RUN_NUMBER'):
            env_vars['GITHUB_RUN_NUMBER'] = os.getenv('GITHUB_RUN_NUMBER')
        
        if env_vars:
            payload["env"] = env_vars
            print(f"🔧 Environment variables: {list(env_vars.keys())}")
        
        response = requests.post(execute_url, json=payload, headers=self.headers)
        
        if response.status_code == 200:
            build_id = response.json().get('buildId')
            print(f"✅ Maestro build started successfully: {build_id}")
            return build_id
        else:
            raise Exception(f"Failed to execute Maestro build: {response.status_code} - {response.text}")
    
    def get_build_status(self, build_id: str) -> Dict[str, Any]:
        """Get the status of a Maestro build"""
        status_url = f"{self.api_base_url}/maestro/v2/builds/{build_id}"
        
        response = requests.get(status_url, headers=self.headers)
        
        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"Failed to get build status: {response.status_code} - {response.text}")
    
    def wait_for_build_completion(self, build_id: str, timeout: int = 1800) -> Dict[str, Any]:
        """Wait for Maestro build to complete with timeout (default 30 minutes)"""
        print(f"⏳ Waiting for build {build_id} to complete...")
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            status_data = self.get_build_status(build_id)
            status = status_data.get('status', 'unknown')
            
            print(f"📊 Build status: {status}")
            
            if status in ['passed', 'failed', 'stopped']:
                print(f"🏁 Build completed with status: {status}")
                return status_data
            
            time.sleep(30)  # Check every 30 seconds
        
        raise TimeoutError(f"Build {build_id} did not complete within {timeout} seconds")
    
    def get_build_devices(self, build_id: str) -> List[Dict[str, Any]]:
        """Get device results for a build"""
        devices_url = f"{self.api_base_url}/maestro/v2/builds/{build_id}/devices"
        
        response = requests.get(devices_url, headers=self.headers)
        
        if response.status_code == 200:
            return response.json().get('devices', [])
        else:
            raise Exception(f"Failed to get build devices: {response.status_code} - {response.text}")
    
    def print_build_summary(self, build_data: Dict[str, Any], devices: List[Dict[str, Any]]):
        """Print a summary of the build results"""
        print("\n" + "="*60)
        print("📋 BUILD SUMMARY")
        print("="*60)
        
        build_id = build_data.get('id', 'unknown')
        status = build_data.get('status', 'unknown')
        duration = build_data.get('duration', 0)
        
        print(f"Build ID: {build_id}")
        print(f"Status: {status}")
        print(f"Duration: {duration}s")
        print(f"Dashboard: https://app.browserstack.com/app-automate/dashboard/v2/builds/{build_id}")
        
        print(f"\n📱 DEVICE RESULTS ({len(devices)} devices):")
        print("-" * 60)
        
        passed_devices = 0
        failed_devices = 0
        
        for device in devices:
            device_name = device.get('device', 'Unknown Device')
            device_status = device.get('status', 'unknown')
            device_duration = device.get('duration', 0)
            
            status_emoji = "✅" if device_status == "passed" else "❌" if device_status == "failed" else "⚠️"
            print(f"{status_emoji} {device_name}: {device_status} ({device_duration}s)")
            
            if device_status == "passed":
                passed_devices += 1
            elif device_status == "failed":
                failed_devices += 1
        
        print("-" * 60)
        print(f"✅ Passed: {passed_devices}")
        print(f"❌ Failed: {failed_devices}")
        print("="*60)


def main():
    """Main execution function"""
    
    # Configuration
    PROJECT_ROOT = os.getenv('GITHUB_WORKSPACE', os.getcwd())
    APP_TYPE = os.getenv('APP_TYPE', 'expo')  # 'expo' or 'bare'
    
    if APP_TYPE == 'expo':
        app_dir = os.path.join(PROJECT_ROOT, 'react-native-expo')
        maestro_dir = os.path.join(app_dir, '.maestro')
        app_path = os.path.join(app_dir, 'android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    else:  # bare
        app_dir = os.path.join(PROJECT_ROOT, 'react-native')
        maestro_dir = os.path.join(app_dir, '.maestro')
        app_path = os.path.join(app_dir, 'android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
    
    maestro_config_path = os.path.join(maestro_dir, 'config.yaml')
    maestro_flows_dir = os.path.join(maestro_dir, 'flows')
    
    # Validate required files exist
    if not os.path.exists(app_path):
        raise FileNotFoundError(f"App APK not found: {app_path}")
    
    if not os.path.exists(maestro_config_path):
        raise FileNotFoundError(f"Maestro config not found: {maestro_config_path}")
    
    if not os.path.exists(maestro_flows_dir):
        raise FileNotFoundError(f"Maestro flows directory not found: {maestro_flows_dir}")
    
    # Define test devices for both Android and iOS
    android_devices = [
        {"device": "Google Pixel 7", "osVersion": "13.0"},
        {"device": "Samsung Galaxy S23", "osVersion": "13.0"},
        {"device": "OnePlus 11", "osVersion": "13.0"},
    ]
    
    ios_devices = [
        {"device": "iPhone 14", "osVersion": "16"},
        {"device": "iPhone 15", "osVersion": "17"},
        {"device": "iPad Pro 12.9 2022", "osVersion": "16"},
    ]
    
    # Use appropriate devices based on platform
    platform_type = os.getenv('PLATFORM_TYPE', 'android')  # android, ios, or both
    
    if platform_type == 'ios':
        test_devices = ios_devices
    elif platform_type == 'both':
        test_devices = android_devices + ios_devices
    else:
        test_devices = android_devices
    
    # Get build name from environment or generate one
    build_name = os.getenv('GITHUB_RUN_NUMBER', None)
    if build_name:
        app_name = 'RN Expo' if APP_TYPE == 'expo' else 'RN Bare'
        build_name = f"{app_name} Maestro Tests - Run #{build_name}"
    
    # Define test tags
    test_tags = ["smoke", "core"]  # Run smoke and core tests
    
    try:
        print(f"🚀 Starting BrowserStack Maestro tests for {APP_TYPE} app")
        print(f"📁 Project root: {PROJECT_ROOT}")
        print(f"📱 App path: {app_path}")
        print(f"🧪 Maestro config: {maestro_config_path}")
        print(f"📂 Maestro flows: {maestro_flows_dir}")
        
        # Initialize runner
        runner = BrowserStackMaestroRunner()
        
        # Upload app
        app_url = runner.upload_app(app_path)
        
        # Create and upload Maestro suite
        suite_zip_path = runner.create_maestro_suite_zip(maestro_config_path, maestro_flows_dir)
        suite_url = runner.upload_maestro_suite(suite_zip_path)
        
        # Execute tests
        build_id = runner.execute_maestro_build(
            app_url=app_url,
            suite_url=suite_url,
            devices=test_devices,
            build_name=build_name,
            tags=test_tags
        )
        
        # Wait for completion
        build_data = runner.wait_for_build_completion(build_id)
        
        # Get device results
        devices = runner.get_build_devices(build_id)
        
        # Print summary
        runner.print_build_summary(build_data, devices)
        
        # Clean up temporary files
        os.unlink(suite_zip_path)
        
        # Exit with appropriate code
        if build_data.get('status') == 'passed':
            print("\n🎉 All tests passed!")
            sys.exit(0)
        else:
            print("\n💥 Some tests failed!")
            sys.exit(1)
            
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    main()