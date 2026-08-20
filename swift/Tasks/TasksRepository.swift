import Combine
import DittoSwift
import Foundation

/// Owns all `tasks` collection data operations: the DQL query strings, the sync
/// subscription, the store observer, the published task list, and CRUD.
///
/// It calls the real Ditto API directly through `DittoManager` (e.g.
/// `DittoManager.shared.ditto?.store.execute(...)`) rather than routing through
/// wrapper methods, so the quickstart shows how to use Ditto directly instead of
/// modeling an abstraction layer over it.
@MainActor
class TasksRepository: ObservableObject {
    @Published var tasks = [TaskModel]()

    private var ditto: Ditto? { DittoManager.shared.ditto }
    private var subscription: DittoSyncSubscription?
    private var storeObserver: DittoStoreObserver?

    // The subscription controls what syncs to this device and is written to the
    // local database; the observer reacts to changes in that local database,
    // hiding soft-deleted tasks from the displayed list.
    private let subscriptionQuery = "SELECT * FROM tasks"

    private let observerQuery = "SELECT * FROM tasks WHERE NOT deleted"

    init() {
        // Register a subscription, which determines what data syncs to this peer.
        // It is registered once and stays registered; toggling sync on/off starts
        // and stops the sync engine (see DittoManager), not the subscription.
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        subscription = try? ditto?.sync.registerSubscription(query: subscriptionQuery)

        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        storeObserver = try? ditto?.store.registerObserver(query: observerQuery) { [weak self] result in
            guard let self = self else { return }
            let mappedTasks = result.items.compactMap { TaskModel($0.jsonData()) }
            Task { @MainActor [weak self] in
                self?.tasks = mappedTasks
            }
        }
    }

    /// Cancel the observer and subscription. Call from `.onDisappear`
    /// — `deinit` is `nonisolated` and cannot safely touch `@MainActor` state.
    func teardown() {
        subscription?.cancel()
        subscription = nil

        storeObserver?.cancel()
        storeObserver = nil
    }

    func toggleComplete(task: TaskModel) {
        Task { [weak self] in
            guard let self else { return }
            let done = !task.done
            let query = """
                UPDATE tasks
                SET done = :done
                WHERE _id = :id
                """

            do {
                if let ditto = self.ditto {
                    try await ditto.store.execute(
                        query: query,
                        arguments: ["done": done, "id": task._id]
                    )
                }
            } catch {
                print(
                    "TasksRepository.\(#function) - ERROR toggling task: \(error.localizedDescription)"
                )
            }
        }
    }

    func saveEditedTask(_ task: TaskModel) {
        Task { [weak self] in
            guard let self else { return }
            let query = """
                UPDATE tasks SET
                    title = :title,
                    done = :done,
                    deleted = :deleted
                WHERE _id = :id
                """

            do {
                if let ditto = self.ditto {
                    try await ditto.store.execute(
                        query: query,
                        arguments: [
                            "title": task.title,
                            "done": task.done,
                            "deleted": task.deleted,
                            "id": task._id
                        ]
                    )
                }
            } catch {
                print(
                    "TasksRepository.\(#function) - ERROR updating task: \(error.localizedDescription)"
                )
            }
        }
    }

    func saveNewTask(_ task: TaskModel) {
        Task { [weak self] in
            guard let self else { return }
            let newTask = task.value
            let query = "INSERT INTO tasks DOCUMENTS (:task)"

            do {
                if let ditto = self.ditto {
                    try await ditto.store.execute(
                        query: query, arguments: ["task": newTask])
                }
            } catch {
                print(
                    "TasksRepository.\(#function) - ERROR creating new task: \(error.localizedDescription)"
                )
            }
        }
    }

    func deleteTask(_ task: TaskModel) {
        Task { [weak self] in
            guard let self else { return }
            let query = "UPDATE tasks SET deleted = true WHERE _id = :id"
            do {
                if let ditto = self.ditto {
                    try await ditto.store.execute(
                        query: query, arguments: ["id": task._id])
                }
            } catch {
                print(
                    "TasksRepository.\(#function) - ERROR deleting task: \(error.localizedDescription)"
                )
            }
        }
    }
}
