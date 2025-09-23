import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.testng.Assert
import org.testng.annotations.*
import java.net.URL
import java.time.Duration

/**
 * True Appium E2E test that launches the android-cpp Tasks app externally
 * and verifies that a task name from environment variable appears on screen.
 *
 * This test is BrowserStack compatible and launches the app as-is (like a user would).
 */
class AppiumE2ETest {

    private lateinit var driver: AndroidDriver
    private lateinit var wait: WebDriverWait

    @BeforeMethod
    fun setUp() {
        // Appium server URL - try multiple sources for BrowserStack detection
        val appiumServerUrl = System.getProperty("appium.server.url")
            ?: System.getenv("APPIUM_SERVER_URL")
            ?: "http://127.0.0.1:4723"

        val isBrowserStack = appiumServerUrl.contains("browserstack.com") ||
                           System.getenv("BROWSERSTACK_USERNAME") != null

        val options = UiAutomator2Options().apply {
            if (isBrowserStack) {
                // BrowserStack capabilities (same as working connection test)
                println("🌐 Configuring for BrowserStack...")
                setPlatformName("Android")
                setDeviceName("Google Pixel 7")
                setPlatformVersion("13.0")

                // Set app URL from system property or environment variable
                val appUrl = System.getProperty("app.url")
                    ?: System.getenv("BROWSERSTACK_APP_URL")
                    ?: "bs://71e2150dc22ea83e959170c6fd9d3bbcd8f0d557"
                setCapability("app", appUrl)

                // BrowserStack specific capabilities
                setCapability("project", "Ditto SDK Android CPP")
                setCapability("build", "Appium E2E Tests")
                setCapability("name", "Task Sync Verification")
                setCapability("automationName", "UiAutomator2")

                println("📱 Using app URL: $appUrl")
                println("📱 Device: Google Pixel 7 (Android 13.0)")

            } else {
                // Local testing capabilities
                println("🏠 Configuring for local testing...")
                setDeviceName("Android Emulator")
                setPlatformName("Android")
                setPlatformVersion("16")  // Match actual emulator version

                // App details - will launch the installed app locally
                setAppPackage("live.ditto.quickstart.taskscpp")
                setAppActivity("live.ditto.quickstart.tasks.MainActivity")

                setCapability("automationName", "UiAutomator2")
                setCapability("noReset", true)
                setCapability("fullReset", false)
            }
        }

        // Determine final server URL for connection
        val finalServerUrl = if (isBrowserStack) {
            val username = System.getenv("BROWSERSTACK_USERNAME")
            val accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY")
            if (username != null && accessKey != null) {
                "https://$username:$accessKey@hub-cloud.browserstack.com/wd/hub"
            } else {
                appiumServerUrl
            }
        } else {
            appiumServerUrl
        }

        println("🚀 Starting Appium test...")
        println("📱 Connecting to Appium server: $finalServerUrl")
        println("📦 App package: live.ditto.quickstart.taskscpp")

        driver = AndroidDriver(URL(finalServerUrl), options)
        wait = WebDriverWait(driver, Duration.ofSeconds(30))

        println("✅ Appium driver initialized successfully")
    }

    @AfterMethod
    fun tearDown() {
        if (::driver.isInitialized) {
            println("🔄 Closing Appium session...")
            driver.quit()
        }
    }

    @Test
    fun testTaskFromEnvironmentVariableAppears() {
        // Get task name from environment variable
        val testTaskName = System.getenv("GITHUB_TEST_DOC_ID")
            ?: System.getProperty("GITHUB_TEST_DOC_ID")
            ?: throw IllegalStateException("No test task name provided. Set GITHUB_TEST_DOC_ID environment variable")

        println("🔍 Testing with task name: '$testTaskName'")

        try {
            // Give app time to launch
            println("⏳ Waiting for app to launch...")
            Thread.sleep(3000)

            // Handle permission dialog if it appears
            try {
                val allowButton = driver.findElement(By.id("com.android.permissioncontroller:id/permission_allow_button"))
                if (allowButton.isDisplayed) {
                    println("📱 Permission dialog found - clicking 'Allow'")
                    allowButton.click()
                    Thread.sleep(2000) // Wait for permission to be granted
                }
            } catch (e: Exception) {
                println("ℹ️ No permission dialog found, continuing...")
            }

            // Wait for Ditto sync after permission granted
            println("⏳ Waiting for Ditto sync...")
            Thread.sleep(6000)

            // Search for the task using multiple strategies
            println("🔍 Searching for task: '$testTaskName'")

            var taskFound = false
            val maxAttempts = 12

            for (attempt in 1..maxAttempts) {
                try {
                    // Strategy 1: Exact text match
                    val taskElement = driver.findElement(By.xpath("//*[@text='$testTaskName']"))
                    if (taskElement.isDisplayed) {
                        println("✅ TASK FOUND using exact text match on attempt $attempt")
                        taskFound = true
                        break
                    }
                } catch (e: Exception) {
                    // Strategy 2: Contains text match
                    try {
                        val taskElement = driver.findElement(By.xpath("//*[contains(@text, '$testTaskName')]"))
                        if (taskElement.isDisplayed) {
                            println("✅ TASK FOUND using contains text match on attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e2: Exception) {
                        println("⏳ Task not found yet, attempt $attempt/$maxAttempts")
                        Thread.sleep(1000)
                    }
                }
            }

            if (!taskFound) {
                // Debug: Print page source for troubleshooting
                println("❌ TASK NOT FOUND. Dumping page source for debugging:")
                println("=".repeat(50))
                println(driver.pageSource)
                println("=".repeat(50))

                Assert.fail("Task with name '$testTaskName' was not found after $maxAttempts attempts")
            }

            println("🎉 Test completed successfully!")

            // Mark test as passed in BrowserStack
            try {
                driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \"Task found successfully\"}}")
            } catch (e: Exception) {
                println("ℹ️ Could not set BrowserStack status (not critical): ${e.message}")
            }

            // Keep app visible for video recording (BrowserStack)
            Thread.sleep(3000)

        } catch (e: Exception) {
            println("❌ Test failed: ${e.message}")

            // Mark test as failed in BrowserStack
            try {
                driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"${e.message}\"}}")
            } catch (statusError: Exception) {
                println("ℹ️ Could not set BrowserStack failed status (not critical): ${statusError.message}")
            }

            // Take screenshot for debugging
            try {
                val screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BASE64)
                println("📸 Screenshot captured (base64): ${screenshot.substring(0, 50)}...")
            } catch (screenshotError: Exception) {
                println("❌ Failed to capture screenshot: ${screenshotError.message}")
            }

            throw e
        }
    }

    @Test(enabled = false)
    fun testAppLaunches() {
        println("🚀 Testing that app launches correctly...")

        try {
            // Verify app title appears
            val titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(@text, 'Ditto Tasks')]")
            ))

            Assert.assertTrue(titleElement.isDisplayed, "App title should be visible")
            println("✅ App launched and title is visible")

        } catch (e: Exception) {
            println("❌ App launch test failed: ${e.message}")
            throw e
        }
    }
}