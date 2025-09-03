package live.ditto.quickstart.tasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
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
class SimpleIntegrationTest {
    
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
        println("🚀 Starting STRICT MainActivity launch test...")
        
        try {
            // Launch activity with proper scenario management
            activityScenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
            
            // Wait for Ditto initialization and UI rendering
            println("⏳ Waiting for Ditto initialization and UI...")
            Thread.sleep(15000) // 15 seconds for full initialization
            
            // STRICT CHECK: Verify the app title is actually visible
            println("🔍 Checking for app title 'Ditto Tasks'...")
            onView(withText("Ditto Tasks"))
                .check(matches(isDisplayed()))
            
            println("✅ App title found - MainActivity UI is working")
            
            // STRICT CHECK: Verify the New Task button exists
            println("🔍 Checking for 'New Task' button...")
            onView(withText("New Task"))
                .check(matches(isDisplayed()))
            
            println("✅ New Task button found - UI is fully functional")
            
        } catch (e: Exception) {
            println("❌ STRICT launch test failed: ${e.message}")
            println("   This means the app UI is NOT working properly")
            throw AssertionError("MainActivity UI verification failed - app not working: ${e.message}")
        }
    }
    
    @Test 
    fun testBasicAppContext() {
        println("🧪 Starting app context verification...")
        
        // Verify app context without UI interaction
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("live.ditto.quickstart.tasks", context.packageName)
        println("✅ App context verified: ${context.packageName}")
        
        // Additional strict check - launch and verify UI briefly
        try {
            if (!::activityScenario.isInitialized) {
                activityScenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
                Thread.sleep(10000) // Wait for initialization
            }
            
            // Verify the activity is actually displaying something
            onView(withText("Ditto Tasks"))
                .check(matches(isDisplayed()))
            
            println("✅ Context test passed - UI is responsive")
        } catch (e: Exception) {
            throw AssertionError("Context test failed - UI not responsive: ${e.message}")
        }
    }
    
    @Test
    fun testGitHubTestDocumentSyncs() {
        println("🔍 Starting GitHub test document sync verification...")
        
        // Get the GitHub test document ID from environment variable
        val githubTestDocId = System.getenv("GITHUB_TEST_DOC_ID")
        
        if (githubTestDocId.isNullOrEmpty()) {
            println("⚠️  No GITHUB_TEST_DOC_ID environment variable found")
            println("   This test MUST run in CI with seeded documents")
            
            // STRICT: In CI, this test should fail if no doc ID is provided
            // We can detect CI by checking for common CI environment variables
            val isCI = System.getenv("CI") != null || 
                      System.getenv("GITHUB_ACTIONS") != null ||
                      System.getenv("BROWSERSTACK_USERNAME") != null
            
            if (isCI) {
                throw AssertionError("GITHUB_TEST_DOC_ID is required in CI environment but was not provided")
            } else {
                println("   Skipping sync test (local environment)")
                return
            }
        }
        
        // Extract the run ID from the document ID (format: github_test_android_RUNID_RUNNUMBER) 
        val runId = githubTestDocId.split("_").getOrNull(3) ?: githubTestDocId
        println("🎯 Looking for GitHub Test Task with Run ID: $runId")
        println("📄 Full document ID: $githubTestDocId")
        
        // Wait longer for sync to complete from Ditto Cloud
        var attempts = 0
        val maxAttempts = 30 // 30 attempts with 2 second waits = 60 seconds max
        var documentFound = false
        var lastException: Exception? = null
        
        // Launch activity and verify it's working before testing sync
        if (!::activityScenario.isInitialized) {
            println("🚀 Launching MainActivity for sync test...")
            activityScenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
            
            // Wait for Ditto to initialize with cloud sync
            println("⏳ Waiting for Ditto cloud sync initialization...")
            Thread.sleep(15000) // 15 seconds for cloud sync setup
            
            // STRICT: Verify the app UI is working before testing sync
            try {
                println("🔍 Verifying app UI is responsive before sync test...")
                onView(withText("Ditto Tasks")).check(matches(isDisplayed()))
                println("✅ App UI is working - proceeding with sync test")
            } catch (e: Exception) {
                throw AssertionError("App UI is not working - cannot test sync: ${e.message}")
            }
        }
        
        // First, ensure we can see any tasks at all (verify UI is working)
        println("🔍 Checking if task list UI is functional...")
        try {
            onView(withText("Ditto Tasks")).check(matches(isDisplayed()))
            println("✅ Task list UI confirmed working")
        } catch (e: Exception) {
            throw AssertionError("Task list UI not working - cannot test sync: ${e.message}")
        }
        
        while (attempts < maxAttempts && !documentFound) {
            attempts++
            println("🔄 Attempt $attempts/$maxAttempts: Searching for document with run ID '$runId'...")
            
            try {
                // STRICT SEARCH: Look for the exact content we expect
                // The document should contain both "GitHub Test Task" and the run ID
                println("   Looking for text containing 'GitHub Test Task' AND '$runId'...")
                
                onView(withText(allOf(
                    containsString("GitHub Test Task"),
                    containsString(runId)
                ))).check(matches(isDisplayed()))
                
                println("✅ SUCCESS: Found synced GitHub test document with run ID: $runId")
                documentFound = true
                
                // Additional verification - make sure it's actually displayed
                onView(withText(allOf(
                    containsString("GitHub Test Task"),
                    containsString(runId)
                ))).check(matches(isDisplayed()))
                
                println("✅ VERIFIED: Document is displayed and contains expected content")
                
            } catch (e: Exception) {
                lastException = e
                println("   ❌ Document not found: ${e.message}")
                
                // Every 5 attempts, verify the app is still working
                if (attempts % 5 == 0) {
                    try {
                        println("   🔍 Verifying app is still responsive...")
                        onView(withText("Ditto Tasks")).check(matches(isDisplayed()))
                        println("   ✅ App is still responsive")
                        
                        // Try to see if there are ANY tasks visible
                        try {
                            onView(withText("New Task")).check(matches(isDisplayed()))
                            println("   📝 'New Task' button visible - UI is working")
                        } catch (buttonE: Exception) {
                            println("   ⚠️  'New Task' button not found: ${buttonE.message}")
                        }
                        
                    } catch (appE: Exception) {
                        throw AssertionError("App became unresponsive during sync test: ${appE.message}")
                    }
                }
                
                if (attempts < maxAttempts) {
                    Thread.sleep(2000)
                }
            }
        }
        
        if (!documentFound) {
            val errorMsg = """
                ❌ FAILED: GitHub test document did not sync within ${maxAttempts * 2} seconds
                
                Expected to find:
                - Document ID: $githubTestDocId
                - Text containing: "GitHub Test Task" AND "$runId"
                - In Compose UI elements
                
                Possible causes:
                1. Document not seeded to Ditto Cloud during CI
                2. App not connecting to Ditto Cloud (check network connectivity)
                3. Ditto sync taking longer than expected
                4. UI structure changed (this is a Compose app, not traditional Views)
                
                Last error: ${lastException?.message}
            """.trimIndent()
            
            throw AssertionError(errorMsg)
        }
    }
}