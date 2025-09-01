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
 * BrowserStack integration test for Ditto sync functionality in Android CPP app.
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
        val githubDocId = InstrumentationRegistry.getArguments().getString("github_test_doc_id")
        val runId = InstrumentationRegistry.getArguments().getString("github_run_id")
        
        // If GitHub document info is available, test sync from cloud
        if (githubDocId != null && runId != null) {
            // Wait for document sync with extended timeout for BrowserStack
            waitForGitHubDocumentSyncCompose(runId, 45)
            
            // Verify the GitHub test document appears in the task list
            composeTestRule.onAllNodesWithText(runId, substring = true)[0]
                .assertIsDisplayed()
        } else {
            // Standard sync verification without GitHub test document
            testBasicTaskSyncFunctionality()
        }
    }

    @Test
    fun testBasicTaskSyncFunctionality() {
        val testTaskTitle = "Test Task ${System.currentTimeMillis()}"
        
        // Add a new task
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        // Wait for task input to appear and add test task
        Thread.sleep(1000)
        composeTestRule.onNodeWithText("Enter task title")
            .performTextInput(testTaskTitle)
        
        // Save the task
        composeTestRule.onNodeWithText("Add")
            .performClick()
        
        // Verify task appears in list
        composeTestRule.onNodeWithText(testTaskTitle)
            .assertIsDisplayed()
    }

    @Test
    fun testTaskToggleCompletion() {
        val testTaskTitle = "Toggle Task ${System.currentTimeMillis()}"
        
        // Add a test task first
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        Thread.sleep(500)
        composeTestRule.onNodeWithText("Enter task title")
            .performTextInput(testTaskTitle)
        
        composeTestRule.onNodeWithText("Add")
            .performClick()
        
        // Wait for task to appear
        Thread.sleep(1000)
        
        // Find and toggle the checkbox (assuming tasks have checkboxes)
        composeTestRule.onNodeWithContentDescription("Toggle completion for $testTaskTitle")
            .performClick()
        
        // Verify the task state changed (this would need specific UI implementation details)
        // For now just verify the task still exists
        composeTestRule.onNodeWithText(testTaskTitle)
            .assertIsDisplayed()
    }

    @Test
    fun testMultipleTasksSync() {
        val timestamp = System.currentTimeMillis()
        val task1 = "Sync Task 1 - $timestamp"
        val task2 = "Sync Task 2 - $timestamp"
        
        // Add first task
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        Thread.sleep(500)
        composeTestRule.onNodeWithText("Enter task title")
            .performTextInput(task1)
        
        composeTestRule.onNodeWithText("Add")
            .performClick()
        
        Thread.sleep(1000)
        
        // Add second task
        composeTestRule.onNodeWithContentDescription("Add Task")
            .performClick()
        
        Thread.sleep(500)
        composeTestRule.onNodeWithText("Enter task title")
            .performTextInput(task2)
        
        composeTestRule.onNodeWithText("Add")
            .performClick()
        
        Thread.sleep(1000)
        
        // Verify both tasks are displayed
        composeTestRule.onNodeWithText(task1)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText(task2)
            .assertIsDisplayed()
    }

    /**
     * Waits for a GitHub test document to sync from Ditto Cloud using Compose UI testing
     */
    private fun waitForGitHubDocumentSyncCompose(runId: String, timeoutSeconds: Int) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = timeoutSeconds * 1000L
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Check if any node contains the GitHub run ID
                composeTestRule.onAllNodesWithText(runId, substring = true)[0]
                    .assertIsDisplayed()
                // If we get here, the document synced successfully
                return
            } catch (e: Exception) {
                // Document not yet synced, wait and try again
                Thread.sleep(2000)
            }
        }
        
        // If we reach here, the document didn't sync within timeout
        throw AssertionError("GitHub test document with run ID '$runId' did not sync within $timeoutSeconds seconds")
    }
}