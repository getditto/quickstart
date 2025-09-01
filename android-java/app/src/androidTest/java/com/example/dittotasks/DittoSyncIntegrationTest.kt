package com.example.dittotasks

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
 * BrowserStack integration test for Ditto sync functionality.
 * This test verifies that the app can sync documents from Ditto Cloud,
 * specifically looking for GitHub test documents inserted during CI.
 * 
 * This test is designed to run on BrowserStack physical devices and
 * validates real-time sync capabilities across the Ditto network.
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    private val syncIdlingResource = CountingIdlingResource("DittoSync")

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(syncIdlingResource)
        // Allow time for Ditto to initialize and establish connections
        Thread.sleep(3000)
    }
    
    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(syncIdlingResource)
    }

    @Test
    fun testAppInitializationAndDittoConnection() {
        // Verify app initializes correctly with Ditto configuration
        onView(withId(R.id.ditto_app_id))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("App ID:"))))
        
        // Verify Ditto credentials are loaded
        onView(withId(R.id.ditto_playground_token))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("Playground Token:"))))
        
        // Verify sync is active by default
        onView(withId(R.id.sync_switch))
            .check(matches(isDisplayed()))
            .check(matches(isChecked()))
    }

    @Test 
    fun testGitHubDocumentSyncFromDittoCloud() {
        // Get the GitHub test document ID from BrowserStack test annotations
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val githubDocId = InstrumentationRegistry.getArguments().getString("github_test_doc_id")
        val runId = InstrumentationRegistry.getArguments().getString("github_run_id")
        
        // If GitHub document info is available, test sync from cloud
        if (githubDocId != null && runId != null) {
            // Wait for document sync with extended timeout for BrowserStack
            waitForGitHubDocumentSync(runId as String, 45)
            
            // Verify the GitHub test document appears in task list
            onView(withId(R.id.task_list))
                .check(matches(isDisplayed()))
            
            // Verify task with GitHub run ID is visible 
            onView(withId(R.id.task_text))
                .check(matches(withText(containsString(runId as String))))
            
            // Verify task contains expected GitHub test content
            onView(withId(R.id.task_text))
                .check(matches(withText(containsString("GitHub Test Task"))))
                
        } else {
            // Fallback to testing local sync functionality
            testLocalTaskSyncFunctionality()
        }
    }

    @Test
    fun testLocalTaskSyncFunctionality() {
        // Test creating a task and verifying it syncs locally
        onView(withId(R.id.add_button))
            .perform(click())
        
        // Enter task in modal dialog
        onView(withId(R.id.modal_task_title))
            .perform(typeText("BrowserStack Integration Test Task"))
        
        // Dismiss keyboard and click Add
        onView(withText("Add"))
            .perform(click())
        
        // Wait for task to be created and UI to update
        Thread.sleep(2000)
        
        // Verify task appears in the list
        onView(withText("BrowserStack Integration Test Task"))
            .check(matches(isDisplayed()))
        
        // Test task completion toggle
        onView(allOf(
            withId(R.id.task_checkbox),
            hasSibling(withText("BrowserStack Integration Test Task"))
        )).perform(click())
        
        // Verify task is marked complete
        Thread.sleep(1000)
        onView(allOf(
            withId(R.id.task_checkbox), 
            hasSibling(withText("BrowserStack Integration Test Task"))
        )).check(matches(isChecked()))
    }

    @Test
    fun testSyncToggleFunction() {
        // Verify sync starts enabled
        onView(withId(R.id.sync_switch))
            .check(matches(isChecked()))
        
        // Toggle sync off
        onView(withId(R.id.sync_switch))
            .perform(click())
        
        // Verify sync state changed (may need to check text or color changes)
        Thread.sleep(1000)
        
        // Toggle sync back on
        onView(withId(R.id.sync_switch))
            .perform(click())
        
        // Verify sync is re-enabled
        Thread.sleep(1000)
        onView(withId(R.id.sync_switch))
            .check(matches(isChecked()))
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
                    println("Still waiting for GitHub document sync... ${attempts}/${maxWaitSeconds}s")
                }
            }
        }
        
        // Timeout reached, document not synced
        throw AssertionError("GitHub test document with run ID '$runId' not found after ${maxWaitSeconds}s. This may indicate a sync issue between Ditto Cloud and the device.")
    }
}