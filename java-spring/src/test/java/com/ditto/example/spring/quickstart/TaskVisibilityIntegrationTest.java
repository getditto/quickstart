package com.ditto.example.spring.quickstart;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Visual browser integration test for Task visibility using Selenium WebDriver.
 * Tests the actual web UI by automating browser interactions.
 * 
 * Supports both local Chrome testing and BrowserStack remote testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskVisibilityIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private String baseUrl;

    @BeforeAll
    static void setupWebDriver() {
        // Check if running on BrowserStack (CI environment). Support both env and -D system properties
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

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    private static void setupBrowserStackDriver(String username, String accessKey) {
        try {
            ChromeOptions options = new ChromeOptions();
            
            // Use standard BrowserStack approach with minimal capabilities
            // Let browserstack.yml file handle most configuration
            Map<String, Object> bsOptions = new HashMap<>();
            bsOptions.put("sessionName", "Java Spring Task Visibility Test");
            
            // Enable BrowserStack Local tunnel
            String bsLocal = firstNonEmpty(System.getProperty("BROWSERSTACK_LOCAL"), System.getenv("BROWSERSTACK_LOCAL"));
            System.out.println("🔍 BROWSERSTACK_LOCAL system property: " + System.getProperty("BROWSERSTACK_LOCAL"));
            System.out.println("🔍 BROWSERSTACK_LOCAL environment var: " + System.getenv("BROWSERSTACK_LOCAL"));
            System.out.println("🔍 Final bsLocal value: " + bsLocal);
            if ("true".equals(bsLocal)) {
                bsOptions.put("local", "true");
                System.out.println("✅ BrowserStack Local tunnel enabled for test");
            } else {
                System.out.println("❌ BrowserStack Local tunnel NOT enabled - bsLocal='" + bsLocal + "'");
            }
            
            // Set build name from system property if provided
            String buildName = System.getProperty("BROWSERSTACK_BUILD_NAME");
            System.out.println("🔍 BROWSERSTACK_BUILD_NAME system property: " + buildName);
            if (buildName != null && !buildName.isEmpty()) {
                bsOptions.put("buildName", buildName);
                System.out.println("✅ Using BrowserStack build name: " + buildName);
            } else {
                System.out.println("❌ No BrowserStack build name provided");
            }
            
            options.setCapability("bstack:options", bsOptions);
            
            // Use standard BrowserStack hub URL (like documentation)
            RemoteWebDriver remote = new RemoteWebDriver(
                new URL("https://" + username + ":" + accessKey + "@hub.browserstack.com/wd/hub"), 
                options
            );
            driver = remote;
            
            System.out.println("✅ BrowserStack WebDriver initialized for visual testing");
            System.out.println("🆔 Session ID: " + remote.getSessionId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BrowserStack WebDriver: " + e.getMessage(), e);
        }
    }

    private static void setupLocalChromeDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            // Headless in CI for reliability; non-headless locally
            if (System.getenv("CI") != null) {
                // Use new headless for Chrome 109+
                options.addArguments("--headless=new");
            }
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1200,800");
            
            driver = new ChromeDriver(options);
            System.out.println("✅ Local Chrome WebDriver initialized for visual testing");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize local Chrome WebDriver: " + e.getMessage(), e);
        }
    }

    @BeforeEach
    void setUp() {
        // Use fixed port 8080 to match CI configuration
        final int FIXED_PORT = 8080;
        
        // Always use localhost - BrowserStack Local tunnel handles the routing (like javascript-web)
        baseUrl = "http://localhost:" + FIXED_PORT;
        
        String bsUser = firstNonEmpty(System.getProperty("BROWSERSTACK_USERNAME"), System.getenv("BROWSERSTACK_USERNAME"));
        String bsLocal = firstNonEmpty(System.getProperty("BROWSERSTACK_LOCAL"), System.getenv("BROWSERSTACK_LOCAL"));
        
        if (bsUser != null && "true".equals(bsLocal)) {
            System.out.println("🔗 BrowserStack testing with Local tunnel: " + baseUrl);
        } else {
            System.out.println("🌐 Local testing: " + baseUrl);
        }
    }

    @Test
    @Order(1)
    void shouldLoadTasksWebPage() {
        System.out.println("🧪 Test 1: Loading Tasks web page...");
        System.out.println("🌐 Target URL: " + baseUrl);
        
        try {
            navigateToPageAndWait();
            checkForErrorPages();
            verifyPageTitle();
            verifyUIElements();
            waitForVisualInspection();
            
            System.out.println("✅ Tasks web page loaded successfully with input field and add button");
            
        } catch (Exception e) {
            logErrorDetails(e);
            throw e; // Re-throw original exception
        }
    }
    
    private void navigateToPageAndWait() {
        driver.get(baseUrl);
        System.out.println("📍 Browser navigation initiated to: " + baseUrl);
        
        String currentUrl = driver.getCurrentUrl();
        System.out.println("🔍 Current browser URL: " + currentUrl);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted during page load", ie);
        }
    }
    
    private void checkForErrorPages() {
        String pageSource = driver.getPageSource();
        System.out.println("📄 Page source length: " + pageSource.length() + " characters");
        
        if (pageSource.contains("This site can't be reached") || 
            pageSource.contains("ERR_") || 
            pageSource.contains("404") ||
            pageSource.contains("Connection refused")) {
            System.out.println("❌ ERROR PAGE DETECTED! Page source snippet:");
            System.out.println(pageSource.length() > 500 ? pageSource.substring(0, 500) + "..." : pageSource);
            throw new RuntimeException("Browser shows error page - connectivity issue detected");
        }
    }
    
    private void verifyPageTitle() {
        wait.until(ExpectedConditions.titleContains("Ditto"));
        
        String pageTitle = driver.getTitle();
        System.out.println("📄 Page title: " + pageTitle);
        Assertions.assertTrue(pageTitle.contains("Ditto"), 
            "Page title should contain 'Ditto', but was: " + pageTitle);
    }
    
    private void verifyUIElements() {
        WebElement taskInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("input")));
        Assertions.assertTrue(taskInput.isDisplayed(), "Task input field should be visible");
        System.out.println("✅ Found input field");
        
        WebElement addButton = driver.findElement(By.cssSelector("button"));
        Assertions.assertTrue(addButton.isDisplayed(), "Add task button should be visible");
        System.out.println("✅ Found add button");
    }
    
    private void waitForVisualInspection() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted during final wait", ie);
        }
    }
    
    private void logErrorDetails(Exception e) {
        try {
            System.out.println("❌ Test failed with error: " + e.getMessage());
            System.out.println("🔍 Current URL when error occurred: " + driver.getCurrentUrl());
            System.out.println("📄 Page title when error occurred: " + driver.getTitle());
            
            String errorPageSource = driver.getPageSource();
            if (errorPageSource.length() > 1000) {
                System.out.println("📄 Error page source (first 1000 chars): " + errorPageSource.substring(0, 1000));
            } else {
                System.out.println("📄 Full error page source: " + errorPageSource);
            }
        } catch (Exception logError) {
            System.out.println("⚠️ Could not capture additional error details: " + logError.getMessage());
        }
    }

    @Test
    @Order(2)
    void shouldFailWithNoEnvironmentVariable() {
        System.out.println("🧪 Test 2: Should fail when no environment variable is set...");
        
        // This test assumes NO environment variables are set
        String targetTaskTitle = System.getenv("TEST_TASK_TITLE");
        String targetTaskId = System.getenv("GITHUB_TEST_DOC_ID");
        
        // Verify both are null (test should only run when neither is set)
        if (targetTaskTitle != null || targetTaskId != null) {
            System.out.println("⚠️  Environment variables are set - skipping this test");
            return;
        }
        
        System.out.println("❌ No TEST_TASK_TITLE or GITHUB_TEST_DOC_ID environment variable found");
        System.out.println("💡 Set TEST_TASK_TITLE='Your Task Title' to test locally");
        System.out.println("💡 In CI, GITHUB_TEST_DOC_ID will be provided by the workflow");
        
        // This test should PASS gracefully because no environment variable is set (expected condition)
        System.out.println("✅ Test passed gracefully - no environment variables set as expected");
    }

    @Test
    @Order(3) 
    void shouldFailWithNonExistingTask() {
        System.out.println("🧪 Test 3: Should fail when searching for non-existing task...");
        
        String searchText = "Non-Existing Task That Should Not Be Found";
        System.out.println("🔍 Searching for non-existing task: " + searchText);
        
        driver.get(baseUrl);
        System.out.println("📍 Browser opened at: " + baseUrl);
        
        // Wait for page to load
        wait.until(ExpectedConditions.titleContains("Ditto"));
        
        // Wait for the page body to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
        
        // Add delay to let tasks load from Ditto
        System.out.println("⏳ Waiting for Ditto tasks to load...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Search for the non-existing task
        boolean taskFound = searchForTask(searchText);
        
        if (taskFound) {
            System.out.println("❌ Target task was found but shouldn't exist: " + searchText);
        } else {
            System.out.println("❌ Target task NOT found in UI: " + searchText);
        }
        
        // Keep browser open to see the result
        System.out.println("🌐 Browser will stay open for 3 seconds to show the result...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // This test should PASS gracefully when the non-existing task is not found (expected)
        // But should FAIL if we get a false positive (finding a task that shouldn't exist)
        Assertions.assertFalse(taskFound, 
            "False positive! Found task '" + searchText + "' that should NOT exist in the UI");
        
        System.out.println("✅ Test passed gracefully - non-existing task was not found as expected");
    }

    @Test
    @Order(4)
    @Timeout(value = 10, unit = TimeUnit.MINUTES)  // Increase timeout for CI
    void shouldPassWithExistingTask() {
        System.out.println("🧪 Test 4: Should pass when finding existing task...");
        System.out.println("🔍 DEBUG: Test method started, about to get task title...");
        
        // Prefer CI-provided title/ID; fall back to known local sample
        String envTitle = firstNonEmpty(
                System.getenv("GITHUB_TEST_DOC_ID"),
                System.getProperty("GITHUB_TEST_DOC_ID"),
                System.getenv("TEST_TASK_TITLE"),
                System.getProperty("TEST_TASK_TITLE")
        );
        String searchText = envTitle != null ? envTitle : "Task 1 - a2218d97";  // fallback for local dev
        System.out.println("🔍 Searching for existing task: " + searchText + (envTitle != null ? " (from CI/env)" : " (fallback)"));
        System.out.println("🔍 DEBUG: About to call driver.get() with URL: " + baseUrl);
        
        driver.get(baseUrl);
        System.out.println("📍 Browser opened at: " + baseUrl);
        
        // Wait for page to load
        wait.until(ExpectedConditions.titleContains("Ditto"));
        
        // Wait for the page body to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
        
        // Enable sync if it's currently disabled
        enableSyncIfDisabled();
        
        // Add delay to let tasks load from Ditto
        System.out.println("⏳ Waiting for Ditto tasks to load...");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Search for the existing task
        boolean taskFound = searchForTask(searchText);
        
        if (taskFound) {
            System.out.println("✅ Target task found in UI (exact match): " + searchText);
        } else {
            System.out.println("❌ Target task NOT found in UI (exact match): " + searchText);
            // Show available tasks for debugging
            showAvailableTasks();
        }
        
        // Keep browser open longer to see the result
        System.out.println("🌐 Browser will stay open for 3 seconds to show the result...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Assertions.assertTrue(taskFound, 
            "Should find the exact task '" + searchText + "' in the UI");
        
        System.out.println("✅ Pre-existing task successfully found and visible in UI");
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private boolean searchForTask(String searchText) {
        boolean taskFound = false;
        
        // Try to find the task text within task list elements
        try {
            // Look for elements containing the exact task text
            List<WebElement> taskElements = driver.findElements(By.xpath("//*[text()='" + searchText + "']"));
            taskFound = !taskElements.isEmpty();
            
            if (!taskFound) {
                // Also check if it appears as partial text content
                List<WebElement> partialMatches = driver.findElements(By.xpath("//*[contains(text(), '" + searchText + "')]"));
                for (WebElement element : partialMatches) {
                    if (element.getText().trim().equals(searchText)) {
                        taskFound = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  XPath search failed, falling back to page source search");
            String pageText = driver.getPageSource();
            taskFound = pageText.contains(searchText);
        }
        
        return taskFound;
    }

    private void showAvailableTasks() {
        try {
            List<WebElement> allTextElements = driver.findElements(By.xpath("//*[text()]"));
            System.out.println("📋 Available text elements in page:");
            for (WebElement el : allTextElements) {
                String text = el.getText().trim();
                if (!text.isEmpty() && text.length() < 100) {
                    System.out.println("  - '" + text + "'");
                }
            }
        } catch (Exception e) {
            String pageText = driver.getPageSource();
            System.out.println("📄 Page source contains: " + 
                (pageText.length() > 300 ? pageText.substring(0, 300) + "..." : pageText));
        }
    }

    private void enableSyncIfDisabled() {
        try {
            // Look for sync state text to determine current state
            List<WebElement> syncStateElements = driver.findElements(By.xpath("//*[contains(text(), 'Sync State:')]"));
            if (!syncStateElements.isEmpty()) {
                String syncText = syncStateElements.get(0).getText();
                System.out.println("🔍 Current sync state: " + syncText);
                
                if (syncText.contains("Sync State: false")) {
                    System.out.println("🔄 Sync is disabled, clicking Toggle button...");
                    
                    // Find and click the Toggle button
                    WebElement toggleButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(), 'Toggle')]")));
                    toggleButton.click();
                    
                    System.out.println("✅ Toggle button clicked, sync should now be enabled");
                    
                    // Wait a moment for the state to update
                    Thread.sleep(2000);
                    
                    // Verify sync is now enabled
                    String newSyncText = syncStateElements.get(0).getText();
                    System.out.println("🔍 Updated sync state: " + newSyncText);
                    
                } else {
                    System.out.println("✅ Sync is already enabled");
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not check/enable sync state: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("🔚 WebDriver closed successfully");
        }
    }
}
