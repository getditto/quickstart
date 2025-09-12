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

@RunWith(AndroidJUnit4::class)
class DittoSeededIdTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testGitHubSeededDocumentSync() {
        val args = InstrumentationRegistry.getArguments()
        val testDocumentTitle = args?.getString("github_test_doc_title")
            ?: throw IllegalStateException("No test document title provided. Please provide it via the instrumentation argument 'github_test_doc_title'.")
        
        Thread.sleep(3000)
        
        // Handle system permission dialogs
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        for (i in 1..3) {
            try {
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
                        allowButton.click()
                        Thread.sleep(1500)
                        found = true
                        break
                    }
                }
                
                if (!found) break
            } catch (e: Exception) {
                break
            }
        }
        
        // Wait for document to appear
        composeTestRule.waitUntil(
            condition = {
                composeTestRule.onAllNodes(hasText(testDocumentTitle)).fetchSemanticsNodes().isNotEmpty()
            },
            timeoutMillis = 15000
        )
        
        // Verify document exists
        composeTestRule
            .onNode(hasText(testDocumentTitle))
            .assertExists("Document with title '$testDocumentTitle' should exist in the task list")
        
        // Allow time for video capture
        Thread.sleep(3000)
    }
}