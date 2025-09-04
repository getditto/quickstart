package live.ditto.quickstart.tasks

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

/**
 * UI tests for the Tasks application using Compose testing framework.
 * These tests verify the user interface functionality on real devices.
 */
@RunWith(AndroidJUnit4::class)
class TasksUITest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private val testDocumentTitle: String by lazy {
        // Read the exact test document title from BrowserStack instrumentationOptions
        val args = InstrumentationRegistry.getArguments()
        val title = args?.getString("github_test_doc_id")
        
        // Fallback for local testing
        title ?: BuildConfig.TEST_DOCUMENT_TITLE ?: "BrowserStack Test Document"
    }
    
    @Before
    fun setUp() {
        // Ensure the activity is launched and Compose UI is ready
        composeTestRule.waitForIdle()
        
        // Debug: Show how we got the test document title
        val args = InstrumentationRegistry.getArguments()
        val fromInstrumentation = args?.getString("github_test_doc_id")
        val fromBuildConfig = try { BuildConfig.TEST_DOCUMENT_TITLE } catch (e: Exception) { "N/A" }
        
        println("DEBUG: Instrumentation arg 'github_test_doc_id' = '$fromInstrumentation'")
        println("DEBUG: BuildConfig.TEST_DOCUMENT_TITLE = '$fromBuildConfig'")
        println("Looking for test document: '$testDocumentTitle'")
        
        // Give extra time for the app to fully initialize
        Thread.sleep(2000)
    }
    
    @Test
    fun testDocumentSyncAndVerification() {
        println("🚀 Starting document sync verification test")
        
        // Wait for app to fully launch and initialize
        var uiReady = false
        var attempts = 0
        val maxAttempts = 6
        
        while (!uiReady && attempts < maxAttempts) {
            attempts++
            println("📱 Attempt $attempts/$maxAttempts: Waiting for app to launch...")
            
            try {
                // Wait for the activity to launch
                composeTestRule.waitForIdle()
                Thread.sleep(5000)
                
                // Try to find any UI element to confirm app launched
                composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
                println("✅ App UI is available")
                uiReady = true
                
            } catch (e: Exception) {
                println("❌ App UI not ready (attempt $attempts): ${e.message}")
                if (attempts < maxAttempts) {
                    println("⏳ Waiting 10 more seconds for app to initialize...")
                    Thread.sleep(10000)
                } else {
                    throw AssertionError("App failed to launch after $maxAttempts attempts. UI not available for testing.", e)
                }
            }
        }
        
        println("✅ App successfully launched, waiting for document sync...")
        
        // Additional wait for document synchronization from Ditto Cloud
        Thread.sleep(10000)
        
        // Look for the exact document title in the UI - this should fail the test if not found
        try {
            composeTestRule.onNode(hasText(testDocumentTitle))
                .assertExists("Document with title '$testDocumentTitle' should exist")
            println("✅ Successfully verified document: '$testDocumentTitle'")
        } catch (e: Exception) {
            println("❌ Failed to find document: '$testDocumentTitle'")
            println("Error: ${e.message}")
            
            // Print all visible text for debugging
            try {
                println("🔍 Debugging visible UI elements...")
                val allTextNodes = composeTestRule.onAllNodes(hasText("", substring = true))
                val nodeCount = allTextNodes.fetchSemanticsNodes().size
                println("Found $nodeCount text nodes in UI")
                
                // Try to find any task-like text
                println("🔍 Looking for any task titles in UI...")
                try {
                    val taskNodes = composeTestRule.onAllNodes(hasText("", substring = true))
                    println("UI appears to be working, but expected document not found")
                    println("Expected: '$testDocumentTitle'")
                } catch (textE: Exception) {
                    println("Unable to enumerate text nodes: ${textE.message}")
                }
                
            } catch (debugE: Exception) {
                println("❌ UI hierarchy not available for debugging: ${debugE.message}")
                println("This suggests the app didn't launch properly or Compose isn't initialized")
            }
            
            // Re-throw to fail the test with more context
            throw AssertionError("Expected document '$testDocumentTitle' not found in UI. App may not have synced with Ditto Cloud or document sync failed.", e)
        }
    }
    
    @Test
    fun testMemoryLeaks() {
        // Perform multiple UI operations to check for memory leaks
        repeat(5) {
            // Try to click around the UI
            try {
                composeTestRule.onAllNodes(hasClickAction())
                    .onFirst()
                    .performClick()
                composeTestRule.waitForIdle()
            } catch (e: Exception) {
                // Ignore if no clickable elements
            }
        }
        
        // Force garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(100)
        
        // Check memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        
        println("Memory usage: ${memoryUsagePercent.toInt()}%")
        assert(memoryUsagePercent < 80) { "Memory usage too high: ${memoryUsagePercent}%" }
    }
}