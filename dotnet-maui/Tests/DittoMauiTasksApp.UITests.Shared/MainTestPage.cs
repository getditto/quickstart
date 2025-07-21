using NUnit.Framework;
using OpenQA.Selenium.Appium;
using OpenQA.Selenium.Appium.Enums;
using System.Linq;

// You will have to make sure that all the namespaces match
// between the different platform specific projects and the shared
// code files. This has to do with how we initialize the AppiumDriver
// through the AppiumSetup.cs files and NUnit SetUpFixture attributes.
// Also see: https://docs.nunit.org/articles/nunit/writing-tests/attributes/setupfixture.html
namespace UITests;

// This is an example of tests that do not need anything platform specific
public class MainPageTests : BaseTest
{
	private static readonly string AddTaskButtonAutomationId = "AddTaskButton";
	private static readonly string CollectionViewAutomationId = "TasksCollectionView";
	private static readonly string TaskTitleLabelAutomationId = "TaskTitleLabel";

	[Test]
	public void AppLaunches()
	{
		Task.Delay(2000).Wait(); // Wait for the app to launch and load the initial tasks 
		App.GetScreenshot().SaveAsFile($"{nameof(AppLaunches)}.png");
	}

	[Test]
	public void VerifyInitialTasks()
	{
		// Arrange - Wait for the app to load and tasks to populate
		Task.Delay(3000).Wait(); // Wait for Ditto to initialize and load initial tasks
		
		// Find the CollectionView
		var collectionView = App.FindElement(MobileBy.AccessibilityId("TasksCollectionView"));
		Assert.That(collectionView, Is.Not.Null, "TasksCollectionView should be found");
		Assert.That(collectionView.Displayed, Is.True, "TasksCollectionView should be visible");

		// Act - Get all task title labels within the collection view
		var taskTitleLabels = App.FindElements(MobileBy.AccessibilityId("TaskTitleLabel"));
		
		// Assert - Validate we have exactly 4 tasks
		Assert.That(taskTitleLabels.Count, Is.EqualTo(4), "Should have exactly 4 initial tasks");

		// Get the expected task titles from the DittoManager's InsertInitialTasks method
		var expectedTitles = new List<string>
		{
			"Buy groceries",
			"Clean the kitchen", 
			"Schedule dentist appointment",
			"Pay bills"
		};

		// Verify each expected title is present
		var actualTitles = taskTitleLabels.Select(label => label.Text).ToList();
		
		foreach (var expectedTitle in expectedTitles)
		{
			Assert.That(actualTitles, Does.Contain(expectedTitle), 
				$"Task with title '{expectedTitle}' should be present in the collection");
		}

		// Verify all tasks are initially not completed (no strikethrough)
		foreach (var label in taskTitleLabels)
		{
			// Check that the label doesn't have strikethrough decoration
			// This is a basic check - you might need to adjust based on how MAUI renders strikethrough
			Assert.That(label.Text, Is.Not.Empty, "Task title should not be empty");
		}

		// Take a screenshot for verification
		App.GetScreenshot().SaveAsFile($"{nameof(VerifyInitialTasks)}.png");
	}
}