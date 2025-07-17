package live.ditto.quickstart.tasks.data

import kotlinx.coroutines.flow.Flow
import live.ditto.quickstart.tasks.models.DittoConfig

interface DataManager {
    val dittoConfig: DittoConfig

    /**
     * Populates the Ditto tasks collection with initial seed data if it's empty.
     *
     * This method:
     * - Creates a set of predefined tasks with unique IDs and titles
     *
     * The initial tasks include common todo items like:
     * - Buy groceries
     * - Clean the kitchen
     * - Schedule dentist appointment
     * - Pay bills
     *
     * @throws Exception if the insert operations fail
     *
     */
    suspend fun populateTaskCollection()

    /**
     * Enables or disables synchronization
     *
     * This method:
     * - Persists the sync preference in DataStore
     * - Updates the UI state through LiveData
     *
     * Implementation details:
     * - Retrieves default sync state from preferences if not specified
     * - Updates both DataStore and LiveData to maintain state
     *
     * @param enabled Boolean value to set sync state to. If null, uses stored preference
     *
     */
    suspend fun setSyncEnabled(enabled: Boolean)

    /**
     * Creates a Flow that observes and emits changes to the tasks collection
     *
     * This method:
     * - Sets up a live query observer for the tasks collection
     * - Emits a new list of TaskModel objects whenever the data changes
     *
     * @return Flow<List<TaskModel>> A flow that emits updated lists of Tasks
     * whenever changes occur in the collection
     */
    fun getTaskModels(): Flow<List<TaskModel>>

    /**
     * Adds a new TaskModel
     *
     * This method:
     * - Creates a new document in the tasks collection
     * - Assigns the provided ID and properties
     * - Triggers a sync with other devices
     *
     * The taskModel object should have:
     * - A unique ID
     * - All required fields populated
     *
     * @param taskModel The new TaskModel object to be added
     * @throws Exception if the insert operation fails
     */
    suspend fun insertTaskModel(taskModel: TaskModel)

    /**
     * Updates an existing TaskModel
     *
     * This method:
     * - Updates all mutable fields of the TaskModel
     * - Maintains the taskModel's ID and references
     * - Triggers a sync with other devices
     *
     * @param taskModel The updated TaskModel object containing all changes
     * @throws Exception if the update operation fails
     */
    suspend fun updateTaskModel(taskModel: TaskModel)

    /**
     * Updates an existing TaskModel, setting the done field to true.
     *
     * This method:
     * - Updates the done field to true
     * - Maintains the taskModel's ID and references
     *
     * The id should be a valid _id of a TaskModel object
     *
     * @param id  The _id of the TaskModel object to update
     * @throws Exception if the insert operation fails
     */
    suspend fun toggleComplete(id: String)

    /**
     * Archives a TaskModel by setting its deleted flag to true.
     *
     * This method:
     * - Marks the taskModel as deleted instead of deleting it
     * - Removes it from active queries and views
     * - Maintains the data for historical purposes
     *
     * Archived TaskModel:
     * - Are excluded from the main TaskModel list
     *
     * @param id  The _id of the TaskModel object to archive
     * @throws Exception if the archive operation fails
     */
    suspend fun deleteTaskModel(id: String)
}