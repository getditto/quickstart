using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.iOS;
using UITests.Shared;
using System.Diagnostics;
using System.Text.RegularExpressions;

namespace UITests.iOS;

public class iOSTaskSearchTests : TaskSearchTests
{
    protected override AppiumDriver CreateDriver()
    {
        var options = new AppiumOptions();

        // Check if running on BrowserStack
        var browserstackUsername = Environment.GetEnvironmentVariable("BROWSERSTACK_USERNAME");
        var browserstackAccessKey = Environment.GetEnvironmentVariable("BROWSERSTACK_ACCESS_KEY");
        var browserstackApp = Environment.GetEnvironmentVariable("BROWSERSTACK_APP_ID");

        if (!string.IsNullOrEmpty(browserstackUsername) && !string.IsNullOrEmpty(browserstackAccessKey))
        {
            // Load device config from environment (set by workflow from browserstack-devices.json)
            var deviceString = Environment.GetEnvironmentVariable("BROWSERSTACK_DEVICE") ?? "iPhone 15-17.0";
            var deviceParts = deviceString.Split('-');
            var deviceName = deviceParts[0];
            var platformVersion = deviceParts.Length > 1 ? deviceParts[1] : "17.0";

            // BrowserStack capabilities for iOS
            options.PlatformName = "iOS";
            options.AutomationName = "XCUITest";
            options.DeviceName = deviceName;
            options.PlatformVersion = platformVersion;
            options.App = browserstackApp ?? GetAppPath();
            options.AddAdditionalAppiumOption("project", "QuickStart .NET MAUI");
            options.AddAdditionalAppiumOption("build", Environment.GetEnvironmentVariable("BUILD_NAME") ?? "Local Tests");
            options.AddAdditionalAppiumOption("name", "iOS Task Search Tests");
            options.AddAdditionalAppiumOption("autoGrantPermissions", true);

            var uri = new Uri($"https://{browserstackUsername}:{browserstackAccessKey}@hub-cloud.browserstack.com/wd/hub");
            return new IOSDriver(uri, options);
        }
        else
        {
            // Local testing capabilities
            options.PlatformName = "iOS";
            options.AutomationName = "XCUITest";

            // App capabilities - use the built .app
            options.App = Path.Combine(GetAppPath(), "DittoMauiTasksApp.app");

            // iOS Simulator capabilities - automatically detect booted simulator
            var (deviceName, platformVersion) = GetFirstBootedSimulator();
            options.DeviceName = deviceName;
            options.PlatformVersion = platformVersion;
            options.AddAdditionalAppiumOption("newCommandTimeout", 300);
            return new IOSDriver(new Uri("http://127.0.0.1:4723"), options);
        }
    }

    private string GetAppPath()
    {
        // Look for the .app in the MAUI project's build output
        var projectRoot = Path.GetFullPath(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", ".."));
        var appPath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Debug", "net9.0-ios", "iossimulator-arm64");

        if (!Directory.Exists(appPath))
        {
            // Fallback to x64 simulator
            appPath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Debug", "net9.0-ios", "iossimulator-x64");
        }

        return appPath;
    }

    private static (string DeviceName, string PlatformVersion) GetFirstBootedSimulator()
    {
        var process = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = "xcrun",
                Arguments = "simctl list devices",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                CreateNoWindow = true
            }
        };

        process.Start();
        var output = process.StandardOutput.ReadToEnd();
        process.WaitForExit();

        // Parse output to find first booted device
        var lines = output.Split('\n');
        string? currentRuntime = null;

        foreach (var line in lines)
        {
            // Check if line contains runtime version (e.g. "-- iOS 18.6 --")
            var runtimeMatch = Regex.Match(line, @"-- iOS ([\d.]+) --");
            if (runtimeMatch.Success)
            {
                currentRuntime = runtimeMatch.Groups[1].Value;
                continue;
            }

            // Check if line contains a booted device
            if (line.Contains("(Booted)") && currentRuntime != null)
            {
                // Extract device name (e.g. "iPhone 16 (8E5063C4-E64F-46BD-B445-1B22F85B3310) (Booted)")
                var deviceMatch = Regex.Match(line, @"^\s+([^(]+)\s+\([^)]+\)\s+\(Booted\)");
                if (deviceMatch.Success)
                {
                    var deviceName = deviceMatch.Groups[1].Value.Trim();
                    return (deviceName, currentRuntime);
                }
            }
        }

        // Fallback if no booted simulator found
        throw new InvalidOperationException("No booted iOS simulators found. Please boot a simulator first.");
    }
}