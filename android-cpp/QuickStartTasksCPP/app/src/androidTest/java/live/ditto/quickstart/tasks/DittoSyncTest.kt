package live.ditto.quickstart.tasks

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Before

/**
 * Appium-based E2E UI test for the android-cpp Tasks application targeting BrowserStack device testing.
 * This test uses UiAutomator for cross-platform compatibility and verifies that a task name
 * provided via environment variable appears on screen.
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testGitHubTestDocumentSyncs() {
        // Get task name from environment variable with multiple fallback sources
        val args = InstrumentationRegistry.getArguments()
        val fromInstrumentation = args?.getString("github_test_doc_id")

        // Try BuildConfig as fallback
        val fromBuildConfig = try {
            BuildConfig.TEST_DOCUMENT_TITLE
        } catch (e: NoSuchFieldError) {
            null
        } catch (e: ExceptionInInitializerError) {
            null
        }

        // Try multiple fallback sources for local development and CI compatibility
        val fromSystemProperty = System.getProperty("GITHUB_TEST_DOC_ID")
        val fromEnvironment = System.getenv("GITHUB_TEST_DOC_ID")

        val testTaskName = fromInstrumentation?.takeIf { it.isNotEmpty() }
            ?: fromBuildConfig?.takeIf { it.isNotEmpty() }
            ?: fromSystemProperty?.takeIf { it.isNotEmpty() }
            ?: fromEnvironment?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("No test task name provided. Expected via instrumentationOptions 'github_test_doc_id', BuildConfig.TEST_DOCUMENT_TITLE, or environment variable 'GITHUB_TEST_DOC_ID'")

        Log.i("DittoSyncTest", "Testing with task name: $testTaskName")

        // Launch activity manually with proper error handling
        Log.i("DittoSyncTest", "Launching MainActivity...")
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                Log.i("DittoSyncTest", "Activity launched successfully")

                // Wait for app to appear on screen
                val appPackage = "live.ditto.quickstart.taskscpp"
                device.wait(Until.hasObject(androidx.test.uiautomator.By.pkg(appPackage)), 10000)

                // Wait for app initialization and Ditto sync
                Log.i("DittoSyncTest", "Waiting for app initialization and Ditto sync...")
                Thread.sleep(6000) // Allow time for Ditto sync and UI updates

                // Wait for the task to appear with timeout using UiAutomator
                Log.i("DittoSyncTest", "Searching for task with name: $testTaskName")
                var taskFound = false
                val maxRetries = 12 // 12 seconds with 1 second intervals

                for (attempt in 1..maxRetries) {
                    // Look for the task text using UiSelector
                    val taskObject: UiObject = device.findObject(UiSelector().textContains(testTaskName))

                    if (taskObject.exists()) {
                        Log.i("DittoSyncTest", "✅ TASK FOUND: '$testTaskName' on attempt $attempt")
                        taskFound = true

                        // Verify the task is actually displayed
                        assertTrue("Task with name '$testTaskName' should be displayed", taskObject.exists())
                        break
                    } else {
                        Log.i("DittoSyncTest", "Task not found yet, attempt $attempt/$maxRetries")
                        Thread.sleep(1000)
                    }
                }

                if (!taskFound) {
                    // Debug: Log what's actually visible on screen
                    Log.e("DittoSyncTest", "❌ TASK NOT FOUND: '$testTaskName'")
                    Log.i("DittoSyncTest", "🔍 Debugging: Checking what's visible on screen...")

                    // Dump UI hierarchy for debugging
                    device.dumpWindowHierarchy(System.out)

                    throw AssertionError("Task with name '$testTaskName' was not found after ${maxRetries} seconds")
                }

                // Keep screen visible for BrowserStack video verification
                // This delay is required for BrowserStack test recording to capture the successful state
                Thread.sleep(3000)
            }
        } catch (e: Exception) {
            Log.e("DittoSyncTest", "❌ TEST FAILED: ${e.message}")
            throw e
        }
    }

    @Test
    fun testActivityLaunches() {
        // Simple smoke test to ensure the app launches correctly
        Log.i("DittoSyncTest", "Testing that MainActivity launches successfully")

        // Launch activity manually with proper error handling
        Log.i("DittoSyncTest", "Launching MainActivity...")
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                Log.i("DittoSyncTest", "Activity launched successfully")

                // Wait for app to appear on screen
                val appPackage = "live.ditto.quickstart.taskscpp"
                device.wait(Until.hasObject(androidx.test.uiautomator.By.pkg(appPackage)), 10000)

                // Wait for content to load
                Thread.sleep(2000)

                // Verify the app title is displayed using UiAutomator
                val titleObject: UiObject = device.findObject(UiSelector().textContains("Ditto Tasks C++"))
                assertTrue("App title should be displayed", titleObject.exists())

                Log.i("DittoSyncTest", "✅ ACTIVITY LAUNCHED SUCCESSFULLY")
            }
        } catch (e: Exception) {
            Log.e("DittoSyncTest", "❌ ACTIVITY FAILED TO LAUNCH: ${e.message}")
            throw e
        }
    }
}