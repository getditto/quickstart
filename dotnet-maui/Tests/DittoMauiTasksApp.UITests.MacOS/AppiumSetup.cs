using NUnit.Framework;

using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Enums;
using OpenQA.Selenium.Appium.Mac;

namespace UITests;

[SetUpFixture]
public class AppiumSetup
{
	private static MacDriver? _driver;
	private const string AppId = "live.ditto.quickstart.mauitasksapp";

	public static AppiumDriver App => _driver ?? throw new NullReferenceException("AppiumDriver is null");

	static AppiumSetup()
	{
		// Register cleanup on process exit
		AppDomain.CurrentDomain.ProcessExit += (sender, e) => CleanupDriver();
	}

	[OneTimeSetUp]
	public void RunBeforeAnyTests()
	{
		// If you started an Appium server manually, make sure to comment out the next line
		// This line starts a local Appium server for you as part of the test run
		AppiumServerHelper.StartAppiumLocalServer();

		var macOptions = new AppiumOptions
		{
			// Specify mac2 as the driver, typically don't need to change this
			AutomationName = "mac2",
			// Always Mac for Mac
			PlatformName = "mac",
			// The full path to the .app file to test
			App = GetAppPath(),
		};

		// Setting the Bundle ID is required, else the automation will run on Finder
		macOptions.AddAdditionalAppiumOption(IOSMobileCapabilityType.BundleId, AppId);
		
		// Add settings to ensure proper cleanup
		macOptions.AddAdditionalAppiumOption("autoAcceptAlerts", true);
		macOptions.AddAdditionalAppiumOption("autoDismissAlerts", true);
		macOptions.AddAdditionalAppiumOption("shouldTerminateApp", true);
		macOptions.AddAdditionalAppiumOption("shouldCloseApp", true);
		
		// Add XCUITest-specific settings for better cleanup
		macOptions.AddAdditionalAppiumOption("webDriverAgentUrl", "");
		macOptions.AddAdditionalAppiumOption("derivedDataPath", "");
		macOptions.AddAdditionalAppiumOption("useNewWDA", true);
		macOptions.AddAdditionalAppiumOption("wdaLocalPort", 8100);
		macOptions.AddAdditionalAppiumOption("showXcodeLog", false);
		macOptions.AddAdditionalAppiumOption("showIOSLog", false);

		// Note there are many more options that you can use to influence the app under test according to your needs
		_driver = new MacDriver(macOptions);
	}

	[OneTimeTearDown]
	public void RunAfterAnyTests()
	{
		try
		{
			// Close the app and quit the driver
			if (_driver != null)
			{
				// Use Appium's proper cleanup sequence
				_driver.CloseApp();
				_driver.Quit();
				_driver.Dispose();
				_driver = null;
			}
		}
		catch (Exception ex)
		{
			// Log any cleanup errors but don't throw
			Console.WriteLine($"Error during driver cleanup: {ex.Message}");
		}
		finally
		{
			// Always clean up the Appium server
			try
			{
				// Give the driver a moment to fully close
				Thread.Sleep(1000);
				AppiumServerHelper.DisposeAppiumLocalServer();
			}
			catch (Exception ex)
			{
				Console.WriteLine($"Error during server cleanup: {ex.Message}");
			}
		}
	}

	/// <summary>
	/// Static cleanup method that can be called from the static constructor
	/// </summary>
	private static void CleanupDriver()
	{
		try
		{
			if (_driver != null)
			{
				// Use Appium's proper cleanup sequence
				_driver.CloseApp();
				_driver.Quit();
				_driver.Dispose();
				_driver = null;
			}
			AppiumServerHelper.DisposeAppiumLocalServer();
		}
		catch (Exception ex)
		{
			Console.WriteLine($"Error during static driver cleanup: {ex.Message}");
		}
	}

	/// <summary>
	/// Gets the path to the MAUI app from environment variable or falls back to default path.
	/// </summary>
	/// <returns>The full path to the .app file for testing.</returns>
	private static string GetAppPath()
	{
		// Try to get the path from the environment variable
		var basePath = Environment.GetEnvironmentVariable("DittoQuickstartMauiPath");
		
		if (string.IsNullOrEmpty(basePath))
		{
			// Fallback to default path if environment variable is not set
			basePath = "~/Developer/ditto/quickstart/dotnet-maui/DittoMauiTasksApp";
			basePath = ExpandTilde(basePath);
		} else {
			// If environment variable is set, append the project path
			basePath = $"{basePath}/dotnet-maui/DittoMauiTasksApp";
		}

		// Construct the full path to the .app file (no leading slashes)
		var directory = Path.Combine(basePath, "bin", "Debug", "net9.0-maccatalyst", "maccatalyst-arm64");
		var appPath = Path.Combine(directory, "DittoMauiTasksApp.app");
		
		// Verify the path exists
		if (!Directory.Exists(directory))
		{
			throw new DirectoryNotFoundException($"Directory not found: {directory}. Please ensure the environment variable 'DittoQuickstartMauiPath' is set correctly or the app has been built.");
		}
		
		if (!Directory.Exists(appPath))
		{
			throw new DirectoryNotFoundException($"App not found: {appPath}. Please ensure the app has been built.");
		}
		
		return appPath;
	}

	/// <summary>
	/// Expands the tilde (~) to the user's home directory path.
	/// </summary>
	/// <param name="path">The path that may contain a tilde.</param>
	/// <returns>The path with tilde expanded to the full home directory path.</returns>
	private static string ExpandTilde(string path)
	{
		if (string.IsNullOrEmpty(path))
			return path;

		if (path.StartsWith("~/"))
		{
			var homeDirectory = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
			return Path.Combine(homeDirectory, path.Substring(2));
		}

		if (path == "~")
		{
			return Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
		}

		return path;
	}
}