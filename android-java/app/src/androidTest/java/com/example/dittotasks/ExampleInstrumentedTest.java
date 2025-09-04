package com.example.dittotasks;

import android.util.Log;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import android.content.Intent;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
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
        
        // Fallback to a default test document for local testing
        if (title == null || title.trim().isEmpty()) {
            title = "Basic Test Task"; // Default fallback for local testing
            Log.i("DittoTest", "Using default test document: " + title);
        }

        Log.i("DittoTest", "Testing with document title: " + title);

        // Launch activity manually with proper error handling
        Log.i("DittoTest", "Launching MainActivity...");
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
        
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            Log.i("DittoTest", "Activity launched successfully");
            
            // Wait for Ditto to initialize and sync data (takes ~5 seconds)
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
        
        // Scroll to the cell containing the specific title (document should be seeded from GHA)
        Log.i("DittoTest", "Scrolling to find pre-seeded task: " + title);
        onView(withId(R.id.task_list))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(allOf(withId(R.id.task_text), withText(title)))
                ));
        
        Log.i("DittoTest", "✅ Found and scrolled to pre-seeded task: " + title);
        
        // Final assertion to confirm it's displayed
        onView(allOf(withId(R.id.task_text), withText(title)))
                .check(ViewAssertions.matches(isDisplayed()));
        
        // Keep screen visible for 3 seconds for BrowserStack video verification
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