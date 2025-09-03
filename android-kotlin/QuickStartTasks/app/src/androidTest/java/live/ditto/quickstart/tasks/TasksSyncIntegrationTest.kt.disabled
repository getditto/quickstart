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

    private fun getTestDocumentId(): String? {
        return System.getProperty("GITHUB_TEST_DOC_ID")
            ?: try {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val bundle = InstrumentationRegistry.getArguments()
                bundle.getString("GITHUB_TEST_DOC_ID")
            } catch (e: Exception) {
                null
            }
    }

    @Test
    fun testGitHubDocumentSyncFromCloud() {
        val testDocumentId = getTestDocumentId()
        
        if (testDocumentId.isNullOrEmpty()) {
            println("⚠️ No GitHub test document ID provided, skipping sync verification")
            
            // Just verify the app launched and UI is working
            try {
                // Wait for the UI to be ready
                composeTestRule.waitForIdle()
                Thread.sleep(5000) // Give time for Ditto initialization and UI rendering
                
                composeTestRule.onNodeWithText("Ditto Tasks", useUnmergedTree = true)
                    .assertExists("App should be running even without test document")
                println("✅ App is running - sync test skipped (no document ID provided)")
            } catch (e: Exception) {
                println("❌ App not running properly: ${e.message}")
                throw AssertionError("App should launch successfully even without test document ID: ${e.message}")
            }
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

        // Wait for the UI to be ready
        composeTestRule.waitForIdle()
        Thread.sleep(5000) // Give time for Ditto initialization and UI rendering
        
        // Verify key UI elements are present
        composeTestRule.onNodeWithText("Ditto Tasks", useUnmergedTree = true)
            .assertExists("App title should be visible")
        composeTestRule.onNodeWithText("New Task", useUnmergedTree = true)
            .assertExists("New Task button should be visible")

        println("✅ Basic UI functionality test completed")
    }

    @Test
    fun testAppStability() {
        println("🧪 Testing app stability...")
        
        // Wait for the UI to be ready
        composeTestRule.waitForIdle()
        Thread.sleep(5000) // Give time for Ditto initialization and UI rendering
        
        // Check if the main UI is visible
        composeTestRule.onNodeWithText("Ditto Tasks", useUnmergedTree = true)
            .assertExists("Main screen should be visible for stability test")
        
        // Just verify the app is stable and doesn't crash
        composeTestRule.waitForIdle()
        
        // Final check that we can still see the main screen
        composeTestRule.onNodeWithText("Ditto Tasks", useUnmergedTree = true)
            .assertExists("App should still be functional")
        
        println("✅ App stability test completed successfully")
    }
}