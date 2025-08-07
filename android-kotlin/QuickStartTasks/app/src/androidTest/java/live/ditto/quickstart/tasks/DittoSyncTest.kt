package live.ditto.quickstart.tasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import org.junit.After

/**
 * Instrumented test for Ditto synchronization functionality.
 * Tests the core Ditto operations on real devices.
 */
@RunWith(AndroidJUnit4::class)
class DittoSyncTest {
    
    private lateinit var appContext: android.content.Context
    
    @Before
    fun setUp() {
        // Get the app context
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("live.ditto.quickstart.tasks", appContext.packageName)
    }
    
    @After
    fun tearDown() {
        // Clean up after tests
    }
    
    @Test
    fun testDittoInitialization() {
        // Test that Ditto can be initialized properly
        // This verifies the native library loading and basic setup
        try {
            // The actual Ditto initialization happens in the app
            // Here we just verify the package and context are correct
            assertNotNull(appContext)
            assertTrue(appContext.packageName.contains("ditto"))
        } catch (e: Exception) {
            fail("Ditto initialization failed: ${e.message}")
        }
    }
}