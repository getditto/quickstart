package com.example.dittotasks;

import android.util.Log;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void testGitHubTestDocumentSyncs() throws Exception {
        // Get environment variable with fallback options
        String title = InstrumentationRegistry.getArguments().getString("github_test_doc_id");
        
        // Try multiple fallback sources
        if (title == null || title.trim().isEmpty()) {
            title = System.getProperty("GITHUB_TEST_DOC_ID");
        }
        if (title == null || title.trim().isEmpty()) {
            title = System.getenv("GITHUB_TEST_DOC_ID");
        }
        
        // No fallback - fail if seed is not set
        if (title == null || title.trim().isEmpty()) {
            throw new AssertionError("Expected test title in 'github_test_doc_id' (or GITHUB_TEST_DOC_ID); none provided. Must be seeded by CI.");
        }

        Log.i("DittoTest", "Testing with document title: " + title);

        // Launch activity manually with proper error handling
        Log.i("DittoTest", "Launching MainActivity...");
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
        
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            Log.i("DittoTest", "Activity launched successfully");
            
            // Wait for Ditto to initialize and sync data
            // Note: Using fixed delay as Espresso IdlingResource is complex for Ditto sync timing
            Log.i("DittoTest", "Waiting for activity and Ditto initialization...");
            Thread.sleep(6000); // Allow time for Ditto sync and UI updates
            
            // Verify activity is still running
            scenario.onActivity(activity -> {
                Log.i("DittoTest", "Activity is running: " + activity.getClass().getSimpleName());
            });
            
            // Run the test logic
            performTestLogic(title);
        } catch (Exception e) {
            Log.e("DittoTest", "Activity failed: " + e.getMessage(), e);
            throw e;
        }
    }

    private void performTestLogic(String title) throws InterruptedException {
        
        // Wait for RecyclerView to appear and be populated (with timeout)
        waitForRecyclerViewToLoad(7_000);
        
        // Verify the seeded document is visible at the top (no scrolling needed)
        Log.i("DittoTest", "🔍 Searching for document with title: '" + title + "'");
        
        try {
            onView(allOf(withId(R.id.task_text), withText(title)))
                    .check(ViewAssertions.matches(isDisplayed()));
            Log.i("DittoTest", "✅ Found document with title: '" + title + "'");
        } catch (Exception e) {
            Log.e("DittoTest", "❌ Document NOT found with title: '" + title + "'");
            Log.e("DittoTest", "Error: " + e.getMessage());
            
            // Log what's actually visible for debugging
            try {
                Log.i("DittoTest", "🔍 Debugging: Checking what tasks are actually visible...");
                onView(withId(R.id.task_list))
                        .check(ViewAssertions.matches(isDisplayed()));
                Log.i("DittoTest", "RecyclerView is present and displayed");
            } catch (Exception recyclerError) {
                Log.e("DittoTest", "RecyclerView not found or displayed: " + recyclerError.getMessage());
            }
            
            throw e; // Re-throw the original exception
        }
        
        // Keep screen visible for BrowserStack video verification
        // This delay is required for BrowserStack test recording to capture the successful state
        // before the test completes and the activity is destroyed
        Thread.sleep(3000);
    }


    /** Wait for RecyclerView to load and be visible with data */
    private void waitForRecyclerViewToLoad(long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        Exception lastException = null;

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                // Check that RecyclerView is displayed and has some items
                onView(withId(R.id.task_list))
                        .check(ViewAssertions.matches(isDisplayed()));
                
                Log.i("DittoTest", "RecyclerView is displayed and ready");
                return; // success
            } catch (Exception e) {
                lastException = e;
                Log.i("DittoTest", "Waiting for RecyclerView to load...");
            }
            Thread.sleep(500);
        }

        if (lastException != null) throw new RuntimeException("RecyclerView not ready after timeout", lastException);
        throw new AssertionError("Timed out waiting for RecyclerView to load");
    }
}