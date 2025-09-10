package com.ditto.quickstart

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
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
        val testDocumentTitle = args?.getString("github_test_doc_title")
            ?: throw IllegalStateException("No test document title provided. Expected via instrumentationOptions 'github_test_doc_title'")
        
        println("🔍 Looking for document: '$testDocumentTitle'")
        
        // Give the app time to fully launch and set up Compose
        Thread.sleep(3000)
        
        // Handle permission dialogs using UiAutomator (can interact with system dialogs)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        for (i in 1..3) { // Try up to 3 permission dialogs
            try {
                // Look for permission dialog buttons in different Android versions
                val allowSelectors = listOf(
                    UiSelector().text("Allow"),
                    UiSelector().text("ALLOW"), 
                    UiSelector().text("Allow only while using the app"),
                    UiSelector().text("While using the app"),
                    UiSelector().text("OK")
                )
                
                var found = false
                for (selector in allowSelectors) {
                    val allowButton = device.findObject(selector)
                    if (allowButton.exists()) {
                        println("📱 Found permission dialog $i, clicking '${allowButton.text}'")
                        allowButton.click()
                        Thread.sleep(1500) // Wait for dialog to dismiss and next one to appear
                        found = true
                        break
                    }
                }
                
                if (!found) {
                    println("ℹ️ No more permission dialogs found after $i attempts")
                    break
                }
            } catch (e: Exception) {
                println("⚠️ Error handling permission dialog $i: ${e.message}")
                break
            }
        }
        
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