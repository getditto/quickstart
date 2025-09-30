using NUnit.Framework;
using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Android;
using OpenQA.Selenium.Appium.iOS;
using System.Reflection;

namespace UITests.Shared;

public abstract class BaseTest
{
    protected AppiumDriver App { get; set; } = null!;
    private static bool _assemblyResolveHooked = false;

    protected abstract AppiumDriver CreateDriver();

    [OneTimeSetUp]
    public void OneTimeSetUp()
    {
        if (!_assemblyResolveHooked)
        {
            AppDomain.CurrentDomain.AssemblyResolve += ResolveAppiumAssembly;
            _assemblyResolveHooked = true;
        }
    }

    private static Assembly? ResolveAppiumAssembly(object? sender, ResolveEventArgs args)
    {
        var assemblyName = new AssemblyName(args.Name);

        if (assemblyName.Name == "Appium.Net")
        {
            var nugetPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                ".nuget", "packages", "appium.webdriver", "5.2.0", "lib", "net6.0", "Appium.Net.dll");

            if (File.Exists(nugetPath))
            {
                return Assembly.LoadFrom(nugetPath);
            }

            var localPath = Path.Combine(AppContext.BaseDirectory, "Appium.Net.dll");
            if (File.Exists(localPath))
            {
                return Assembly.LoadFrom(localPath);
            }
        }

        return null;
    }

    [SetUp]
    public void SetUp()
    {
        App = CreateDriver();

        // Handle permission dialogs on app startup
        HandlePermissionDialogs();
    }

    private void HandlePermissionDialogs()
    {
        try
        {
            Thread.Sleep(2000);
            var isIOS = App.GetType().Name.Contains("IOSDriver");

            if (isIOS)
            {
                var iOSButtons = new[] { "Allow", "OK", "Allow While Using App", "Allow Once" };

                for (int dialogRound = 1; dialogRound <= 2; dialogRound++)
                {
                    foreach (var buttonText in iOSButtons)
                    {
                        try
                        {
                            var allowButton = App.FindElement(MobileBy.XPath($"//XCUIElementTypeButton[@name='{buttonText}']"));
                            if (allowButton.Displayed)
                            {
                                allowButton.Click();
                                Thread.Sleep(3000);
                                break;
                            }
                        }
                        catch
                        {
                        }
                    }
                    Thread.Sleep(1000);
                }
            }
            else
            {
                var androidButtons = new[] { "Allow", "ALLOW", "OK", "Accept" };

                foreach (var buttonText in androidButtons)
                {
                    try
                    {
                        var allowButton = App.FindElement(MobileBy.XPath($"//android.widget.Button[@text='{buttonText}']"));
                        if (allowButton.Displayed)
                        {
                            allowButton.Click();
                            Thread.Sleep(1000);
                        }
                    }
                    catch
                    {
                    }
                }
            }
        }
        catch
        {
        }
    }

    [TearDown]
    public void TearDown()
    {
        App?.Quit();
    }
}