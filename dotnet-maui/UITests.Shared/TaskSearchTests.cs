using NUnit.Framework;
using OpenQA.Selenium;
using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Support.UI;

namespace UITests.Shared;

[TestFixture]
public abstract class TaskSearchTests : BaseTest
{
    [Test]
    public void CanFindTaskByTitle()
    {
        var expectedTaskTitle = Environment.GetEnvironmentVariable("EXPECTED_TASK_TITLE");

        if (string.IsNullOrEmpty(expectedTaskTitle))
        {
            Assert.Fail("EXPECTED_TASK_TITLE environment variable is not set or empty");
            return;
        }

        var isBrowserStack = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("BROWSERSTACK_USERNAME"));

        try
        {
            // Wait for app to load and sync - longer for BrowserStack
            var waitTime = isBrowserStack ? 15000 : 6000;
            Thread.Sleep(waitTime);

            var taskFound = false;

            for (int attempt = 1; attempt <= 10; attempt++)
            {
                try
                {
                    // Find all TaskTitleLabel elements using AutomationId (works on both iOS and Android)
                    var taskLabels = App.FindElements(MobileBy.Id("TaskTitleLabel"));

                    foreach (var label in taskLabels)
                    {
                        try
                        {
                            if (label.Text.Contains(expectedTaskTitle) && label.Displayed)
                            {
                                taskFound = true;
                                break;
                            }
                        }
                        catch
                        {
                            // Label not accessible, continue
                        }
                    }

                    if (taskFound)
                        break;
                }
                catch
                {
                    Thread.Sleep(1000);
                }
            }

            if (!taskFound)
            {
                if (isBrowserStack)
                {
                    App.ExecuteScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"Task not found\"}}");
                }
                Assert.Fail($"Task '{expectedTaskTitle}' not found");
            }

            if (isBrowserStack)
            {
                App.ExecuteScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \"Task found successfully\"}}");
            }
        }
        catch (Exception ex)
        {
            if (isBrowserStack)
            {
                App.ExecuteScript($"browserstack_executor: {{\"action\": \"setSessionStatus\", \"arguments\": {{\"status\": \"failed\", \"reason\": \"{ex.Message}\"}}}}");
            }
            Assert.Fail($"Test failed: {ex.Message}");
        }
    }
}