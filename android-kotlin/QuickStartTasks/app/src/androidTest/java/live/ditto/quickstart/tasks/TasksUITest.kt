package live.ditto.quickstart.tasks

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Tasks application targeting BrowserStack device testing.
 *
 * Must run against an emulator or physical device. The test does NOT silently pass
 * when run without a compose hierarchy — that would mask real failures in CI.
 */
@RunWith(AndroidJUnit4::class)
class TasksUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testDocumentSyncAndVerification() {
        val testDocumentTitle = resolveTestDocumentTitle()

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = SYNC_TIMEOUT_MS) {
            composeTestRule
                .onAllNodes(hasText(testDocumentTitle))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNode(hasText(testDocumentTitle))
            .assertExists("Document with title '$testDocumentTitle' should exist in the task list")
    }

    /**
     * Resolves the document title we expect Ditto to sync down. Prefer the
     * instrumentation argument (set by BrowserStack), then BuildConfig fallback.
     */
    private fun resolveTestDocumentTitle(): String {
        val fromInstrumentation = InstrumentationRegistry.getArguments()
            ?.getString(INSTRUMENTATION_ARG)
            ?.takeIf { it.isNotEmpty() }
        if (fromInstrumentation != null) return fromInstrumentation

        val fromBuildConfig = runCatching { BuildConfig.TEST_DOCUMENT_TITLE }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
        if (fromBuildConfig != null) return fromBuildConfig

        throw IllegalStateException(
            "No test document title provided. Expected via instrumentationOptions " +
                    "'$INSTRUMENTATION_ARG' or BuildConfig.TEST_DOCUMENT_TITLE"
        )
    }

    companion object {
        private const val INSTRUMENTATION_ARG = "DITTO_CLOUD_TASK_TITLE"
        private const val SYNC_TIMEOUT_MS = 18_000L
    }
}
