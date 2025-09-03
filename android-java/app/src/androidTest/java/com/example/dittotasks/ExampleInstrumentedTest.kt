package com.example.dittotasks

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    fun testAppLaunchesSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
            Thread.sleep(2000) // Brief wait to detect immediate crashes
        } catch (e: Exception) {
            throw AssertionError("MainActivity failed to launch: ${e.message}")
        }
    }
    
    @Test 
    fun testBasicAppContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.dittotasks", context.packageName)
    }
    
    @Test
    fun testGitHubTestDocumentSyncs() {
        val githubTestDocId = System.getenv("GITHUB_TEST_DOC_ID") ?: return
        testDocumentSyncVerification(githubTestDocId)
    }
    
    private fun testDocumentSyncVerification(docId: String) {
        val runId = docId.split("_").getOrNull(2) ?: docId
        val maxAttempts = 30
        var documentFound = false
        var attempts = 0
        
        // Launch activity for sync test
        if (!::activityScenario.isInitialized) {
            activityScenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
            Thread.sleep(20000) // Wait for Ditto cloud sync initialization
        }
        
        while (attempts < maxAttempts && !documentFound) {
            try {
                onView(allOf(
                    withId(R.id.task_text),
                    withText(containsString("GitHub Test Task")),
                    withText(containsString(runId))
                )).check(matches(isDisplayed()))
                
                Thread.sleep(2000) // Visual confirmation pause
                documentFound = true
                
            } catch (e: Exception) {
                attempts++
                if (attempts % 5 == 0) {
                    try {
                        onView(withId(R.id.task_list)).check(matches(isDisplayed()))
                    } catch (listE: Exception) {
                        // Task list not available
                    }
                }
                Thread.sleep(2000)
            }
        }
        
        if (!documentFound) {
            throw AssertionError(
                "GitHub test document did not sync within ${maxAttempts * 2} seconds. " +
                "Expected document ID: $docId with text containing 'GitHub Test Task' and '$runId'"
            )
        }
    }
    
}