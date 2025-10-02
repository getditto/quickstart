package com.ditto.quickstart.kmp

import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.ios.options.XCUITestOptions
import org.openqa.selenium.By
import org.testng.Assert
import org.testng.annotations.*
import java.net.URL

/** Appium E2E test for KMP iOS Tasks app */
class KMPiOSAppiumTest {

    private lateinit var driver: IOSDriver

    @BeforeMethod
    fun setUp() {
        val isBrowserStack = System.getenv("BROWSERSTACK_USERNAME") != null

        val options = XCUITestOptions().apply {
            if (isBrowserStack) {
                setPlatformName("iOS")
                setDeviceName("iPhone 15")
                setPlatformVersion("17.0")
                setCapability("app", System.getenv("BROWSERSTACK_APP_URL"))
                setCapability("project", "Ditto SDK Kotlin Multiplatform iOS")
                setCapability("build", "Appium E2E Tests - KMP iOS")
                setCapability("name", "Task Sync Verification - iOS")
                setCapability("automationName", "XCUITest")
                setCapability("autoAcceptAlerts", true)
            } else {
                setDeviceName("iPhone 16 Pro")
                setPlatformName("iOS")
                setPlatformVersion("18.5")
                setCapability("bundleId", "live.ditto.quickstart.QuickStartTasks")
                setCapability("automationName", "XCUITest")
                setCapability("autoAcceptAlerts", true)
                setCapability("udid", "7B8D3954-463E-42AF-9C57-EF52044DE23D")
            }
        }

        val serverUrl = if (isBrowserStack) {
            val username = System.getenv("BROWSERSTACK_USERNAME")
            val accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY")
            "https://$username:$accessKey@hub-cloud.browserstack.com/wd/hub"
        } else {
            "http://127.0.0.1:4723"
        }

        driver = IOSDriver(URL(serverUrl), options)
    }

    @AfterMethod
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    @Test
    fun testTaskFromEnvironmentVariableAppears() {
        val testTaskName = System.getenv("GITHUB_TEST_DOC_ID")
            ?: throw IllegalStateException("GITHUB_TEST_DOC_ID environment variable not set")

        try {
            // Wait for app to launch and initialize Ditto
            println("⏳ Waiting for app to launch and Ditto to initialize...")
            Thread.sleep(10000)

            // Trigger accessibility tree sync by requesting page source (for lazy loading)
            try {
                println("🔄 Triggering accessibility tree lazy sync...")
                val initialPageSource = driver.pageSource
                Thread.sleep(2000) // Give it time to fully sync
                println("✓ Accessibility tree requested")
            } catch (e: Exception) {
                println("⚠️ Error triggering accessibility sync: ${e.message}")
            }

            // Debug: Print all elements AFTER triggering sync
            try {
                println("📋 Checking all elements after sync trigger...")
                val allElements = driver.findElements(By.xpath("//*"))
                println("📋 Found ${allElements.size} total elements")

                // Look specifically for accessible elements
                val accessibleElements = allElements.filter { elem ->
                    try {
                        val accessible = elem.getAttribute("accessible")
                        val name = elem.getAttribute("name")
                        val label = elem.getAttribute("label")
                        accessible == "true" || !name.isNullOrEmpty() || !label.isNullOrEmpty()
                    } catch (e: Exception) {
                        false
                    }
                }
                println("📋 Found ${accessibleElements.size} accessible/named elements")

                accessibleElements.take(10).forEach { elem ->
                    try {
                        println("  ✓ ${elem.tagName}: name=${elem.getAttribute("name")}, label=${elem.getAttribute("label")}, accessible=${elem.getAttribute("accessible")}")
                    } catch (e: Exception) {
                        println("  - ${elem.tagName}: (error reading attributes)")
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Error listing elements: ${e.message}")
            }

            // Wait for sync with extended timeout for BrowserStack environments
            val maxWaitSeconds = System.getenv("SYNC_MAX_WAIT_SECONDS")?.toIntOrNull() ?: 30
            println("⏳ Waiting up to ${maxWaitSeconds}s for task '$testTaskName' to sync...")

            var taskFound = false
            val startTime = System.currentTimeMillis()
            var attempt = 0

            while (System.currentTimeMillis() - startTime < maxWaitSeconds * 1000 && !taskFound) {
                attempt++
                try {
                    // Try multiple strategies to find the task in Compose Multiplatform UI

                    // Generate the testTag that Compose uses (task_title_<sanitized_title>)
                    val sanitizedTestTag = "task_title_${testTaskName.replace(" ", "_").replace(Regex("[^A-Za-z0-9_]"), "")}"

                    // Strategy 1: Find by accessibility ID (testTag)
                    try {
                        val taskElement = driver.findElement(By.id(sanitizedTestTag))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using accessibility ID at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Continue to next strategy
                    }

                    // Strategy 2: Find by accessibility ID in XPath
                    try {
                        val taskElement = driver.findElement(By.xpath("//*[@accessibilityId='$sanitizedTestTag']"))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using accessibilityId XPath at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Continue to next strategy
                    }

                    // Strategy 3: Find by exact text match (XCUIElementTypeStaticText)
                    try {
                        val taskElement = driver.findElement(By.xpath("//XCUIElementTypeStaticText[@name='$testTaskName']"))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using exact text match at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Continue to next strategy
                    }

                    // Strategy 4: Find by label containing text
                    try {
                        val taskElement = driver.findElement(By.xpath("//XCUIElementTypeStaticText[@label='$testTaskName']"))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using label match at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Continue to next strategy
                    }

                    // Strategy 5: Find any element with accessibilityId matching testTag
                    try {
                        val taskElement = driver.findElement(By.xpath("//*[@name='$sanitizedTestTag']"))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using name=testTag at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Continue to next strategy
                    }

                    // Strategy 6: Find in any element (broader search)
                    try {
                        val taskElement = driver.findElement(By.xpath("//*[contains(@name, '$testTaskName') or contains(@label, '$testTaskName') or @name='$sanitizedTestTag']"))
                        if (taskElement.isDisplayed) {
                            println("✅ Found task using broad search at attempt $attempt")
                            taskFound = true
                            break
                        }
                    } catch (e: Exception) {
                        // Wait before retry
                        if (attempt % 5 == 0) {
                            println("⏳ Attempt $attempt: Task not found yet, continuing to wait...")
                        }
                        Thread.sleep(2000)
                    }

                } catch (e: Exception) {
                    if (attempt % 5 == 0) {
                        println("⚠️ Attempt $attempt failed: ${e.message}")
                    }
                    Thread.sleep(2000)
                }
            }

            if (!taskFound) {
                // Log page source for debugging
                try {
                    val pageSource = driver.pageSource
                    println("📄 Page source at failure:\n$pageSource")
                } catch (e: Exception) {
                    println("⚠️ Could not retrieve page source: ${e.message}")
                }

                // Mark test as failed in BrowserStack before assertion
                try {
                    driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"Task '$testTaskName' not found after ${attempt} attempts over ${maxWaitSeconds}s\"}}")
                } catch (e: Exception) {
                    // Ignore - not running on BrowserStack
                }

                Assert.fail("Task '$testTaskName' not found after ${attempt} attempts over ${maxWaitSeconds}s")
            }

            // Mark test as passed in BrowserStack
            driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \"Task found successfully after $attempt attempts\"}}")

        } catch (e: Exception) {
            // Mark test as failed in BrowserStack
            try {
                driver.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \"${e.message}\"}}")
            } catch (e2: Exception) {
                // Ignore - not running on BrowserStack
            }
            throw e
        }
    }
}