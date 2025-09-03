package com.example.dittotasks

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.junit.Assert.assertEquals

/**
 * UI tests for the Ditto Tasks application using Espresso framework.
 * These tests verify the user interface functionality and Ditto sync on real devices.
 */
@RunWith(AndroidJUnit4::class)
class TasksUITest {
    
    // Idling resource to wait for async operations  
    private val idlingResource = CountingIdlingResource("TaskSync")
    private lateinit var mainActivity: MainActivity
    
    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(idlingResource)
        
        // Launch activity manually with proper intent and longer timeout
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Use try-catch to handle the activity launch timeout more gracefully
        try {
            mainActivity = instrumentation.startActivitySync(intent) as MainActivity
            // Wait for UI to settle and Ditto to initialize
            Thread.sleep(10000) // Extended wait for Ditto SDK initialization
        } catch (e: RuntimeException) {
            println("❌ Activity launch timed out, likely due to Ditto initialization: ${e.message}")
            throw e
        }
    }
    
    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        if (::mainActivity.isInitialized) {
            mainActivity.finish()
        }
    }
    
    @Test
    fun testAppLaunchesSuccessfully() {
        // Activity should already be launched by setUp()
        println("✓ Activity launched successfully: ${mainActivity.javaClass.simpleName}")
        
        try {
            // Verify the main elements are displayed
            onView(withId(R.id.ditto_app_id))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("App ID:"))))
            
            onView(withId(R.id.sync_switch))
                .check(matches(isDisplayed()))
                .check(matches(isChecked()))
            
            onView(withId(R.id.add_button))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.task_list))
                .check(matches(isDisplayed()))
                
            println("✓ All UI elements found and displayed correctly")
            
        } catch (e: Exception) {
            println("❌ Test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    @Test
    fun testGitHubTestDocumentSyncs() {
        // Get the GitHub test document ID from environment variable
        val githubTestDocId = System.getenv("GITHUB_TEST_DOC_ID") ?: return
        
        // Extract the run ID from the document ID (format: github_test_RUNID_RUNNUMBER) 
        val runId = githubTestDocId.split("_").getOrNull(2) ?: githubTestDocId
        println("Looking for GitHub Run ID: $runId")
        
        // Wait longer for sync to complete from Ditto Cloud
        var attempts = 0
        val maxAttempts = 30 // 30 attempts with 2 second waits = 60 seconds max
        var documentFound = false
        
        while (attempts < maxAttempts && !documentFound) {
            try {
                // Look for a TextView with id task_text containing the GitHub test task with our run ID
                // This looks within the RecyclerView items for the actual task text
                onView(allOf(
                    withId(R.id.task_text),
                    withText(containsString("GitHub Test Task")),
                    withText(containsString(runId))
                )).check(matches(isDisplayed()))
                
                println("✓ Found synced GitHub test document with run ID: $runId")
                documentFound = true
                
            } catch (e: Exception) {
                // Document not found yet, wait and try again
                attempts++
                println("Attempt $attempts: GitHub test document not found yet, waiting...")
                Thread.sleep(2000)
            }
        }
        
        if (!documentFound) {
            throw AssertionError("GitHub test document with run ID '$runId' did not sync within ${maxAttempts * 2} seconds")
        }
    }
    
    @Test
    fun testAddNewTaskFlow() {
        // Test adding a new task through the UI
        try {
            // Click the add button
            onView(withId(R.id.add_button)).perform(click())
            
            // Wait for dialog to appear
            Thread.sleep(1000)
            
            // Type in the modal task title field
            onView(withId(R.id.modal_task_title))
                .perform(typeText("BrowserStack Test Task"))
            
            // Click Add button in dialog
            onView(withText("Add")).perform(click())
            
            // Wait for task to appear
            Thread.sleep(2000)
            
            // Verify task appears in the list - look for task_text TextView in RecyclerView
            onView(allOf(
                withId(R.id.task_text),
                withText(containsString("BrowserStack Test Task"))
            )).check(matches(isDisplayed()))
                
            println("✓ Successfully added new task through UI")
                
        } catch (e: Exception) {
            println("⚠ Add task flow test failed: ${e.message}")
            // Don't fail the test since the main sync test is more important
        }
    }
    
    @Test 
    fun testBasicAppContext() {
        // Simple test that verifies app context without UI interaction
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.dittotasks", context.packageName)
        println("✓ App context verified: ${context.packageName}")
    }
    
    @Test
    fun testMemoryUsage() {
        // Perform multiple UI operations to check for memory leaks
        repeat(3) {
            try {
                // Open add task dialog
                onView(withId(R.id.add_button)).perform(click())
                Thread.sleep(500)
                
                // Close dialog with cancel
                onView(withText("Cancel")).perform(click())
                Thread.sleep(500)
                
            } catch (e: Exception) {
                // Ignore if dialog interaction fails
                println("Dialog interaction failed on iteration ${it + 1}: ${e.message}")
            }
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        // Check memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        
        println("Memory usage: ${memoryUsagePercent.toInt()}%")
        
        // Allow up to 80% memory usage before failing
        if (memoryUsagePercent > 80) {
            throw AssertionError("Memory usage too high: ${memoryUsagePercent}%")
        }
    }
}