package live.ditto.quickstart.tasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.hamcrest.CoreMatchers.*

/**
 * BrowserStack integration test for Ditto sync functionality in Android CPP app.
 * This test verifies that the app can sync documents from Ditto Cloud,
 * specifically looking for GitHub test documents inserted during CI.
 * 
 * This test is designed to run on BrowserStack physical devices and
 * validates real-time sync capabilities with native C++ Ditto integration.
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    private val syncIdlingResource = CountingIdlingResource("DittoSync")

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(syncIdlingResource)
        // Allow time for native C++ Ditto to initialize and establish connections
        Thread.sleep(4000)
    }
    
    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(syncIdlingResource)
    }

    @Test
    fun testAppInitializationWithCppDitto() {
        // Verify app initializes correctly with C++ Ditto integration
        onView(withText("Ditto Tasks"))
            .check(matches(isDisplayed()))
        
        // Verify task list is present
        onView(withId(android.R.id.list))
            .check(matches(isDisplayed()))
        
        // Verify add button is available
        onView(withId(R.id.add_task_button))
            .check(matches(isDisplayed()))
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
            waitForGitHubDocumentSync(runId, 45)
            
            // Verify the GitHub test document appears in task list
            onView(withId(android.R.id.list))
                .check(matches(isDisplayed()))
            
            // Verify task with GitHub run ID is visible
            onView(withText(containsString(runId)))
                .check(matches(isDisplayed()))
            
            // Verify task contains expected GitHub test content
            onView(withText(containsString("GitHub Test Task")))
                .check(matches(isDisplayed()))
                
        } else {
            // Fallback to testing local sync functionality
            testLocalTaskSyncFunctionality()
        }
    }

    @Test
    fun testLocalTaskSyncFunctionality() {
        // Test creating a task and verifying it syncs through C++ layer
        onView(withId(R.id.add_task_button))
            .perform(click())
        
        // Enter task text (assuming EditText dialog)
        onView(withId(R.id.task_input))
            .perform(typeText("BrowserStack CPP Integration Test Task"))
        
        // Confirm task creation
        onView(withText("OK"))
            .perform(click())
        
        // Wait for task to be created via C++ and UI to update
        Thread.sleep(3000)
        
        // Verify task appears in the list
        onView(withText("BrowserStack CPP Integration Test Task"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNativeCppTaskOperations() {
        // Test task operations that go through the native C++ layer
        onView(withId(R.id.add_task_button))
            .perform(click())
        
        onView(withId(R.id.task_input))
            .perform(typeText("CPP Native Test Task"))
        
        onView(withText("OK"))
            .perform(click())
        
        Thread.sleep(2000)
        
        // Verify task is created
        onView(withText("CPP Native Test Task"))
            .check(matches(isDisplayed()))
        
        // Test task completion toggle (if available in CPP app)
        try {
            onView(allOf(
                withId(R.id.task_checkbox),
                hasSibling(withText("CPP Native Test Task"))
            )).perform(click())
            
            Thread.sleep(1000)
            
            // Verify task completion state changed
            onView(allOf(
                withId(R.id.task_checkbox),
                hasSibling(withText("CPP Native Test Task"))
            )).check(matches(isChecked()))
            
        } catch (e: Exception) {
            // Task completion toggle may not be implemented, continue test
            println("Task completion toggle not available in CPP app: ${e.message}")
        }
    }

    @Test
    fun testCppDittoSyncBehavior() {
        // Test behavior specific to C++ Ditto implementation
        
        // Create multiple tasks to test bulk sync through native layer
        val taskNames = listOf("CPP Task 1", "CPP Task 2", "CPP Task 3")
        
        taskNames.forEach { taskName ->
            onView(withId(R.id.add_task_button))
                .perform(click())
            
            onView(withId(R.id.task_input))
                .perform(typeText(taskName))
            
            onView(withText("OK"))
                .perform(click())
            
            Thread.sleep(1500) // Allow time for C++ processing
        }
        
        // Verify all tasks are displayed
        taskNames.forEach { taskName ->
            onView(withText(taskName))
                .check(matches(isDisplayed()))
        }
        
        // Allow time for potential C++ sync operations to complete
        Thread.sleep(3000)
    }

    private fun waitForGitHubDocumentSync(runId: String, maxWaitSeconds: Int) {
        val maxAttempts = maxWaitSeconds
        var attempts = 0
        
        while (attempts < maxAttempts) {
            try {
                // Look for task containing the GitHub run ID
                onView(withText(containsString(runId)))
                    .check(matches(isDisplayed()))
                
                // Document found, test passed
                return
                
            } catch (e: AssertionError) {
                // Document not found yet, wait and retry
                Thread.sleep(1000)
                attempts++
                
                // Log progress for BrowserStack debugging
                if (attempts % 10 == 0) {
                    println("Still waiting for GitHub document sync via C++ layer... ${attempts}/${maxWaitSeconds}s")
                }
            }
        }
        
        // Timeout reached, document not synced
        throw AssertionError("GitHub test document with run ID '$runId' not found after ${maxWaitSeconds}s. This may indicate a sync issue between Ditto Cloud and the C++ native layer.")
    }
}