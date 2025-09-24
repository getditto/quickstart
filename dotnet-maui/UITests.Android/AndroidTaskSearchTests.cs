using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Android;
using UITests.Shared;

namespace UITests.Android;

public class AndroidTaskSearchTests : TaskSearchTests
{
    protected override AppiumDriver CreateDriver()
    {
        var options = new AppiumOptions();

        // Platform capabilities
        options.PlatformName = "Android";
        options.AutomationName = "UIAutomator2";

        // App capabilities - use the built APK
        options.App = GetAppPath();

        // Optional capabilities for local testing
        options.AddAdditionalAppiumOption("appWaitActivity", "crc647fcdc6dfabca042e.MainActivity");
        options.AddAdditionalAppiumOption("newCommandTimeout", 300);
        options.AddAdditionalAppiumOption("autoGrantPermissions", true);

        return new AndroidDriver(new Uri("http://127.0.0.1:4723"), options);
    }

    private string GetAppPath()
    {
        var projectRoot = Path.GetFullPath(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", ".."));
        var apkFileName = "live.ditto.quickstart.mauitasksapp-Signed.apk";

        // Look for APK in release directory first (what we just built)
        var releasePath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Release", "net9.0-android", apkFileName);
        if (File.Exists(releasePath))
        {
            Console.WriteLine($"Found APK at: {releasePath}");
            return releasePath;
        }

        // Fallback to debug directory
        var debugPath = Path.Combine(projectRoot, "DittoMauiTasksApp", "bin", "Debug", "net9.0-android", apkFileName);
        if (File.Exists(debugPath))
        {
            Console.WriteLine($"Found APK at: {debugPath}");
            return debugPath;
        }

        throw new FileNotFoundException($"APK not found. Checked:\n- {debugPath}\n- {releasePath}");
    }
}