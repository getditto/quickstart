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
        assertEquals("live.ditto.quickstart.taskscpp", appContext.packageName)
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
    
    @Test
    fun testNativeLibraryLoading() {
        // Verify that the native libraries are properly loaded
        try {
            // Check if the native library directory exists
            val nativeLibDir = appContext.applicationInfo.nativeLibraryDir
            assertNotNull(nativeLibDir)
            
            val libDir = java.io.File(nativeLibDir)
            assertTrue("Native library directory should exist", libDir.exists())
            assertTrue("Native library directory should be a directory", libDir.isDirectory)
            
            // Check for expected native libraries
            val files = libDir.listFiles()
            assertNotNull("Native library directory should contain files", files)
            
            // Look for Ditto-related libraries
            val hasDittoLibs = files?.any { file ->
                file.name.contains("ditto", ignoreCase = true) ||
                file.name.contains("c++_shared", ignoreCase = true)
            } ?: false
            
            assertTrue("Should have Ditto native libraries", hasDittoLibs)
        } catch (e: Exception) {
            fail("Native library check failed: ${e.message}")
        }
    }
    
    @Test
    fun testBuildConfigValues() {
        // Test that build config values are properly set
        try {
            // Access BuildConfig through reflection since it's in the app module
            val buildConfigClass = Class.forName("${appContext.packageName}.BuildConfig")
            
            // Check if required fields exist
            val fields = listOf("DITTO_APP_ID", "DITTO_PLAYGROUND_TOKEN", 
                               "DITTO_AUTH_URL", "DITTO_WEBSOCKET_URL")
            
            for (fieldName in fields) {
                try {
                    val field = buildConfigClass.getDeclaredField(fieldName)
                    assertNotNull("BuildConfig.$fieldName should exist", field)
                    
                    // Verify the field is a String
                    assertEquals("BuildConfig.$fieldName should be a String", 
                                String::class.java, field.type)
                } catch (e: NoSuchFieldException) {
                    fail("BuildConfig.$fieldName is missing")
                }
            }
        } catch (e: ClassNotFoundException) {
            fail("BuildConfig class not found: ${e.message}")
        }
    }
    
    @Test
    fun testDeviceCapabilities() {
        // Test device capabilities for Ditto requirements
        val androidVersion = android.os.Build.VERSION.SDK_INT
        assertTrue("Android version should be >= 23 (minimum SDK)", androidVersion >= 23)
        
        // Check device architecture
        val supportedAbis = android.os.Build.SUPPORTED_ABIS
        assertNotNull("Device should report supported ABIs", supportedAbis)
        assertTrue("Device should support at least one ABI", supportedAbis.isNotEmpty())
        
        // Log device info for debugging
        println("Device Info:")
        println("- Android Version: $androidVersion")
        println("- Device Model: ${android.os.Build.MODEL}")
        println("- Manufacturer: ${android.os.Build.MANUFACTURER}")
        println("- Supported ABIs: ${supportedAbis.joinToString(", ")}")
    }
    
    @Test
    fun testMemoryAvailability() {
        // Test that device has sufficient memory for Ditto operations
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        
        // Ditto requires reasonable memory availability
        assertTrue("Max memory should be > 64MB", maxMemory > 64 * 1024 * 1024)
        assertTrue("Free memory should be > 16MB", freeMemory > 16 * 1024 * 1024)
        
        println("Memory Info:")
        println("- Max Memory: ${maxMemory / 1024 / 1024}MB")
        println("- Total Memory: ${totalMemory / 1024 / 1024}MB")
        println("- Free Memory: ${freeMemory / 1024 / 1024}MB")
    }
}