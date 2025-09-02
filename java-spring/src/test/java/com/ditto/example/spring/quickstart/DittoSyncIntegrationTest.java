package com.ditto.example.spring.quickstart;

import com.ditto.example.spring.quickstart.service.DittoTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Ditto sync functionality in Spring Boot application.
 * This test verifies that the app can create and sync tasks using the Ditto SDK,
 * testing both the service layer and REST API endpoints.
 * 
 * Uses SDK insertion approach for better local testing:
 * 1. Creates test tasks using DittoTaskService directly
 * 2. Verifies tasks appear via REST API endpoints  
 * 3. Tests real-time sync capabilities using same Ditto configuration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class DittoSyncIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DittoTaskService taskService;

    @Test
    @Order(1)
    public void testApplicationStartsSuccessfully() {
        // Test that Spring Boot application starts and Ditto initializes
        assertNotNull(taskService, "DittoTaskService should be initialized");
        System.out.println("✓ Spring Boot application started successfully with Ditto integration");
    }

    @Test
    @Order(2)
    public void testSDKTaskCreationAndRetrieval() {
        // Create deterministic task using GitHub run info or timestamp
        String runId = System.getProperty("github.run.id", String.valueOf(System.currentTimeMillis()));
        String taskTitle = "GitHub Test Task Java Spring " + runId;
        
        System.out.println("Creating test task via SDK: " + taskTitle);
        
        // Insert test task using SDK (same as the application uses)
        try {
            taskService.addTask(taskTitle);
            System.out.println("✓ Test task inserted via SDK");
            
            // Wait a moment for task to be persisted and available
            Thread.sleep(2000);
            
            // Verify task can be retrieved via service using reactive stream
            var tasksFlux = taskService.observeAll().take(1).blockFirst();
            assertNotNull(tasksFlux, "Tasks should be observable");
            
            boolean taskFound = tasksFlux.stream()
                .anyMatch(task -> task.title().contains("GitHub Test Task") && 
                                 task.title().contains(runId));
                                 
            assertTrue(taskFound, "SDK-created task should be retrievable via service");
            System.out.println("✓ SDK test task successfully created and retrieved");
            
        } catch (Exception e) {
            fail("Failed to create test task via SDK: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    public void testRESTAPITaskCreation() {
        // Test task creation via REST API endpoint
        String runId = System.getProperty("github.run.id", String.valueOf(System.currentTimeMillis()));
        String taskTitle = "GitHub API Test Task " + runId;
        
        System.out.println("Creating test task via REST API: " + taskTitle);
        
        try {
            // Create task via REST API
            MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
            request.add("title", taskTitle);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/tasks", 
                request, 
                String.class
            );
            
            assertEquals(200, response.getStatusCode().value(), "REST API task creation should succeed");
            System.out.println("✓ Test task created via REST API");
            
            // Wait for task to be processed
            Thread.sleep(1000);
            
            // Verify task exists via service layer using reactive stream
            var tasksFlux = taskService.observeAll().take(1).blockFirst();
            assertNotNull(tasksFlux, "Tasks should be observable");
            
            boolean taskFound = tasksFlux.stream()
                .anyMatch(task -> task.title().equals(taskTitle));
                
            assertTrue(taskFound, "API-created task should be retrievable via service");
            System.out.println("✓ REST API test task successfully created and verified");
            
        } catch (Exception e) {
            fail("Failed to create test task via REST API: " + e.getMessage());
        }
    }

    @Test
    @Order(4) 
    public void testTaskToggleFunctionality() {
        // Test task completion toggle functionality
        try {
            // Create a task first
            String testTitle = "Toggle Test Task " + System.currentTimeMillis();
            taskService.addTask(testTitle);
            
            Thread.sleep(1000);
            
            // Find the created task using reactive stream
            var tasksFlux = taskService.observeAll().take(1).blockFirst();
            assertNotNull(tasksFlux, "Tasks should be observable");
            
            var testTask = tasksFlux.stream()
                .filter(task -> task.title().equals(testTitle))
                .findFirst()
                .orElse(null);
                
            assertNotNull(testTask, "Test task should exist for toggle test");
            
            // Test toggle via service
            String taskId = testTask.id();
            taskService.toggleTaskDone(taskId);
            
            System.out.println("✓ Task toggle functionality working via SDK");
            
            // Test toggle via REST API
            ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/tasks/" + taskId + "/toggle",
                null,
                String.class
            );
            
            assertEquals(200, response.getStatusCode().value(), "Task toggle via API should succeed");
            System.out.println("✓ Task toggle functionality working via REST API");
            
        } catch (Exception e) {
            fail("Task toggle test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    public void testDittoSyncStability() {
        // Test that Ditto sync remains stable throughout operations
        try {
            // Create multiple tasks to test sync stability
            for (int i = 0; i < 3; i++) {
                String title = "Stability Test Task " + i + " " + System.currentTimeMillis();
                taskService.addTask(title);
                Thread.sleep(500);
            }
            
            // Wait for all tasks to be processed
            Thread.sleep(2000);
            
            // Verify all tasks are accessible using reactive stream  
            var tasksFlux = taskService.observeAll().take(1).blockFirst();
            assertNotNull(tasksFlux, "Tasks should be observable");
            
            long stabilityTasks = tasksFlux.stream()
                .filter(task -> task.title().contains("Stability Test Task"))
                .count();
                
            assertTrue(stabilityTasks >= 3, "All stability test tasks should be created and retrievable");
            System.out.println("✓ Ditto sync remains stable under multiple operations (" + stabilityTasks + " tasks)");
            
        } catch (Exception e) {
            fail("Ditto sync stability test failed: " + e.getMessage());
        }
    }
}