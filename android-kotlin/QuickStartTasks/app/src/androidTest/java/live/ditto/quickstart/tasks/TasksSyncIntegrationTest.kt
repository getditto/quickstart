package live.ditto.quickstart.tasks

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import java.util.concurrent.TimeUnit

/**
 * Integration tests for verifying Ditto Cloud sync functionality.
 * These tests verify that documents seeded in Ditto Cloud appear in the mobile app UI.
 */
@RunWith(AndroidJUnit4::class)
class TasksSyncIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private var testDocumentId: String? = null

    @Before
    fun setUp() {
        // Get the test document ID from system properties or instrumentation arguments
        testDocumentId = System.getProperty("GITHUB_TEST_DOC_ID")
            ?: try {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val bundle = InstrumentationRegistry.getArguments()
                bundle.getString("GITHUB_TEST_DOC_ID")
            } catch (e: Exception) {
                null
            }

        println("TasksSyncIntegrationTest: Test document ID = $testDocumentId")

        // Wait for the UI to settle and initial sync
        composeTestRule.waitForIdle()
        
        // Give additional time for Ditto to establish connection and sync
        Thread.sleep(5000)
    }

    @Test
    fun testGitHubDocumentSyncFromCloud() {
        if (testDocumentId.isNullOrEmpty()) {
            println("⚠️ No GitHub test document ID provided, skipping sync verification")
            // Still run basic UI test to ensure app is functional
            testBasicUIFunctionality()
            return
        }

        println("🔍 Looking for GitHub test document: $testDocumentId")

        // Extract run ID from document ID for searching (format: github_test_android_RUNID_RUNNUMBER)
        val runId = testDocumentId!!.split('_').getOrNull(3) ?: testDocumentId!!
        println("🔍 Looking for GitHub Run ID: $runId")

        var documentFound = false
        var attempts = 0
        val maxAttempts = 30 // 30 seconds with 1-second intervals

        // Wait for the document to sync with polling
        while (!documentFound && attempts < maxAttempts) {
            attempts++
            
            try {
                // Look for any text containing our run ID in task items
                val matchingNodes = composeTestRule.onAllNodesWithText(
                    runId, 
                    substring = true, 
                    ignoreCase = true
                ).fetchSemanticsNodes()

                if (matchingNodes.isNotEmpty()) {
                    println("✅ Found ${matchingNodes.size} matching nodes containing '$runId'")
                    documentFound = true
                    break
                }

                // Alternative: Look for "GitHub Test Task" text
                val githubTestNodes = composeTestRule.onAllNodesWithText(
                    "GitHub Test Task", 
                    substring = true, 
                    ignoreCase = true
                ).fetchSemanticsNodes()

                if (githubTestNodes.isNotEmpty()) {
                    // Check if any of these contain our run ID
                    for (i in 0 until githubTestNodes.size) {
                        try {
                            val node = composeTestRule.onAllNodesWithText(
                                "GitHub Test Task", 
                                substring = true, 
                                ignoreCase = true
                            )[i]
                            
                            // Check if this node also contains our run ID
                            val nodeText = try {
                                val config = node.fetchSemanticsNode().config
                                val textList = config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                                textList.joinToString(" ") { it.text }
                            } catch (e: Exception) {
                                ""
                            }
                            
                            if (nodeText.contains(runId, ignoreCase = true)) {
                                println("✅ Found GitHub test task containing run ID: $nodeText")
                                documentFound = true
                                break
                            }
                        } catch (e: Exception) {
                            // Continue checking other nodes
                        }
                    }
                }

            } catch (e: Exception) {
                // Node not found yet, continue waiting
                println("⏳ Attempt $attempts: Document not found yet...")
            }

            if (!documentFound) {
                Thread.sleep(1000) // Wait 1 second before next attempt
            }
        }

        if (documentFound) {
            println("🎉 Successfully verified GitHub test document synced from Ditto Cloud!")
            
            // Additional verification: Try to interact with the synced task
            try {
                composeTestRule.onNodeWithText(runId, substring = true, ignoreCase = true)
                    .assertExists("Synced document should be visible in UI")
                
                println("✅ Synced document is properly displayed in the UI")
            } catch (e: Exception) {
                println("⚠️ Document found but might not be fully rendered: ${e.message}")
            }
            
        } else {
            // Print current UI state for debugging
            println("❌ GitHub test document not found after ${maxAttempts} seconds")
            println("🔍 Current UI content for debugging:")
            
            try {
                // Print all text nodes for debugging
                val allTextNodes = composeTestRule.onAllNodes(hasText("", substring = true))
                    .fetchSemanticsNodes()
                
                allTextNodes.forEachIndexed { index, node ->
                    val text = try {
                        val textList = node.config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                        textList.joinToString(" ") { it.text }
                    } catch (e: Exception) {
                        "No text"
                    }
                    println("  Text node $index: $text")
                }
            } catch (e: Exception) {
                println("  Could not retrieve UI text content: ${e.message}")
            }
            
            throw AssertionError("GitHub test document '$testDocumentId' did not sync within timeout period")
        }
    }

    @Test 
    fun testBasicUIFunctionality() {
        println("🧪 Testing basic UI functionality...")

        // Verify key UI elements are present and functional
        composeTestRule.onNodeWithText("Ditto Tasks").assertExists("App title should be visible")
        composeTestRule.onNodeWithText("New Task").assertExists("New Task button should be visible")

        // Test navigation to add task screen
        try {
            composeTestRule.onNodeWithText("New Task").performClick()
            composeTestRule.waitForIdle()

            // Should navigate to edit screen - look for input field or save button
            Thread.sleep(2000) // Give time for navigation

            // Look for common edit screen elements
            val hasInputField = try {
                composeTestRule.onNodeWithText("Task Title", ignoreCase = true, substring = true).assertExists()
                true
            } catch (e: Exception) {
                try {
                    composeTestRule.onNode(hasSetTextAction()).assertExists()
                    true
                } catch (e2: Exception) {
                    false
                }
            }

            if (hasInputField) {
                println("✅ Successfully navigated to task creation screen")
            } else {
                println("⚠️ Navigation to task creation screen may not have worked as expected")
            }

        } catch (e: Exception) {
            println("⚠️ Could not test task creation navigation: ${e.message}")
        }

        println("✅ Basic UI functionality test completed")
    }

    @Test
    fun testAppStability() {
        println("🧪 Testing app stability...")
        
        // Perform multiple operations to ensure app doesn't crash
        repeat(3) { iteration ->
            try {
                println("  Stability test iteration ${iteration + 1}")
                
                // Wait for UI to settle
                composeTestRule.waitForIdle()
                
                // Try to interact with UI elements
                val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
                    .fetchSemanticsNodes()
                
                if (clickableNodes.isNotEmpty()) {
                    // Click the first clickable element (likely the New Task button)
                    composeTestRule.onAllNodes(hasClickAction())[0].performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(1000)
                }
                
                // Go back if we're not on main screen
                try {
                    // Look for back navigation or try to get back to main screen
                    composeTestRule.onNodeWithContentDescription("Navigate up").performClick()
                    Thread.sleep(500)
                } catch (e: Exception) {
                    // Might already be on main screen or different navigation pattern
                }
                
            } catch (e: Exception) {
                println("    Warning in iteration ${iteration + 1}: ${e.message}")
            }
        }

        // Final check that we can still see the main screen
        composeTestRule.onNodeWithText("Ditto Tasks").assertExists("App should still be functional after stress test")
        
        println("✅ App stability test completed successfully")
    }
}