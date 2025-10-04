package com.ditto.example.spring.quickstart;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for Task visibility using BrowserStack.
 * Tests the web UI by checking if tasks are properly visible.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskVisibilityIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setupWebDriver() {
        String browserStackUser = firstNonEmpty(
                System.getProperty("BROWSERSTACK_USERNAME"),
                System.getenv("BROWSERSTACK_USERNAME")
        );
        String browserStackKey = firstNonEmpty(
                System.getProperty("BROWSERSTACK_ACCESS_KEY"),
                System.getenv("BROWSERSTACK_ACCESS_KEY")
        );

        if (browserStackUser != null && browserStackKey != null) {
            setupBrowserStackDriver(browserStackUser, browserStackKey);
        } else {
            setupLocalChromeDriver();
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    private static void setupBrowserStackDriver(String username, String accessKey) {
        try {
            ChromeOptions options = new ChromeOptions();

            Map<String, Object> bsOptions = new HashMap<>();
            bsOptions.put("sessionName", "Java Spring Task Visibility Test");

            String bsLocal = firstNonEmpty(System.getProperty("BROWSERSTACK_LOCAL"), System.getenv("BROWSERSTACK_LOCAL"));
            if ("true".equals(bsLocal)) {
                bsOptions.put("local", "true");
            }

            String buildName = System.getProperty("BROWSERSTACK_BUILD_NAME");
            if (buildName != null && !buildName.isEmpty()) {
                bsOptions.put("buildName", buildName);
            }

            options.setCapability("bstack:options", bsOptions);

            RemoteWebDriver remote = new RemoteWebDriver(
                new URL("https://" + username + ":" + accessKey + "@hub.browserstack.com/wd/hub"),
                options
            );
            driver = remote;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BrowserStack WebDriver: " + e.getMessage(), e);
        }
    }

    private static void setupLocalChromeDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            if (System.getenv("CI") != null) {
                options.addArguments("--headless=new");
            }
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1200,800");

            driver = new ChromeDriver(options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize local Chrome WebDriver: " + e.getMessage(), e);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void shouldPassWithExistingTask() {
        String envTitle = firstNonEmpty(
                System.getenv("DITTO_CLOUD_TASK_TITLE"),
                System.getProperty("DITTO_CLOUD_TASK_TITLE")
        );

        Assertions.assertNotNull(envTitle, "DITTO_CLOUD_TASK_TITLE must be provided for testing");

        driver.get("http://localhost:8080");
        wait.until(ExpectedConditions.titleContains("Ditto"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder*='Task']")));

        // Enable sync if disabled
        enableSyncIfDisabled();

        // Wait for tasks to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean taskFound = isTaskVisibleOnPage(envTitle);
        Assertions.assertTrue(taskFound, "Task should be visible in the UI: " + envTitle);
    }

    private boolean isTaskVisibleOnPage(String taskTitle) {
        try {
            // Use safer approach: get all text elements and filter programmatically
            // This avoids XPath injection by not concatenating user input into XPath
            List<WebElement> allTextElements = driver.findElements(By.xpath("//*[text()]"));

            for (WebElement element : allTextElements) {
                String elementText = element.getText().trim();
                if (elementText.equals(taskTitle)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // Fallback to page source search (also safe from injection)
            String pageText = driver.getPageSource();
            return pageText.contains(taskTitle);
        }
    }

    private void enableSyncIfDisabled() {
        try {
            // Use CSS selector for more reliable element location
            List<WebElement> allElements = driver.findElements(By.cssSelector("*"));

            for (WebElement element : allElements) {
                String text = element.getText();
                if (text.contains("Sync State: false")) {
                    // Find toggle button using CSS selector instead of XPath
                    List<WebElement> buttons = driver.findElements(By.cssSelector("button"));
                    for (WebElement button : buttons) {
                        if ("Toggle".equals(button.getText().trim())) {
                            button.click();
                            Thread.sleep(2000);
                            return;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not enable sync: " + e.getMessage());
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Warning: Error during WebDriver cleanup: " + e.getMessage());
            }
        }
    }
}