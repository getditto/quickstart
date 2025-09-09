package com.ditto.quickstart

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class DittoSeededIdTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testGitHubSeededDocumentSync() {
        // Get test document title from instrumentation arguments
        val args = InstrumentationRegistry.getArguments()
        val actualDocumentTitle = args?.getString("github_test_doc_title")
            ?: throw IllegalStateException("No test document title provided. Expected via instrumentationOptions 'github_test_doc_title'")
        
        // INTENTIONALLY SEARCH FOR NON-EXISTENT TASK TO TEST FALSE POSITIVES
        val testDocumentTitle = "NonExistentTask_FalsePositive_12345"
        
        println("🔍 INTENTIONAL FALSE POSITIVE TEST")
        println("🔍 Actual seeded document: '$actualDocumentTitle'")
        println("🔍 Searching for NON-EXISTENT document: '$testDocumentTitle'")
        
        // Give the app time to fully launch and set up Compose
        Thread.sleep(5000)
        
        // Debug: Print the UI tree to see what's actually there
        try {
            composeTestRule.onRoot().printToLog("UI_TREE")
        } catch (e: Exception) {
            println("⚠️ Could not print UI tree: ${e.message}")
        }
        
        // Wait for app initialization and Ditto sync with intelligent polling
        composeTestRule.waitUntil(
            condition = {
                try {
                    val nodes = composeTestRule.onAllNodes(hasText(testDocumentTitle)).fetchSemanticsNodes()
                    println("📊 Found ${nodes.size} nodes with text '$testDocumentTitle'")
                    nodes.isNotEmpty()
                } catch (e: Exception) {
                    println("❌ Exception while searching: ${e.message}")
                    false
                }
            },
            timeoutMillis = 15000 // Wait up to 15 seconds for app init and Ditto sync
        )
        
        // Final verification that document exists
        composeTestRule
            .onNode(hasText(testDocumentTitle))
            .assertExists("Document with title '$testDocumentTitle' should exist in the task list")
        
        println("✅ DOCUMENT FOUND: '$testDocumentTitle'")
        
        // Sleep 3 seconds to allow BrowserStack video to capture the UI state
        println("⏱️ Sleeping 3 seconds for BrowserStack video capture...")
        Thread.sleep(3000)
        println("🎉 Test completed - UI state captured in video")
    }
}