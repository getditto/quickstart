import Combine
import DittoSwift
import SwiftUI

/// View model for TasksListScreen
@MainActor
class TasksListScreenViewModel: ObservableObject {
    @Published var tasks = [TaskModel]()
    @Published var isPresentingEditScreen: Bool = false
    private(set) var taskToEdit: TaskModel?

    private let dittoSync = DittoManager.shared.ditto.sync
    private let dittoStore = DittoManager.shared.ditto.store
    private var subscription: DittoSyncSubscription?
    private var storeObserver: DittoStoreObserver?

    private let query = """
        SELECT * FROM tasks
        WHERE NOT deleted
        ORDER BY _id
        """

    init() {
        storeObserver = try? dittoStore.registerObserver(query: query) {
            [weak self] result in
            guard let self = self else { return }
            self.tasks = result.items.compactMap {
                TaskModel($0.jsonString())
            }
        }
    }

    deinit {
        if let sub = subscription {
            sub.cancel()
            subscription = nil
        }
        if let obs = storeObserver {
            obs.cancel()
            storeObserver = nil
        }
        if DittoManager.shared.ditto.isSyncActive {
            DittoManager.shared.ditto.stopSync()
        }
    }

    func setSyncEnabled(_ newValue: Bool) throws {
        if !DittoManager.shared.ditto.isSyncActive && newValue {
            try startSync()
        } else if DittoManager.shared.ditto.isSyncActive && !newValue {
            stopSync()
        }
    }

    private func startSync() throws {
        do {
            try DittoManager.shared.ditto.startSync()
            subscription = try dittoSync.registerSubscription(query: query)
        } catch {
            print(
                "TaskListScreenVM.\(#function) - ERROR starting sync operations: \(error.localizedDescription)"
            )
            throw error
        }
    }

    private func stopSync() {
        if let sub = subscription {
            sub.cancel()
            subscription = nil
        }

        DittoManager.shared.ditto.stopSync()
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
                try await dittoStore.execute(
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
                    done = :done,
                    deleted = :deleted
                WHERE _id == :_id
                """

            do {
                try await dittoStore.execute(
                    query: query,
                    arguments: [
                        "done": task.done,
                        "deleted": task.deleted,
                        "_id": task._id,
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
                try await dittoStore.execute(
                    query: query, arguments: ["newTask": newTask])
            } catch {
                print(
                    "EditScreenVM.\(#function) - ERROR creating new task: \(error.localizedDescription)"
                )
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
    private static let SYNC_ENABLED_KEY = "syncEnabled"

    @StateObject var viewModel = TasksListScreenViewModel()

    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()

    var body: some View {
        NavigationView {
            List {
                Section(
                    header: VStack {
                        Text("App ID: \(Env.DITTO_APP_ID)")
                        Text("Token: \(Env.DITTO_PLAYGROUND_TOKEN)")
                    }.font(.caption).textCase(nil)
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
                }
            }
            .animation(.default, value: viewModel.tasks)
            .navigationTitle("Ditto Tasks")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Toggle("Sync", isOn: $syncEnabled)
                        .toggleStyle(SwitchToggleStyle())
                        .onChange(of: syncEnabled) {
                            Self.saveSyncEnabledState(syncEnabled)
                            do {
                                try viewModel.setSyncEnabled(syncEnabled)
                            } catch {
                                syncEnabled = false
                            }
                        }
                }
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Spacer()
                        Button(action: {
                            viewModel.onNewTask()
                        }) {
                            HStack {
                                Image(systemName: "plus")
                                Text("New Task")
                            }
                        }
                        .padding()
                        .buttonStyle(.borderedProminent)
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

    private static func loadSyncEnabledState() -> Bool {
        if UserDefaults.standard.object(forKey: SYNC_ENABLED_KEY) == nil {
            return true
        } else {
            return UserDefaults.standard.bool(forKey: SYNC_ENABLED_KEY)
        }
    }

    private static func saveSyncEnabledState(_ state: Bool) {
        UserDefaults.standard.set(state, forKey: SYNC_ENABLED_KEY)
        UserDefaults.standard.synchronize()
    }
}

struct TasksListScreen_Previews: PreviewProvider {
    static var previews: some View {
        TasksListScreen()
    }
}
