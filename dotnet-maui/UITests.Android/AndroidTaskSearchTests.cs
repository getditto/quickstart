using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Android;
using UITests.Shared;

namespace UITests.Android;

public class AndroidTaskSearchTests : TaskSearchTests
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
            // BrowserStack capabilities - match android-cpp pattern exactly
            options.PlatformName = "Android";
            options.AutomationName = "UiAutomator2";
            options.DeviceName = "Google Pixel 7";
            options.PlatformVersion = "13.0";
            options.App = browserstackApp ?? GetAppPath();
            options.AddAdditionalAppiumOption("project", "Ditto .NET MAUI");
            options.AddAdditionalAppiumOption("build", "Appium E2E Tests");
            options.AddAdditionalAppiumOption("name", "Task Search Tests");

            var uri = new Uri($"https://{browserstackUsername}:{browserstackAccessKey}@hub-cloud.browserstack.com/wd/hub");
            return new AndroidDriver(uri, options);
        }
        else
        {
            // Local testing capabilities
            options.PlatformName = "Android";
            options.AutomationName = "UIAutomator2";
            options.App = GetAppPath();

            // Optional capabilities for local testing
            options.AddAdditionalAppiumOption("appWaitActivity", "crc647fcdc6dfabca042e.MainActivity");
            options.AddAdditionalAppiumOption("newCommandTimeout", 300);
            options.AddAdditionalAppiumOption("autoGrantPermissions", true);

            return new AndroidDriver(new Uri("http://127.0.0.1:4723"), options);
        }
    }

    private string GetAppPath()
    {
        var projectRoot = Path.GetFullPath(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", ".."));
        var apkFileName = "live.ditto.quickstart.mauitasksapp-Signed.apk";

        // Look for APK in release directory first (what we just built)
        var releasePath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Release", "net9.0-android", apkFileName);
        if (File.Exists(releasePath))
        {
            return releasePath;
        }

        var debugPath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Debug", "net9.0-android", apkFileName);
        if (File.Exists(debugPath))
        {
            return debugPath;
        }

        throw new FileNotFoundException($"APK not found. Checked:\n- {debugPath}\n- {releasePath}");
    }
}