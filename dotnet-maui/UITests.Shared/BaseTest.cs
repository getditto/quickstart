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
        // Hook AssemblyResolve to handle missing Appium.Net.dll
        if (!_assemblyResolveHooked)
        {
            AppDomain.CurrentDomain.AssemblyResolve += ResolveAppiumAssembly;
            _assemblyResolveHooked = true;
            Console.WriteLine("AssemblyResolve hook registered for Appium dependencies");
        }
    }

    private static Assembly? ResolveAppiumAssembly(object? sender, ResolveEventArgs args)
    {
        var assemblyName = new AssemblyName(args.Name);
        Console.WriteLine($"AssemblyResolve: Looking for {assemblyName.Name}");

        // Handle Appium.Net assembly specifically
        if (assemblyName.Name == "Appium.Net")
        {
            // Look in NuGet packages folder
            var nugetPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                ".nuget", "packages", "appium.webdriver", "5.2.0", "lib", "net6.0", "Appium.Net.dll");

            if (File.Exists(nugetPath))
            {
                Console.WriteLine($"✓ Loading Appium.Net.dll from: {nugetPath}");
                return Assembly.LoadFrom(nugetPath);
            }

            // Fallback: look in current directory
            var localPath = Path.Combine(AppContext.BaseDirectory, "Appium.Net.dll");
            if (File.Exists(localPath))
            {
                Console.WriteLine($"✓ Loading Appium.Net.dll from: {localPath}");
                return Assembly.LoadFrom(localPath);
            }

            Console.WriteLine($"❌ Could not find Appium.Net.dll in expected locations");
        }

        return null;
    }

    [SetUp]
    public void SetUp()
    {
        App = CreateDriver();
    }

    [TearDown]
    public void TearDown()
    {
        App?.Quit();
    }
}