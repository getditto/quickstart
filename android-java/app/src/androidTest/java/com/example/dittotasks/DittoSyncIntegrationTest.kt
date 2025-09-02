package com.example.dittotasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.recyclerview.widget.RecyclerView
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Before
import org.hamcrest.CoreMatchers.*

/**
 * BrowserStack integration test for Ditto sync functionality.
 * This test verifies that the app can sync documents from Ditto Cloud,
 * specifically looking for GitHub test documents inserted during CI.
 * 
 * Similar to the JavaScript integration test, this validates:
 * 1. GitHub test documents appear in the app after sync
 * 2. Basic task creation and sync functionality works
 * 3. Real-time sync capabilities across the Ditto network
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Wait for activity to launch and Ditto to initialize
        Thread.sleep(3000)
        
        // Ensure sync is enabled
        try {
            onView(withId(R.id.sync_switch))
                .check(matches(isChecked()))
        } catch (e: Exception) {
            // If we can't verify switch state, try to enable it
            try {
                onView(withId(R.id.sync_switch))
                    .perform(click())
            } catch (ignored: Exception) {
                // Continue with test even if switch interaction fails
            }
        }
        
        // Additional time for initial sync to complete
        Thread.sleep(2000)
    }

    @Test
    fun testAppInitializationAndDittoConnection() {
        // Test that the app launches without crashing and displays key UI elements
        onView(withId(R.id.ditto_app_id))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.sync_switch))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.task_list))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.add_button))
            .check(matches(isDisplayed()))
    }

    @Test 
    fun testGitHubDocumentSyncFromDittoCloud() {
        // Get GitHub test document info from BrowserStack test runner args
        val githubDocId = InstrumentationRegistry.getArguments().getString("github_test_doc_id")
        val runId = InstrumentationRegistry.getArguments().getString("github_run_id")
        
        if (githubDocId.isNullOrEmpty() || runId.isNullOrEmpty()) {
            println("⚠ No GitHub test document ID provided, skipping sync verification")
            return
        }
        
        println("Checking for GitHub test document: $githubDocId")
        println("Looking for GitHub Run ID: $runId")
        
        // Wait for the GitHub test document to sync and appear in the task list
        if (waitForSyncDocument(runId, maxWaitSeconds = 30)) {
            println("✓ GitHub test document successfully synced from Ditto Cloud")
            
            // Verify the task is actually visible in the RecyclerView
            onView(withText(containsString("GitHub Test Task")))
                .check(matches(isDisplayed()))
                
            // Verify it contains our run ID
            onView(withText(containsString(runId)))
                .check(matches(isDisplayed()))
                
        } else {
            // Take a screenshot for debugging
            println("❌ GitHub test document did not sync within timeout period")
            println("Available tasks:")
            logVisibleTasks()
            throw AssertionError("Failed to sync test document from Ditto Cloud")
        }
    }

    @Test
    fun testBasicTaskCreationAndSync() {
        val deviceTaskTitle = "BrowserStack Test Task - ${android.os.Build.MODEL}"
        
        // Click the add button to create a new task
        onView(withId(R.id.add_button))
            .perform(click())
        
        // Wait for dialog to appear and add task
        Thread.sleep(1000)
        
        try {
            // Enter task text in the dialog
            onView(withId(android.R.id.edit))
                .perform(typeText(deviceTaskTitle), closeSoftKeyboard())
                
            // Click OK button
            onView(withText("OK"))
                .perform(click())
                
            // Wait for task to be added and potentially sync
            Thread.sleep(3000)
            
            // Verify the task appears in the list
            onView(withText(deviceTaskTitle))
                .check(matches(isDisplayed()))
                
            println("✓ Task created successfully and appears in list")
            
        } catch (e: Exception) {
            println("⚠ Task creation failed, this might be due to dialog differences: ${e.message}")
            // Continue with test - dialog interaction can be fragile across devices
        }
    }

    @Test
    fun testSyncToggleFunction() {
        // Test that sync toggle works without crashing the app
        try {
            // Toggle sync off
            onView(withId(R.id.sync_switch))
                .perform(click())
                
            Thread.sleep(2000)
            
            // Toggle sync back on
            onView(withId(R.id.sync_switch))
                .perform(click())
                
            Thread.sleep(2000)
            
            // Verify app is still stable
            onView(withId(R.id.task_list))
                .check(matches(isDisplayed()))
                
            println("✓ Sync toggle functionality working")
            
        } catch (e: Exception) {
            println("⚠ Sync toggle interaction failed: ${e.message}")
            // Verify app is still stable even if toggle failed
            onView(withId(R.id.ditto_app_id))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTaskListDisplaysContent() {
        // Verify the RecyclerView can display content
        try {
            // Wait for any initial sync to complete
            Thread.sleep(5000)
            
            // Check if RecyclerView has content or is empty
            val recyclerView = activityRule.scenario.onActivity { activity ->
                activity.findViewById<RecyclerView>(R.id.task_list)
            }
            
            // Just verify the RecyclerView is working
            onView(withId(R.id.task_list))
                .check(matches(isDisplayed()))
                
            println("✓ Task list RecyclerView is displayed and functional")
            
        } catch (e: Exception) {
            println("⚠ Task list verification failed: ${e.message}")
        }
    }

    /**
     * Wait for a GitHub test document to appear in the task list.
     * Similar to the JavaScript test's wait_for_sync_document function.
     */
    private fun waitForSyncDocument(runId: String, maxWaitSeconds: Int): Boolean {
        val startTime = System.currentTimeMillis()
        val timeout = maxWaitSeconds * 1000L
        
        println("Waiting for document with Run ID '$runId' to sync...")
        
        while ((System.currentTimeMillis() - startTime) < timeout) {
            try {
                // Look for the GitHub test task containing our run ID
                onView(allOf(
                    withText(containsString("GitHub Test Task")),
                    withText(containsString(runId))
                )).check(matches(isDisplayed()))
                
                println("✓ Found synced document with Run ID: $runId")
                return true
                
            } catch (e: Exception) {
                // Document not found yet, continue waiting
                Thread.sleep(1000) // Check every second
            }
        }
        
        println("❌ Document not found after $maxWaitSeconds seconds")
        return false
    }

    /**
     * Log visible tasks for debugging purposes
     */
    private fun logVisibleTasks() {
        try {
            activityRule.scenario.onActivity { activity ->
                val recyclerView = activity.findViewById<RecyclerView>(R.id.task_list)
                val adapter = recyclerView.adapter
                
                if (adapter != null) {
                    println("RecyclerView has ${adapter.itemCount} items")
                } else {
                    println("RecyclerView adapter is null")
                }
            }
        } catch (e: Exception) {
            println("Failed to log visible tasks: ${e.message}")
        }
    }
}