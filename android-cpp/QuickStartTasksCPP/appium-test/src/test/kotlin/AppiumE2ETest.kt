import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.By
import org.testng.Assert
import org.testng.annotations.*
import java.net.URL

/** Appium E2E test for android-cpp Tasks app */
class AppiumE2ETest {

    private lateinit var driver: AndroidDriver

    @BeforeMethod
    fun setUp() {
        val isBrowserStack = System.getenv("BROWSERSTACK_USERNAME") != null

        val options = UiAutomator2Options().apply {
            if (isBrowserStack) {
                // Load device config from environment (set by workflow from browserstack-devices.yml)
                val deviceString = System.getenv("BROWSERSTACK_DEVICE")
                    ?: throw IllegalStateException("BROWSERSTACK_DEVICE environment variable not set")
                val deviceParts = deviceString.split("-")
                val deviceName = deviceParts[0]
                val platformVersion = deviceParts.getOrElse(1) {
                    throw IllegalStateException("Invalid BROWSERSTACK_DEVICE format: $deviceString (expected 'Device Name-Version')")
                }

                setPlatformName("Android")
                setDeviceName(deviceName)
                setPlatformVersion(platformVersion)
                setCapability("app", System.getenv("BROWSERSTACK_APP_URL"))
                setCapability("project", "QuickStart Android CPP")
                setCapability("build", "CI Build #${System.getenv("GITHUB_RUN_NUMBER") ?: "Local"}")
                setCapability("name", "Task Sync Verification")
                setCapability("automationName", "UiAutomator2")
            } else {
                setDeviceName("Android Emulator")
                setPlatformName("Android")
                setAppPackage("live.ditto.quickstart.taskscpp")
                setAppActivity("live.ditto.quickstart.tasks.MainActivity")
                setCapability("automationName", "UiAutomator2")
                setCapability("noReset", true)
            }
        }

        val serverUrl = if (isBrowserStack) {
            val username = System.getenv("BROWSERSTACK_USERNAME")
            val accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY")
            "https://$username:$accessKey@hub-cloud.browserstack.com/wd/hub"
        } else {
            "http://127.0.0.1:4723"
        }

        driver = AndroidDriver(URL(serverUrl), options)
    }

    @AfterMethod
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    @Test
    fun testTaskFromEnvironmentVariableAppears() {
        val testTaskName = System.getenv("DITTO_CLOUD_TASK_TITLE")
            ?: throw IllegalStateException("DITTO_CLOUD_TASK_TITLE environment variable not set")

        try {
            Thread.sleep(3000)
            try {
                val allowButton = driver.findElement(By.id("com.android.permissioncontroller:id/permission_allow_button"))
                if (allowButton.isDisplayed) {
                    allowButton.click()
                    Thread.sleep(2000)
                }
            } catch (e: Exception) {
                // Permission dialog not found - continue
            }
            Thread.sleep(6000)

            var taskFound = false
            for (attempt in 1..10) {
                try {
                    val taskElement = driver.findElement(By.xpath("//*[@text='$testTaskName']"))
                    if (taskElement.isDisplayed) {
                        taskFound = true
                        break
                    }
                } catch (e: Exception) {
                    try {
                        val taskElement = driver.findElement(By.xpath("//*[contains(@text, '$testTaskName')]"))
                        if (taskElement.isDisplayed) {
                            taskFound = true
                            break
                        }
                    } catch (e2: Exception) {
                        Thread.sleep(1000)
                    }
                }
            }

            if (!taskFound) {
                Assert.fail("Task '$testTaskName' not found")
            }

            driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \"Task found successfully\"}}")

        } catch (e: Exception) {
            driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"${e.message}\"}}")
            throw e
        }
    }

}
