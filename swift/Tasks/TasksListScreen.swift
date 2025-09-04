import Combine
import DittoSwift
import SwiftUI

/// View model for TasksListScreen
@MainActor
class TasksListScreenViewModel: ObservableObject {
    @Published var tasks = [TaskModel]()
    @Published var isPresentingEditScreen: Bool = false
    private(set) var taskToEdit: TaskModel?

    private let ditto = DittoManager.shared.ditto
    private var subscription: DittoSyncSubscription?
    private var storeObserver: DittoStoreObserver?

    private let subscriptionQuery = "SELECT * from tasks"

    private let observerQuery = "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC"

    init() {
        populateTasksCollection()

        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        storeObserver = try? ditto.store.registerObserver(query: observerQuery) {
            [weak self] result in
            guard let self = self else { return }
            self.tasks = result.items.compactMap {
                TaskModel($0.jsonData())
            }
        }
    }

    deinit {
        subscription?.cancel()
        subscription = nil

        storeObserver?.cancel()
        storeObserver = nil

        if ditto.isSyncActive {
            DittoManager.shared.ditto.stopSync()
        }
    }

    func setSyncEnabled(_ newValue: Bool) throws {
        if !ditto.isSyncActive && newValue {
            try startSync()
        } else if ditto.isSyncActive && !newValue {
            stopSync()
        }
    }

    private func startSync() throws {
        do {
            try ditto.startSync()

            // Register a subscription, which determines what data syncs to this peer
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            subscription = try ditto.sync.registerSubscription(query: subscriptionQuery)
        } catch {
            print(
                "TaskListScreenVM.\(#function) - ERROR starting sync operations: \(error.localizedDescription)"
            )
            throw error
        }
    }

    private func stopSync() {
        subscription?.cancel()
        subscription = nil

        ditto.stopSync()
    }

    func toggleComplete(task: TaskModel) {
        Task {
            let done = !task.done
            let query = """
                UPDATE tasks
                SET done = :done
                WHERE _id == :_id
                """

            do {
                try await ditto.store.execute(
                    query: query,
                    arguments: ["done": done, "_id": task._id]
                )
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR toggling task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func saveEditedTask(_ task: TaskModel) {
        Task {
            let query = """
                UPDATE tasks SET
                    title = :title,
                    done = :done,
                    deleted = :deleted
                WHERE _id == :_id
                """

            do {
                try await ditto.store.execute(
                    query: query,
                    arguments: [
                        "title": task.title,
                        "done": task.done,
                        "deleted": task.deleted,
                        "_id": task._id
                    ]
                )
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR updating task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func saveNewTask(_ task: TaskModel) {
        Task {
            let newTask = task.value
            let query = "INSERT INTO tasks DOCUMENTS (:newTask)"

            do {
                try await ditto.store.execute(
                    query: query, arguments: ["newTask": newTask])
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR creating new task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func deleteTask(_ task: TaskModel) {
        Task {
            let query = "UPDATE tasks SET deleted = true WHERE _id = :_id"
            do {
                try await ditto.store.execute(
                    query: query, arguments: ["_id": task._id])
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR deleting task: \(error.localizedDescription)"
                )
            }
        }
    }

    private nonisolated func populateTasksCollection() {
        Task {
            let initialTasks: [TaskModel] = [
                TaskModel(
                    _id: "50191411-4C46-4940-8B72-5F8017A04FA7",
                    title: "Buy groceries"),
                TaskModel(
                    _id: "6DA283DA-8CFE-4526-A6FA-D385089364E5",
                    title: "Clean the kitchen"),
                TaskModel(
                    _id: "5303DDF8-0E72-4FEB-9E82-4B007E5797F0",
                    title: "Schedule dentist appointment"),
                TaskModel(
                    _id: "38411F1B-6B49-4346-90C3-0B16CE97E174",
                    title: "Pay bills")
            ]

            for task in initialTasks {
                do {
                    try await ditto.store.execute(
                        query: "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                        arguments: [
                            "task":
                                [
                                    "_id": task._id,
                                    "title": task.title,
                                    "done": task.done,
                                    "deleted": task.deleted
                                ]
                        ]
                    )
                } catch {
                    print(
                        "TaskListScreenVM.\(#function) - ERROR creating initial task: \(error.localizedDescription)"
                    )
                }
            }
        }
    }

    func onEdit(task: TaskModel) {
        taskToEdit = task
        isPresentingEditScreen = true
    }

    func onNewTask() {
        taskToEdit = nil
        isPresentingEditScreen = true
    }
}

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    private static let isSyncEnabledKey = "syncEnabled"

    @StateObject var viewModel = TasksListScreenViewModel()

    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()

    var body: some View {
        NavigationView {
            List {
                Section(
                    header: VStack {
                        Text("App ID: \(Env.DITTO_APP_ID)")
                        Text("Token: \(Env.DITTO_PLAYGROUND_TOKEN)")
                    }
                    .font(.caption)
                    .textCase(nil)
                    .padding(.bottom)
                ) {
                    ForEach(viewModel.tasks) { task in
                        TaskRow(
                            task: task,
                            onToggle: { task in
                                viewModel.toggleComplete(task: task)
                            },
                            onClickEdit: { task in
                                viewModel.onEdit(task: task)
                            }
                        )
                    }
                    .onDelete(perform: deleteTaskItems)
                }
            }
            .animation(.default, value: viewModel.tasks)
            .navigationTitle("Ditto Tasks")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack {
                        Toggle("Sync", isOn: $syncEnabled)
                            .toggleStyle(SwitchToggleStyle())
                            .onChange(of: syncEnabled) { newSyncEnabled in
                                Self.saveSyncEnabledState(newSyncEnabled)
                                do {
                                    try viewModel.setSyncEnabled(newSyncEnabled)
                                } catch {
                                    syncEnabled = false
                                }
                            }
                    }
                }
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Spacer()
                        Button(action: {
                            viewModel.onNewTask()
                        }, label: {
                            HStack {
                                Image(systemName: "plus")
                                Text("New Task")
                            }
                        })
                        .buttonStyle(.borderedProminent)
                        .padding(.bottom)
                    }
                }
            }
            .sheet(
                isPresented: $viewModel.isPresentingEditScreen,
                content: {
                    EditScreen(task: viewModel.taskToEdit)
                        .environmentObject(viewModel)
                })
        }
        .navigationViewStyle(.stack)
        .onAppear {
            // Prevent Xcode previews from syncing: non-preview simulators and real devices can sync
            let isPreview: Bool =
                ProcessInfo.processInfo.environment[
                    "XCODE_RUNNING_FOR_PREVIEWS"]
                == "1"
            if !isPreview {
                do {
                    try viewModel.setSyncEnabled(syncEnabled)
                } catch {
                    syncEnabled = false
                }
            }
        }
    }

    private func deleteTaskItems(at offsets: IndexSet) {
        let deletedTasks = offsets.map { viewModel.tasks[$0] }
        for task in deletedTasks {
            viewModel.deleteTask(task)
        }
    }

    private static func loadSyncEnabledState() -> Bool {
        if UserDefaults.standard.object(forKey: isSyncEnabledKey) == nil {
            return true
        } else {
            return UserDefaults.standard.bool(forKey: isSyncEnabledKey)
        }
    }

    private static func saveSyncEnabledState(_ state: Bool) {
        UserDefaults.standard.set(state, forKey: isSyncEnabledKey)
        UserDefaults.standard.synchronize()
    }
}

struct TasksListScreen_Previews: PreviewProvider {
    static var previews: some View {
        TasksListScreen()
    }
}
