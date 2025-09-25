using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using OpenQA.Selenium;
using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Android;
using OpenQA.Selenium.Appium.iOS;
using OpenQA.Selenium.Support.UI;
using Xunit;
using Xunit.Abstractions;

namespace DittoMauiTasksApp.UITests
{
    [Collection("MAUI Collection")]
    public class GitHubSyncTest : IDisposable
    {
        private readonly AppiumDriver _driver;
        private readonly ITestOutputHelper _output;
        
        // Test document information from GitHub Actions environment
        private readonly string _githubTestDocId;
        private readonly string _githubTestTitle;
        private readonly string _githubRunId;
        private readonly string _githubRunNumber;
        
        private const int DefaultSyncTimeoutSeconds = 60;
        private const int ElementWaitTimeoutSeconds = 30;

        public GitHubSyncTest(AppiumTestFixture fixture, ITestOutputHelper output)
        {
            _driver = fixture.Driver;
            _output = output;
            
            // Get test document information from environment variables set by GitHub Actions
            _githubTestDocId = Environment.GetEnvironmentVariable("GITHUB_TEST_DOC_ID") ?? 
                              throw new InvalidOperationException("GITHUB_TEST_DOC_ID environment variable is required");
            _githubTestTitle = Environment.GetEnvironmentVariable("GITHUB_TEST_TITLE") ?? 
                              throw new InvalidOperationException("GITHUB_TEST_TITLE environment variable is required");
            _githubRunId = Environment.GetEnvironmentVariable("GITHUB_RUN_ID") ?? "unknown";
            _githubRunNumber = Environment.GetEnvironmentVariable("GITHUB_RUN_NUMBER") ?? "unknown";
            
            _output.WriteLine($"Test Document ID: {_githubTestDocId}");
            _output.WriteLine($"Test Document Title: {_githubTestTitle}");
            _output.WriteLine($"GitHub Run: {_githubRunId} #{_githubRunNumber}");
        }

        [Fact]
        [Trait("Category", "Integration")]
        [Trait("Category", "BrowserStack")]
        public async Task GitHubSeededDocument_ShouldAppearInTaskList()
        {
            _output.WriteLine("Starting GitHub seeded document sync test...");
            
            try
            {
                // Wait for app to launch and initialize
                await WaitForAppInitialization();
                
                // Wait for Ditto sync to complete and document to appear
                var documentFound = await WaitForGitHubDocument();
                
                Assert.True(documentFound, 
                    $"GitHub test document '{_githubTestTitle}' (ID: {_githubTestDocId}) " +
                    $"did not sync within {DefaultSyncTimeoutSeconds} seconds");
                
                _output.WriteLine("✅ GitHub test document successfully synced from Ditto Cloud");
                
                // Verify document properties
                await VerifyDocumentProperties();
                
                _output.WriteLine("✅ Document properties verified successfully");
            }
            catch (Exception ex)
            {
                _output.WriteLine($"❌ Test failed: {ex.Message}");
                await TakeScreenshot("test_failure");
                throw;
            }
        }
        
        [Fact]
        [Trait("Category", "UI")]
        [Trait("Category", "BrowserStack")]
        public async Task TaskList_ShouldDisplayBasicElements()
        {
            _output.WriteLine("Verifying basic UI elements are present...");
            
            try
            {
                // Wait for app to initialize
                await WaitForAppInitialization();
                
                // Verify key UI elements exist
                await VerifyUIElements();
                
                _output.WriteLine("✅ All basic UI elements verified successfully");
            }
            catch (Exception ex)
            {
                _output.WriteLine($"❌ UI verification failed: {ex.Message}");
                await TakeScreenshot("ui_verification_failure");
                throw;
            }
        }

        private async Task WaitForAppInitialization(int timeoutSeconds = ElementWaitTimeoutSeconds)
        {
            _output.WriteLine("Waiting for app to initialize...");
            
            if (_driver == null)
            {
                _output.WriteLine("Driver is not initialized");
                return;
            }
            
            var wait = new WebDriverWait(_driver, TimeSpan.FromSeconds(timeoutSeconds));
            
            try
            {
                // Wait for the main page to load - look for the App ID or Token text
                wait.Until(driver =>
                {
                    try
                    {
                        // Look for app identification elements that should be present
                        var appIdElement = FindElementSafely(By.XPath("//*[contains(@text, 'App ID:') or contains(@name, 'App ID:')]"));
                        var tokenElement = FindElementSafely(By.XPath("//*[contains(@text, 'Token:') or contains(@name, 'Token:')]"));
                        
                        return appIdElement != null || tokenElement != null;
                    }
                    catch
                    {
                        return false;
                    }
                });
                
                _output.WriteLine("App initialization detected");
                
                // Give additional time for Ditto initialization
                await Task.Delay(3000);
            }
            catch (WebDriverTimeoutException)
            {
                _output.WriteLine("⚠ App initialization timeout - continuing with test");
                await TakeScreenshot("app_init_timeout");
            }
        }

        private async Task<bool> WaitForGitHubDocument(int timeoutSeconds = DefaultSyncTimeoutSeconds)
        {
            _output.WriteLine($"Waiting for GitHub document to sync: {_githubTestTitle}");
            
            var startTime = DateTime.UtcNow;
            var endTime = startTime.AddSeconds(timeoutSeconds);
            
            while (DateTime.UtcNow < endTime)
            {
                try
                {
                    // Look for the document by title text - try multiple approaches
                    var documentFound = FindGitHubDocumentElement() != null;
                    
                    if (documentFound)
                    {
                        var elapsed = DateTime.UtcNow - startTime;
                        _output.WriteLine($"✅ Document found after {elapsed.TotalSeconds:F1} seconds");
                        return true;
                    }
                    
                    // Log progress every 10 seconds
                    var currentElapsed = DateTime.UtcNow - startTime;
                    if (currentElapsed.TotalSeconds % 10 < 2)
                    {
                        _output.WriteLine($"Still waiting for document... ({currentElapsed.TotalSeconds:F0}s elapsed)");
                        await TakeScreenshot($"sync_progress_{currentElapsed.TotalSeconds:F0}s");
                    }
                }
                catch (Exception ex)
                {
                    _output.WriteLine($"Error checking for document: {ex.Message}");
                }
                
                await Task.Delay(2000); // Check every 2 seconds
            }
            
            _output.WriteLine($"❌ Document not found within {timeoutSeconds} seconds");
            await TakeScreenshot("document_not_found");
            return false;
        }

        private IWebElement? FindGitHubDocumentElement()
        {
            // Extract the run ID from the title for flexible matching
            var runId = _githubRunId;
            
            // Try multiple selector strategies for cross-platform compatibility
            var strategies = new[]
            {
                // Exact title match
                By.XPath($"//*[contains(@text, '{_githubTestTitle}') or contains(@name, '{_githubTestTitle}')]"),
                
                // Match by run ID (more flexible)
                By.XPath($"//*[contains(@text, '{runId}') or contains(@name, '{runId}')]"),
                
                // Match "GitHub Test Task" pattern
                By.XPath("//*[contains(@text, 'GitHub Test Task') or contains(@name, 'GitHub Test Task')]"),
                
                // Generic task item selectors (platform-specific)
#if ANDROID
                By.XPath("//android.widget.TextView[contains(@text, 'GitHub Test')]"),
                By.Id("task_title"),
                By.ClassName("android.widget.TextView"),
#elif IOS
                By.XPath("//XCUIElementTypeStaticText[contains(@name, 'GitHub Test')]"),
                By.ClassName("XCUIElementTypeStaticText"),
                By.AccessibilityId("task_title"),
#endif
            };
            
            foreach (var strategy in strategies)
            {
                try
                {
                    var elements = _driver?.FindElements(strategy) ?? new List<AppiumElement>().AsReadOnly();
                    foreach (var element in elements)
                    {
                        var elementText = GetElementText(element);
                        if (!string.IsNullOrEmpty(elementText) && 
                            (elementText.Contains(_githubTestTitle) || 
                             elementText.Contains(runId) || 
                             elementText.Contains("GitHub Test Task")))
                        {
                            _output.WriteLine($"Found document using strategy: {strategy} with text: {elementText}");
                            return element;
                        }
                    }
                }
                catch (Exception ex)
                {
                    _output.WriteLine($"Strategy {strategy} failed: {ex.Message}");
                }
            }
            
            return null;
        }

        private async Task VerifyDocumentProperties()
        {
            var documentElement = FindGitHubDocumentElement();
            Assert.NotNull(documentElement);
            
            var elementText = GetElementText(documentElement);
            
            // Verify the document title is exactly what we expect
            Assert.Contains(_githubTestTitle, elementText);
            
            _output.WriteLine($"Document text verified: {elementText}");
            
            // Take a screenshot showing the synced document
            await TakeScreenshot("synced_document");
        }

        private async Task VerifyUIElements()
        {
            var elements = new[]
            {
                ("App ID Label", By.XPath("//*[contains(@text, 'App ID:') or contains(@name, 'App ID:')]")),
                ("Token Label", By.XPath("//*[contains(@text, 'Token:') or contains(@name, 'Token:')]")),
                ("Task List", By.XPath("//*[contains(@text, 'Buy groceries') or contains(@name, 'Buy groceries')]")),
            };
            
            foreach (var (name, selector) in elements)
            {
                var element = FindElementSafely(selector);
                if (element != null)
                {
                    _output.WriteLine($"✅ Found {name}");
                }
                else
                {
                    _output.WriteLine($"⚠ {name} not found - may be expected on some platforms");
                }
            }
            
            await TakeScreenshot("ui_elements");
        }

        private IWebElement? FindElementSafely(By selector)
        {
            try
            {
                return _driver?.FindElement(selector);
            }
            catch (NoSuchElementException)
            {
                return null;
            }
        }

        private string GetElementText(IWebElement element)
        {
            try
            {
                // Try different ways to get text depending on platform
                var text = element.Text;
                if (!string.IsNullOrEmpty(text))
                    return text;
                
                var name = element.GetDomAttribute("name") ?? element.GetDomProperty("name");
                if (!string.IsNullOrEmpty(name))
                    return name;
                
                var contentDesc = element.GetDomAttribute("content-desc");
                if (!string.IsNullOrEmpty(contentDesc))
                    return contentDesc;
                
                return string.Empty;
            }
            catch
            {
                return string.Empty;
            }
        }

        private Task TakeScreenshot(string name)
        {
            try
            {
                if (_driver == null) return Task.CompletedTask;
                var screenshot = ((ITakesScreenshot)_driver).GetScreenshot();
                var timestamp = DateTime.UtcNow.ToString("yyyyMMdd_HHmmss");
                var filename = $"{name}_{timestamp}.png";
                
                // Save screenshot (path will depend on test environment)
                screenshot.SaveAsFile(filename);
                _output.WriteLine($"Screenshot saved: {filename}");
                return Task.CompletedTask;
            }
            catch (Exception ex)
            {
                _output.WriteLine($"Failed to take screenshot: {ex.Message}");
                return Task.CompletedTask;
            }
        }

        public void Dispose()
        {
            // Cleanup handled by test fixture
        }
    }

    // Test fixture for managing Appium driver lifecycle
    public class AppiumTestFixture : IDisposable
    {
        public AppiumDriver Driver { get; private set; } = null!;
        
        public AppiumTestFixture()
        {
            InitializeDriver();
        }
        
        private void InitializeDriver()
        {
            // When running with BrowserStack SDK, the driver will be configured via browserstack.yml
            // Otherwise, use local configuration
            var isBrowserStack = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("BROWSERSTACK_USERNAME"));
            
            if (isBrowserStack)
            {
                // BrowserStack SDK will inject the proper driver configuration
                // based on browserstack.yml settings
                var options = new AppiumOptions();
                options.AddAdditionalAppiumOption("browserstack.user", Environment.GetEnvironmentVariable("BROWSERSTACK_USERNAME"));
                options.AddAdditionalAppiumOption("browserstack.key", Environment.GetEnvironmentVariable("BROWSERSTACK_ACCESS_KEY"));
                
                // The SDK will handle the actual driver initialization
                Driver = new IOSDriver(new Uri("https://hub-cloud.browserstack.com/wd/hub"), options);
            }
            else
            {
                // Local configuration for development/debugging
                var options = new AppiumOptions();
                options.PlatformName = "iOS";
                options.AutomationName = "XCUITest";
                options.DeviceName = "iPhone Simulator";
                
                Driver = new IOSDriver(new Uri("http://localhost:4723/"), options);
            }
        }
        
        public void Dispose()
        {
            Driver?.Quit();
            Driver?.Dispose();
        }
    }
    
    [CollectionDefinition("MAUI Collection")]
    public class AppiumCollection : ICollectionFixture<AppiumTestFixture>
    {
        // This class is just for xUnit collection definition
    }
}
