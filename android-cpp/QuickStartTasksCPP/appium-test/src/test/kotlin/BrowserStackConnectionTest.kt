import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.testng.annotations.*
import java.net.URL

/**
 * Simple test to verify BrowserStack connection
 */
class BrowserStackConnectionTest {

    @Test
    fun testBrowserStackConnection() {
        println("🌐 Testing BrowserStack connection...")

        val options = UiAutomator2Options().apply {
            // BrowserStack required capabilities
            setPlatformName("Android")
            setDeviceName("Google Pixel 7")
            setPlatformVersion("13.0")
            setCapability("app", "bs://71e2150dc22ea83e959170c6fd9d3bbcd8f0d557")

            // Additional BrowserStack capabilities
            setCapability("project", "Ditto SDK Android CPP")
            setCapability("build", "Connection Test")
            setCapability("name", "BrowserStack Connection Verification")
            setCapability("automationName", "UiAutomator2")
        }

        val serverUrl = "https://teodorciuraru_g4ijVa:4hJsNzsc2BXiEiu9ktDt@hub-cloud.browserstack.com/wd/hub"

        println("📱 Connecting to: $serverUrl")
        println("📱 Device: Google Pixel 7 (Android 13.0)")
        println("📱 App: bs://71e2150dc22ea83e959170c6fd9d3bbcd8f0d557")

        try {
            val driver = AndroidDriver(URL(serverUrl), options)
            println("✅ BrowserStack connection successful!")
            println("📱 Session ID: ${driver.sessionId}")

            // Wait a moment to see the app launch
            Thread.sleep(5000)

            println("📱 App context: ${driver.context}")
            println("📱 Current activity: ${driver.currentActivity()}")

            driver.quit()
            println("✅ Test completed successfully!")

        } catch (e: Exception) {
            println("❌ BrowserStack connection failed:")
            println("Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}