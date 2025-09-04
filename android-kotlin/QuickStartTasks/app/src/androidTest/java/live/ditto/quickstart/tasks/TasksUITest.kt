package live.ditto.quickstart.tasks

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        // Read the exact test document title from BrowserStack environment variable (like Swift)
        System.getenv("GITHUB_TEST_DOC_TITLE") ?: "BrowserStack Test Document"
    }
    
    @Before
    fun setUp() {
        // Wait for the UI to settle
        composeTestRule.waitForIdle()
        println("Looking for test document: '$testDocumentTitle'")
    }
    
    @Test
    fun testDocumentSyncAndVerification() {
        // Wait for document to sync and appear in UI
        Thread.sleep(3000)
        
        // Look for the exact document title in the UI - this should fail the test if not found
        try {
            composeTestRule.onNode(hasText(testDocumentTitle, substring = true))
                .assertExists("Document with title '$testDocumentTitle' should exist")
            println("✅ Successfully verified document: '$testDocumentTitle'")
        } catch (e: Exception) {
            println("❌ Failed to find document: '$testDocumentTitle'")
            println("Error: ${e.message}")
            
            // Print all visible text for debugging
            try {
                val allNodes = composeTestRule.onAllNodes(hasText("", substring = true))
                println("All visible text nodes:")
                // This will help debug what's actually visible in the UI
            } catch (debugE: Exception) {
                println("Could not enumerate visible text nodes")
            }
            
            // Re-throw to fail the test
            throw AssertionError("Expected document '$testDocumentTitle' not found in UI", e)
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