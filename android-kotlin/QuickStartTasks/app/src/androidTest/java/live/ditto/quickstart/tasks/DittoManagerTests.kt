package live.ditto.quickstart.tasks

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.quickstart.tasks.data.CustomDirectoryAndroidDittoDependencies
import live.ditto.quickstart.tasks.data.DittoManager
import live.ditto.quickstart.tasks.models.DittoConfig
import live.ditto.quickstart.tasks.data.TaskModel
import live.ditto.quickstart.tasks.services.ErrorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DittoManagerTests {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("live.ditto.quickstart.tasks", appContext.packageName)
    }

    @Test
    fun testDittoPopulateTaskCollection() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val expectedTitles = listOf(
                "Buy groceries",
                "Clean the kitchen",
                "Schedule dentist appointment",
                "Pay bills"
            )

            //act
            dittoManager.populateTaskCollection()
            advanceUntilIdle()
            val results = dittoManager.ditto?.store?.execute("SELECT * FROM tasks WHERE NOT deleted")
            val tasks = results?.items?.map { TaskModel.fromMap(it.value) } ?: emptyList()


            //assert
            assertEquals(expectedTitles.size, tasks.size)
            assertEquals(
                expectedTitles.sorted(),
                tasks.map { it.title }.sorted()
            )
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    @Test
    fun testDittoInsertTaskModel() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val newTask = createInitialTaskModel()

            //act
            dittoManager.insertTaskModel(newTask)
            advanceUntilIdle()

            //assert
            val tasks = dittoManager.getTaskModels().first()
            val newCount = tasks.size
            val insertedTask = tasks.find { it._id == newTask._id }
            assertEquals(1, newCount)
            assertEquals(newTask, insertedTask)
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    @Test
    fun testDittoUpdateTaskModel() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val newTask = createInitialTaskModel()
            dittoManager.insertTaskModel(newTask)
            advanceUntilIdle()

            //act
            val updatedTask = newTask.copy(title = "Updated Task", done = true)
            dittoManager.updateTaskModel(updatedTask)
            advanceUntilIdle()

            //assert
            val tasks = dittoManager.getTaskModels().first()
            val newCount = tasks.size
            val insertedTask = tasks.find { it._id == updatedTask._id }
            assertEquals(1, newCount)
            assertNotNull(insertedTask)
            assertEquals(updatedTask, insertedTask)
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    @Test
    fun testDittoUpdateTaskModel_WhenDeleted() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val newTask = createInitialTaskModel()
            dittoManager.insertTaskModel(newTask)
            advanceUntilIdle()

            //act
            val updatedTask = newTask.copy(deleted = true)
            dittoManager.updateTaskModel(updatedTask)
            advanceUntilIdle()

            //assert
            val tasks = dittoManager.getTaskModels().first()
            val newCount = tasks.size
            assertEquals(0, newCount)
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    @Test
    fun testDittoToggleComplete() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val newTask = createInitialTaskModel()
            dittoManager.insertTaskModel(newTask)
            advanceUntilIdle()

            //act
            dittoManager.toggleComplete(newTask._id)
            advanceUntilIdle()

            //assert
            val tasks = dittoManager.getTaskModels().first()
            val newCount = tasks.size
            assertEquals(1, newCount)
            val insertedTask = tasks.find { it._id == newTask._id }
            assertNotNull(insertedTask)
            if (insertedTask != null) {
                assertTrue(insertedTask.done)
            }
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    @Test
    fun testDittoDeleteTaskModel() = runTest {
        val dittoManager = createDittoManagerForTests()
        try {
            //arrange
            val newTask = createInitialTaskModel()
            dittoManager.insertTaskModel(newTask)
            advanceUntilIdle()

            //act
            dittoManager.deleteTaskModel(newTask._id)
            advanceUntilIdle()

            //assert
            val tasks = dittoManager.getTaskModels().first()
            val newCount = tasks.size
            assertEquals(0, newCount)
        } finally {
            cleanUpCollection(dittoManager)
        }
    }

    private fun createInitialTaskModel() : TaskModel {
        return TaskModel(
            _id = UUID.randomUUID().toString(),
            title = "Test Task",
            done = false,
            deleted = false)
    }

    private suspend fun createDittoManagerForTests() : DittoManager {
        // Create a temporary directory for testing
        val tempDir = withContext(Dispatchers.IO) {
            File.createTempFile("ditto_test_${UUID.randomUUID()}", null)
        }
        tempDir.delete()
        tempDir.mkdirs()

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val dittoConfig = DittoConfig(
            authUrl = BuildConfig.DITTO_AUTH_URL,
            websocketUrl = "",
            appId = BuildConfig.DITTO_APP_ID,
            authToken = BuildConfig.DITTO_PLAYGROUND_TOKEN
        )

        val customDependencies = CustomDirectoryAndroidDittoDependencies(
            DefaultAndroidDittoDependencies(appContext),
            tempDir
        )

        val dittoManager = DittoManager(
            dittoConfig = dittoConfig,
            androidDittoDependencies = customDependencies,
            errorService = createErrorService()
        )
        dittoManager.ditto?.let { ditto ->
            // disable sync with v3 peers, required for DQL to work
            ditto.disableSyncWithV3()

            // Disable DQL strict mode
            // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
            // https://docs.ditto.live/dql/strict-mode
            ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")

            ditto.store.execute(
                "ALTER SYSTEM SET USER_COLLECTION_SYNC_SCOPES = :syncScopes",
                mapOf(
                    "syncScopes" to mapOf(
                        "tasks" to "LocalPeerOnly"
                    )
                )
            )
        }
        return dittoManager
    }

    private fun createErrorService() : ErrorService {
        return ErrorService()
    }

    private suspend fun cleanUpCollection(dittoManager: DittoManager) {
        try {
            dittoManager.stopSync()
            dittoManager.ditto?.store?.execute("EVICT FROM tasks")

            //remove the directory and all files
            val directoryPath = dittoManager.ditto?.persistenceDirectory
            directoryPath?.let { path ->
                val directory = File(path)
                if (directory.exists()) {
                    directory.listFiles()?.forEach { file ->
                        try {
                            file.delete()
                        } catch (e: Exception) {
                            Log.e("DittoManagerTests", "Failed to delete file: ${file.absolutePath}", e)
                        }
                    }
                    try {
                        directory.delete()
                    } catch (e: Exception) {
                        Log.e("DittoManagerTests", "Failed to delete directory: ${directory.absolutePath}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DittoManagerTests", "Failed to clean up collection", e)
        } finally {
            dittoManager.ditto = null
        }
    }

}