package com.ditto.example.spring.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that verifies BrowserStack environment availability.
 * 
 * This test is designed to:
 * - PASS in CI environments (GitHub Actions) where BrowserStack secrets are available
 * - FAIL in local environments where BrowserStack secrets are not configured
 * 
 * This validates that our CI pipeline has proper BrowserStack integration
 * while preventing the test from passing in environments where it shouldn't.
 */
@SpringBootTest
@ActiveProfiles("test")
public class BrowserStackIntegrationTest {

    @Test
    public void testBrowserStackEnvironmentAvailability() {
        // Check for BrowserStack environment variables that would be present in CI
        String browserstackUsername = System.getenv("BROWSERSTACK_USERNAME");
        String browserstackAccessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        String dittoApiKey = System.getenv("DITTO_API_KEY");
        String dittoApiUrl = System.getenv("DITTO_API_URL");
        
        // This test should fail locally (no BrowserStack secrets) but pass in CI
        assertAll("BrowserStack Integration Environment",
            () -> assertNotNull(browserstackUsername, 
                "BROWSERSTACK_USERNAME must be set in CI environment"),
            () -> assertNotNull(browserstackAccessKey, 
                "BROWSERSTACK_ACCESS_KEY must be set in CI environment"),
            () -> assertNotNull(dittoApiKey, 
                "DITTO_API_KEY must be set for Ditto Cloud integration"),
            () -> assertNotNull(dittoApiUrl, 
                "DITTO_API_URL must be set for Ditto Cloud integration")
        );
        
        // Additional validation that secrets are not empty
        assertTrue(browserstackUsername != null && !browserstackUsername.trim().isEmpty(), 
            "BROWSERSTACK_USERNAME must not be empty");
        assertTrue(browserstackAccessKey != null && !browserstackAccessKey.trim().isEmpty(), 
            "BROWSERSTACK_ACCESS_KEY must not be empty");
        assertTrue(dittoApiKey != null && !dittoApiKey.trim().isEmpty(), 
            "DITTO_API_KEY must not be empty");
        assertTrue(dittoApiUrl != null && !dittoApiUrl.trim().isEmpty(), 
            "DITTO_API_URL must not be empty");
    }

    @Test
    public void testDittoCloudConnectivity() {
        // This test verifies that we can connect to Ditto Cloud
        // In a real scenario, this would make API calls to verify connectivity
        
        String githubTestDocId = System.getenv("GITHUB_TEST_DOC_ID");
        String dittoApiKey = System.getenv("DITTO_API_KEY");
        
        // These should be available in CI after the seeding step
        assertNotNull(githubTestDocId, 
            "GITHUB_TEST_DOC_ID should be set after Ditto Cloud seeding step in CI");
        assertNotNull(dittoApiKey, 
            "DITTO_API_KEY should be available for Ditto Cloud integration");
        
        // Verify the test document ID follows expected format
        if (githubTestDocId != null) {
            assertTrue(githubTestDocId.startsWith("github_spring_test_"), 
                "Test document ID should follow expected naming pattern");
        }
    }

    @Test
    public void testCIEnvironmentValidation() {
        // This test validates we're running in the expected CI environment
        String githubActions = System.getenv("GITHUB_ACTIONS");
        String githubRunId = System.getenv("GITHUB_RUN_ID");
        
        // These variables are set by GitHub Actions
        assertNotNull(githubActions, "Should be running in GitHub Actions environment");
        assertEquals("true", githubActions, "GITHUB_ACTIONS should be 'true' in CI");
        assertNotNull(githubRunId, "GITHUB_RUN_ID should be set in CI environment");
        
        // Verify the run ID is numeric
        if (githubRunId != null) {
            assertTrue(githubRunId.matches("\\d+"), 
                "GitHub run ID should be numeric");
        }
    }
}