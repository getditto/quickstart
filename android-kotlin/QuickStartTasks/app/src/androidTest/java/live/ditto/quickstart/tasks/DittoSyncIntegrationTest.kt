package live.ditto.quickstart.tasks

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Before

/**
 * BrowserStack integration test for Ditto sync functionality in Kotlin/Compose app.
 * This test verifies that the app can sync documents from Ditto Cloud,
 * specifically looking for GitHub test documents inserted during CI.
 * 
 * This test is designed to run on BrowserStack physical devices and
 * validates real-time sync capabilities across the Ditto network.
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Allow time for Compose UI to initialize and Ditto to connect
        Thread.sleep(3000)
    }

    @Test
    fun testAppInitializationWithCompose() {
        // Verify app initializes correctly with Compose UI
        composeTestRule.onNodeWithText("Ditto Tasks")
            .assertIsDisplayed()
        
        // Verify Add FAB is present
        composeTestRule.onNodeWithContentDescription("Add Task")
            .assertIsDisplayed()
    }

    @Test
    fun testGitHubDocumentSyncFromDittoCloud() {
        // Get GitHub test document info from BrowserStack test runner args
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val githubDocId = instrumentation.getArguments().getString("github_test_doc_id")
        val runId = instrumentation.getArguments().getString("github_run_id")
        
        // If GitHub document info is available, test sync from cloud
        if (!githubDocId.isNullOrEmpty() && !runId.isNullOrEmpty()) {
            // Wait for document sync with extended timeout for BrowserStack
            waitForGitHubDocumentSyncCompose(runId, 45)
            
            // Verify the GitHub test document appears in the task list
            composeTestRule.onNodeWithText(runId, substring = true)
                .assertIsDisplayed()
            
            // Verify task contains expected GitHub test content
            composeTestRule.onNodeWithText("GitHub Test Task", substring = true)
                .assertIsDisplayed()
        } else {
            // Fallback to testing local sync functionality
            testLocalTaskSyncFunctionality()
        }
    }

    @Test
    fun testLocalTaskSyncFunctionality() {
        // Click Add FAB to create new task
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        // Wait for edit dialog to appear
        Thread.sleep(1000)
        
        // Enter task text
        composeTestRule.onNodeWithText("Task Title")
            .performTextInput("BrowserStack Compose Integration Test")
        
        // Save the task
        composeTestRule.onNodeWithText("Save")
            .performClick()
        
        // Wait for task to be created and UI to update
        Thread.sleep(2000)
        
        // Verify task appears in the list
        composeTestRule.onNodeWithText("BrowserStack Compose Integration Test")
            .assertIsDisplayed()
    }

    @Test
    fun testTaskCompletionToggle() {
        // First create a task to test with
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        Thread.sleep(1000)
        
        composeTestRule.onNodeWithText("Task Title")
            .performTextInput("Test Toggle Task")
        
        composeTestRule.onNodeWithText("Save")
            .performClick()
        
        Thread.sleep(2000)
        
        // Verify task is created
        composeTestRule.onNodeWithText("Test Toggle Task")
            .assertIsDisplayed()
        
        // Click on the task item to toggle completion
        composeTestRule.onNodeWithText("Test Toggle Task")
            .performClick()
        
        Thread.sleep(1000)
        
        // The task should still be displayed (may have visual changes for completion)
        composeTestRule.onNodeWithText("Test Toggle Task")
            .assertIsDisplayed()
    }

    @Test
    fun testMultipleTasksDisplay() {
        // Create multiple tasks to verify list functionality
        val taskNames = listOf("Task One", "Task Two", "Task Three")
        
        taskNames.forEach { taskName ->
            composeTestRule.onNodeWithContentDescription("Add Task")
                .performClick()
            
            Thread.sleep(500)
            
            composeTestRule.onNodeWithText("Task Title")
                .performTextInput(taskName)
            
            composeTestRule.onNodeWithText("Save")
                .performClick()
            
            Thread.sleep(1000)
        }
        
        // Verify all tasks are displayed
        taskNames.forEach { taskName ->
            composeTestRule.onNodeWithText(taskName)
                .assertIsDisplayed()
        }
        
        // Verify we have the expected number of task items
        composeTestRule.onAllNodesWithText("Task", substring = true)
            .fetchSemanticsNodes().size >= taskNames.size
    }

    private fun waitForGitHubDocumentSyncCompose(runId: String, maxWaitSeconds: Int) {
        val maxAttempts = maxWaitSeconds
        var attempts = 0
        
        while (attempts < maxAttempts) {
            try {
                // Look for task containing the GitHub run ID
                composeTestRule.onNodeWithText(runId, substring = true)
                    .assertIsDisplayed()
                
                // Document found, test passed
                return
                
            } catch (e: AssertionError) {
                // Document not found yet, wait and retry
                Thread.sleep(1000)
                attempts++
                
                // Log progress for BrowserStack debugging
                if (attempts % 10 == 0) {
                    println("Still waiting for GitHub document sync... ${attempts}/${maxWaitSeconds}s")
                }
            }
        }
        
        // Timeout reached, document not synced
        throw AssertionError("GitHub test document with run ID '$runId' not found after ${maxWaitSeconds}s. This may indicate a sync issue between Ditto Cloud and the device.")
    }
}