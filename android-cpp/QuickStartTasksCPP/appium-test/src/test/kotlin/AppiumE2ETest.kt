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
        // Appium server URL - local by default, can be overridden for BrowserStack
        val appiumServerUrl = System.getProperty("appium.server.url") ?: "http://127.0.0.1:4723"
        val isBrowserStack = appiumServerUrl.contains("browserstack.com")

        val options = UiAutomator2Options().apply {
            if (isBrowserStack) {
                // BrowserStack capabilities (same as working connection test)
                println("🌐 Configuring for BrowserStack...")
                setPlatformName("Android")
                setDeviceName("Google Pixel 7")
                setPlatformVersion("13.0")

                // Set app URL from system property
                val appUrl = System.getProperty("app.url") ?: "bs://71e2150dc22ea83e959170c6fd9d3bbcd8f0d557"
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

        println("🚀 Starting Appium test...")
        println("📱 Connecting to Appium server: $appiumServerUrl")
        println("📦 App package: live.ditto.quickstart.taskscpp")

        driver = AndroidDriver(URL(appiumServerUrl), options)
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
            // Wait for app to launch and load
            println("⏳ Waiting for app to launch...")
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(@text, 'Ditto Tasks')]")
            ))
            println("✅ App launched successfully")

            // Wait for Ditto sync and UI to populate
            println("⏳ Waiting for Ditto sync (6 seconds)...")
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

            // Keep app visible for video recording (BrowserStack)
            Thread.sleep(3000)

        } catch (e: Exception) {
            println("❌ Test failed: ${e.message}")

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

    @Test
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