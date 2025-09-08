package com.ditto.quickstart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DittoSeededIdTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun seededTaskIsVisible() {
        val args = InstrumentationRegistry.getArguments()
        val taskId = args.getString("DITTO_TASK_ID") 
            ?: error("DITTO_TASK_ID instrumentation argument is required")
        
        // Wait for the task with the seeded ID to appear in the UI
        composeTestRule
            .onNodeWithText(taskId)
            .assertIsDisplayed()
    }
}