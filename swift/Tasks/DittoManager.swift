import DittoSwift
import Foundation

/// Owner of the Ditto object
@MainActor class DittoManager: ObservableObject {
    @Published var tasks = [TaskModel]()
    @Published var selectedTaskModel: TaskModel?

    var subscription: DittoSyncSubscription?
    var storeObserver: DittoStoreObserver?

    var ditto: Ditto?
    static var shared = DittoManager()

    init() {}

    /// Performs cleanup of Ditto resources
    ///
    /// This method handles the graceful shutdown of Ditto components by:
    /// - Cancelling any active subscriptions
    /// - Cancelling store observers
    /// - Stopping the Ditto sync process
    func deinitialize() {
        subscription?.cancel()
        subscription = nil

        storeObserver?.cancel()
        storeObserver = nil

        if let dittoInstance = ditto {
            if dittoInstance.isSyncActive {
                dittoInstance.stopSync()
            }
            ditto = nil
        }
    }

    /// Initializes the Ditto instance and configures its environment.
    ///
    /// - Throws: An error if Ditto configuration or DQL commands fail
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
    /// - SeeAlso: https://docs.ditto.live/dql/strict-mode
    func initialize() async throws {
        //setup logging level
        let isPreview: Bool =
            ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"]
            == "1"
        if !isPreview {
            DittoLogger.minimumLogLevel = .debug
        }

        // Setup Ditto Identity
        // https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
        let ditto = Ditto(
            identity: .onlinePlayground(
                appID: Env.DITTO_APP_ID,
                token: Env.DITTO_PLAYGROUND_TOKEN,
                // This is required to be set to false to use the correct URLs
                // This only disables cloud sync when the webSocketURL is not set explicitly
                enableDittoCloudSync: false,
                customAuthURL: URL(string: Env.DITTO_AUTH_URL)
            )
        )

        self.ditto = ditto

        // Set the Ditto Websocket URL
        ditto.updateTransportConfig { transportConfig in
            transportConfig.connect.webSocketURLs.insert(
                Env.DITTO_WEBSOCKET_URL
            )
        }

        // disable sync with v3 peers, required for DQL
        try ditto.disableSyncWithV3()

        // Disable DQL strict mode
        // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
        // https://docs.ditto.live/dql/strict-mode
        try await ditto.store.execute(
            query: "ALTER SYSTEM SET DQL_STRICT_MODE = false"
        )

        //setup the collection with initial data and then setup observer
        try await self.populateTaskCollection()
        try self.registerObservers()

    }

    /// Populates the Ditto tasks collection with initial seed data if it's empty
    ///
    /// This method creates a set of predefined tasks in the Ditto store by:
    /// - Defining an array of initial `TaskModel` objects with unique IDs and titles
    /// - Inserting each task into the Ditto store using DQL (Ditto Query Language)
    /// - Using the INITIAL keyword to only insert if documents don't already exist
    ///
    /// The initial tasks include common todo items like:
    /// - Buy groceries
    /// - Clean the kitchen
    /// - Schedule dentist appointment
    /// - Pay bills
    ///
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/read#using-args-to-query-dynamic-values///
    ///
    /// - Throws: A DittoError if the insert operations fail
    func populateTaskCollection() async throws {

        let initialTasks: [TaskModel] = [
            TaskModel(
                _id: "50191411-4C46-4940-8B72-5F8017A04FA811",
                title: "Buy groceries"
            ),
            TaskModel(
                _id: "6DA283DA-8CFE-4526-A6FA-D385089364E811",
                title: "Clean the kitchen"
            ),
            TaskModel(
                _id: "5303DDF8-0E72-4FEB-9E82-4B007E5797F911",
                title: "Schedule dentist appointment"
            ),
            TaskModel(
                _id: "38411F1B-6B49-4346-90C3-0B16CE97E17911",
                title: "Pay bills"
            ),
        ]

        for task in initialTasks {
            if let dittoInstance = ditto {
                // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                try await dittoInstance.store.execute(
                    query: "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                    arguments: [
                        "task":
                            [
                                "_id": task._id,
                                "title": task.title,
                                "done": task.done,
                                "deleted": task.deleted,
                            ]
                    ]
                )
            }
        }
    }

    /// Registers observers for the planets collection to handle real-time updates.
    ///
    /// This method sets up a live query observer that:
    /// - Monitors the tasks collection for changes
    /// - Updates the @Published tasks array when changes occur
    /// - Filters out tasks NOT deleted
    ///
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/read
    ///
    /// - Throws: A DittoError if the observer cannot be registered
    func registerObservers() throws {
        if let dittoInstance = ditto {

            let observerQuery = "SELECT * FROM tasks WHERE NOT deleted"
            storeObserver = try dittoInstance.store.registerObserver(
                query: observerQuery
            ) {
                [weak self] results in
                Task { @MainActor in
                    // Create new TaskModel instances and update the published property
                    self?.tasks = results.items.compactMap {
                        TaskModel($0.jsonString())
                    }
                }
            }
        }
    }

    /// Enables or disables Ditto sync based on the provided value.
    ///
    /// This method checks the current sync state and starts or stops
    /// Ditto sync accordingly:
    /// - If `newValue` is `true` and sync is not active, it starts sync.
    /// - If `newValue` is `false` and sync is active, it stops sync.
    ///
    /// - Parameter newValue: A Boolean indicating whether sync should be enabled (`true`) or disabled (`false`).
    /// - Throws: A DittoError if the operation fails
    func setSyncEnabled(_ newValue: Bool) throws {
        if let dittoInstance = ditto {
            if !dittoInstance.isSyncActive && newValue {
                try startSync()
            } else if dittoInstance.isSyncActive && !newValue {
                stopSync()
            }
        }
    }

    /// Starts Ditto sync and registers a subscription for the tasks collection.
    ///
    /// This method:
    /// - Initiates the Ditto sync process if the Ditto instance is available.
    /// - Registers a subscription to keep the local tasks collection in sync with remote changes.
    ///
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    /// - Throws: A DittoError if the operation fails
    private func startSync() throws {
        if let dittoInstance = ditto {

            // https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
            try dittoInstance.startSync()

            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            let subscriptionQuery = "SELECT * from tasks"
            subscription = try dittoInstance.sync.registerSubscription(
                query: subscriptionQuery
            )
        }
    }

    /// Stops Ditto sync and cancels the active subscription for the tasks collection.
    ///
    /// This method:
    /// - Cancels the current sync subscription to stop receiving updates from remote peers.
    /// - Sets the subscription property to `nil`.
    /// - Stops the Ditto sync process if it is active.
    ///
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/sync/syncing-data#canceling-subscriptions
    /// - Throws: A DittoError if the operation fails
    private func stopSync() {
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#canceling-subscriptions
        subscription?.cancel()
        subscription = nil

        ditto?.stopSync()
    }

    /// Creates a new TaskModel document in the Ditto store.
    ///
    /// This method:
    /// - Creates a new document in the tasks collection
    ///
    /// - Parameter task: The TaskModel to add to the store
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/create#creating-documents
    ///
    /// - Throws: A DittoError if the insert operation fails
    func insertTaskModel(_ task: TaskModel) async throws {
        let newTask = task.value

        // https://docs.ditto.live/dql/insert
        // https://docs.ditto.live/sdk/latest/crud/create#creating-documents
        let query = "INSERT INTO tasks DOCUMENTS (:newTask)"

        if let dittoInstance = ditto {

            // https://docs.ditto.live/sdk/latest/crud/create#creating-documents
            try await dittoInstance.store.execute(
                query: query,
                arguments: ["newTask": newTask]
            )
        }
    }

    /// Updates an existing TaskModel's properties in the Ditto store.
    ///
    /// This method uses DQL to update all mutable fields of the TaskModel
    ///
    /// - Parameter planet: The TaskModel object containing the updated values
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/update#updating
    ///
    /// - Throws: A DittoError if the update operation fails
    func updateTaskModel(_ task: TaskModel) async throws {
        // https://docs.ditto.live/dql/update#basic-update
        // https://docs.ditto.live/sdk/latest/crud/update#updating
        let query = """
            UPDATE tasks SET
            title = :title,
            done = :done,
            deleted = :deleted
            WHERE _id == :_id
            """

        if let dittoInstance = ditto {

            // https://docs.ditto.live/sdk/latest/crud/update#updating
            try await dittoInstance.store.execute(
                query: query,
                arguments: [
                    "title": task.title,
                    "done": task.done,
                    "deleted": task.deleted,
                    "_id": task._id,
                ]
            )
        }
    }

    /// Toggles the completion status of a task in the Ditto store
    ///
    /// This method:
    /// - Inverts the current 'done' status of the task
    /// - Updates only the 'done' field in the store using DQL
    /// - Maintains all other task properties unchanged
    ///
    /// - Parameter task: The TaskModel to toggle completion status for
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/update#updating
    ///
    /// - Throws: A DittoError if the update operation fails
    func toggleComplete(task: TaskModel) async throws {
        let done = !task.done

        // https://docs.ditto.live/dql/update#basic-update
        // https://docs.ditto.live/sdk/latest/crud/update#updating
        let query = """
            UPDATE tasks
            SET done = :done 
            WHERE _id == :_id
            """

        if let dittoInstance = ditto {

            // https://docs.ditto.live/sdk/latest/crud/update#updating
            try await dittoInstance.store.execute(
                query: query,
                arguments: ["done": done, "_id": task._id]
            )
        }
    }

    /// Delete a TaskModel by setting its deleted flag to true.
    ///
    /// This method implements the 'Soft-Delete' pattern, which:
    /// - Marks the TaskModel as deleted instead of evicting it
    /// - Removes it from active queries and views
    /// - Maintains the data for historical purposes
    ///
    /// - Parameter task: The TaskModel to soft-delet
    /// - SeeAlso: https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
    ///
    /// - Throws: A DittoError if the archive operation fails
    func deleteTaskModel(_ task: TaskModel) async throws {

        // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
        let query = "UPDATE tasks SET deleted = true WHERE _id = :_id"
        if let dittoInstance = ditto {

            // https://docs.ditto.live/sdk/latest/crud/update#updating
            try await dittoInstance.store.execute(
                query: query,
                arguments: ["_id": task._id]
            )
        }
    }
}
