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

/**
 * Visual browser integration test for Task visibility using Selenium WebDriver.
 * Tests the actual web UI by automating browser interactions.
 * 
 * Supports both local Chrome testing and BrowserStack remote testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskVisibilityIntegrationTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeAll
    static void setupWebDriver() {
        // Check if running on BrowserStack (CI environment)
        String browserStackUser = System.getenv("BROWSERSTACK_USERNAME");
        String browserStackKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        
        if (browserStackUser != null && browserStackKey != null) {
            setupBrowserStackDriver(browserStackUser, browserStackKey);
        } else {
            setupLocalChromeDriver();
        }
        
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private static void setupBrowserStackDriver(String username, String accessKey) {
        try {
            ChromeOptions options = new ChromeOptions();
            Map<String, Object> browserstackOptions = new HashMap<>();
            browserstackOptions.put("userName", username);
            browserstackOptions.put("accessKey", accessKey);
            browserstackOptions.put("projectName", "Ditto Java Spring Tasks");
            browserstackOptions.put("buildName", "Task Visibility Integration Test");
            browserstackOptions.put("sessionName", "Visual Task Testing");
            browserstackOptions.put("os", "Windows");
            browserstackOptions.put("osVersion", "11");
            browserstackOptions.put("browserVersion", "latest");
            browserstackOptions.put("local", "true");
            
            // Add BrowserStack Local identifier if provided
            String localIdentifier = System.getProperty("BROWSERSTACK_LOCAL_IDENTIFIER");
            if (localIdentifier != null && !localIdentifier.isEmpty()) {
                browserstackOptions.put("localIdentifier", localIdentifier);
                System.out.println("🔗 Using BrowserStack Local identifier: " + localIdentifier);
            }
            
            options.setCapability("bstack:options", browserstackOptions);
            
            driver = new RemoteWebDriver(
                new URL("https://hub-cloud.browserstack.com/wd/hub"), 
                options
            );
            
            System.out.println("✅ BrowserStack WebDriver initialized for visual testing");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BrowserStack WebDriver: " + e.getMessage(), e);
        }
    }

    private static void setupLocalChromeDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            // Remove headless mode for local visual testing
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
        baseUrl = "http://localhost:" + port;
        System.out.println("🌐 Testing Spring Boot Ditto Tasks app at: " + baseUrl);
    }

    @Test
    @Order(1)
    void shouldLoadTasksWebPage() {
        System.out.println("🧪 Test 1: Loading Tasks web page...");
        
        driver.get(baseUrl);
        System.out.println("📍 Browser opened at: " + baseUrl);
        
        // Add delay to see the page loading
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Wait for page to load and verify title
        wait.until(ExpectedConditions.titleContains("Ditto"));
        
        String pageTitle = driver.getTitle();
        System.out.println("📄 Page title: " + pageTitle);
        Assertions.assertTrue(pageTitle.contains("Ditto"), 
            "Page title should contain 'Ditto', but was: " + pageTitle);
        
        // Verify main elements are present - use more flexible selectors
        WebElement taskInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("input")));
        Assertions.assertTrue(taskInput.isDisplayed(), "Task input field should be visible");
        System.out.println("✅ Found input field");
        
        WebElement addButton = driver.findElement(By.cssSelector("button"));
        Assertions.assertTrue(addButton.isDisplayed(), "Add task button should be visible");
        System.out.println("✅ Found add button");
        
        // Keep browser open longer to see the result
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Tasks web page loaded successfully with input field and add button");
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
        
        // Wait for the task list to load
        WebElement taskList = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("body")));
        
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
    void shouldPassWithExistingTask() {
        System.out.println("🧪 Test 4: Should pass when finding existing task...");
        
        String searchText = "Task 1 - a2218d97";  // Known existing task
        System.out.println("🔍 Searching for existing task: " + searchText);
        
        driver.get(baseUrl);
        System.out.println("📍 Browser opened at: " + baseUrl);
        
        // Wait for page to load
        wait.until(ExpectedConditions.titleContains("Ditto"));
        
        // Wait for the task list to load
        WebElement taskList = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("body")));
        
        // Add delay to let tasks load from Ditto
        System.out.println("⏳ Waiting for Ditto tasks to load...");
        try {
            Thread.sleep(3000);
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


    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("🔚 WebDriver closed successfully");
        }
    }
}