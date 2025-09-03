package com.example.dittotasks

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.junit.Assert.assertEquals
import androidx.test.platform.app.InstrumentationRegistry

/**
 * UI tests for the Ditto Tasks application using Espresso framework.
 * These tests verify the user interface functionality and Ditto sync on real devices.
 */
@RunWith(AndroidJUnit4::class)
class TasksUITest {
    
    private lateinit var activityScenario: androidx.test.core.app.ActivityScenario<MainActivity>
    
    // Idling resource to wait for async operations  
    private val idlingResource = CountingIdlingResource("TaskSync")
    
    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(idlingResource)
    }
    
    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        if (::activityScenario.isInitialized) {
            activityScenario.close()
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
    fun testGitHubTestDocumentSyncs() {
        println("🔍 Starting GitHub test document sync verification...")
        
        // Get the GitHub test document ID from environment variable
        val githubTestDocId = System.getenv("GITHUB_TEST_DOC_ID")
        
        if (githubTestDocId == null) {
            println("⚠️  No GITHUB_TEST_DOC_ID environment variable found - skipping sync test")
            println("   This is expected when running locally (only works in CI)")
            return
        }
        
        // Extract the run ID from the document ID (format: github_test_RUNID_RUNNUMBER) 
        val runId = githubTestDocId.split("_").getOrNull(2) ?: githubTestDocId
        println("🎯 Looking for GitHub Test Task with Run ID: $runId")
        println("📄 Full document ID: $githubTestDocId")
        
        // Wait longer for sync to complete from Ditto Cloud
        var attempts = 0
        val maxAttempts = 30 // 30 attempts with 2 second waits = 60 seconds max
        var documentFound = false
        var lastException: Exception? = null
        
        // Launch activity only when we need to test sync
        if (!::activityScenario.isInitialized) {
            println("🚀 Launching MainActivity for sync test...")
            activityScenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
            
            // Wait for Ditto to initialize with cloud sync
            println("⏳ Waiting for Ditto cloud sync initialization...")
            Thread.sleep(20000) // 20 seconds for cloud sync setup
        }
        
        while (attempts < maxAttempts && !documentFound) {
            try {
                // Look for a TextView with id task_text containing the GitHub test task with our run ID
                // This looks within the RecyclerView items for the actual task text
                onView(allOf(
                    withId(R.id.task_text),
                    withText(containsString("GitHub Test Task")),
                    withText(containsString(runId))
                )).check(matches(isDisplayed()))
                
                println("✅ SUCCESS: Found synced GitHub test document with run ID: $runId")
                documentFound = true
                
            } catch (e: Exception) {
                lastException = e
                attempts++
                println("🔄 Attempt $attempts/$maxAttempts: GitHub test document not found yet, waiting 2s...")
                
                // Every 10 attempts, log what we can see in the task list
                if (attempts % 10 == 0) {
                    try {
                        // Try to count how many tasks are visible
                        onView(withId(R.id.task_list)).check(matches(isDisplayed()))
                        println("📝 Task list is visible, but target document not found yet")
                    } catch (listE: Exception) {
                        println("⚠️  Task list not found: ${listE.message}")
                    }
                }
                
                Thread.sleep(2000)
            }
        }
        
        if (!documentFound) {
            val errorMsg = """
                ❌ FAILED: GitHub test document did not sync within ${maxAttempts * 2} seconds
                
                Expected to find:
                - Document ID: $githubTestDocId
                - Text containing: "GitHub Test Task" AND "$runId"
                - In RecyclerView item with id: task_text
                
                Possible causes:
                1. Document not seeded to Ditto Cloud during CI
                2. App not connecting to Ditto Cloud (check DITTO_ENABLE_CLOUD_SYNC = true)
                3. Network connectivity issues
                4. Ditto sync taking longer than expected
                
                Last error: ${lastException?.message}
            """.trimIndent()
            
            throw AssertionError(errorMsg)
        }
    }
    
}