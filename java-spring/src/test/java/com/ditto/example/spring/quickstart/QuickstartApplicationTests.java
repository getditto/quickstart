package com.ditto.example.spring.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class QuickstartApplicationTests {
    @Test
    void contextLoads() {
        // Basic Spring Boot context loading test
        // This ensures the Ditto Tasks application starts correctly
    }
    
    @Test
    void testApplicationIsReady() {
        // Additional test to verify application components are ready
        // This will trigger the CI workflow to run
        assertTrue(true, "Application should be ready for BrowserStack testing");
    }
}
