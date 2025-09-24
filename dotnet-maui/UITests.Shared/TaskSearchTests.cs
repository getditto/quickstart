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
        // Get the expected task title from environment variable
        var expectedTaskTitle = Environment.GetEnvironmentVariable("EXPECTED_TASK_TITLE");

        if (string.IsNullOrEmpty(expectedTaskTitle))
        {
            Assert.Fail("EXPECTED_TASK_TITLE environment variable is not set or empty");
            return;
        }

        Console.WriteLine($"Looking for task with title: {expectedTaskTitle}");

        // Wait for the tasks list to be visible
        var wait = new WebDriverWait(App, TimeSpan.FromSeconds(30));

        try
        {
            var tasksList = wait.Until(driver =>
                driver.FindElement(MobileBy.Id("TasksList")));

            Console.WriteLine("Tasks list found, waiting for sync...");

            // Wait a bit for sync to complete
            Thread.Sleep(3000);

            // Look for task title labels within the tasks list
            var taskFound = false;
            var maxAttempts = 10;
            var attempt = 0;

            while (!taskFound && attempt < maxAttempts)
            {
                try
                {
                    // Find all task title labels
                    var taskLabels = App.FindElements(MobileBy.Id("TaskTitleLabel"));

                    Console.WriteLine($"Found {taskLabels.Count} task labels on attempt {attempt + 1}");

                    foreach (var label in taskLabels)
                    {
                        try
                        {
                            var labelText = label.Text;
                            Console.WriteLine($"Checking task: '{labelText}'");

                            if (labelText.Contains(expectedTaskTitle))
                            {
                                Console.WriteLine($"✓ Found matching task: {labelText}");
                                taskFound = true;
                                break;
                            }
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Error reading label text: {ex.Message}");
                        }
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Error finding task labels on attempt {attempt + 1}: {ex.Message}");
                }

                if (!taskFound)
                {
                    attempt++;
                    Thread.Sleep(2000); // Wait 2 seconds between attempts
                }
            }

            // Assert that we found the expected task
            Assert.That(taskFound, Is.True,
                $"Could not find task with title containing '{expectedTaskTitle}' after {maxAttempts} attempts");

            Console.WriteLine($"✓ Test passed: Successfully found task with title containing '{expectedTaskTitle}'");
        }
        catch (WebDriverTimeoutException)
        {
            Assert.Fail("Tasks list was not found within timeout period. App may not have loaded correctly.");
        }
        catch (Exception ex)
        {
            Assert.Fail($"Unexpected error during test: {ex.Message}");
        }
    }
}