import Combine
import DittoSwift
import SwiftUI

/// View model for TasksListScreen
@MainActor
class TasksListScreenViewModel: ObservableObject {

    @Published var useDQLForSearch = true {
        didSet {
            updateDittoObserver()
            // TODO - JZ - Hack for bug due to UI race condition...adding delay SwiftUI settle
            // would need to introduce async call, add a callback, or an additional event trigger
            if !useDQLForSearch {
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 200_000_000) // 0.2 seconds
                    updateDisplayedTasks()
                }
            }
        }
    }

    @Published var isCaseSensitive = false {
        didSet { updateDisplayedTasks() }
    }

    @Published var searchText: String = "" {
        didSet {
            guard searchText != oldValue else { return }
            updateDisplayedTasks()
        }
    }

    @Published var searchScope = TaskModelSearchScope.title {
        didSet { updateDisplayedTasks() }
    }

    @Published var showDittoTools = false
    @Published private(set) var displayedTasks: [TaskModel] = [] {
        didSet {
            logDisplayResults()
        }
    }
    @Published private(set) var dittoObserverTasks = [TaskModel]()

    @Published var isPresentingEditScreen: Bool = false
    @Published var observerQuery = ""

    private var cancellables = Set<AnyCancellable>()
    private(set) var taskToEdit: TaskModel?

    private let ditto = DittoManager.shared.ditto
    private var subscription: DittoSyncSubscription?
    private var storeObserver: DittoStoreObserver?

    let syncSubscriptionQuery = "SELECT * FROM tasks"

    init() {
        populateTasksCollection()
        updateDittoObserver()
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
            subscription = try ditto.sync.registerSubscription(query: syncSubscriptionQuery)
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

private extension TasksListScreenViewModel {

    private func updateDittoObserver() {
        var whereStatement = "WHERE NOT deleted"
        if !searchText.isEmpty && useDQLForSearch {
            whereStatement += " AND \(searchScope == .title ? "title" : "_id") \(isCaseSensitive ? "LIKE" : "ILIKE") '%\(searchText)%'"
        }
        observerQuery = "\(syncSubscriptionQuery) \(whereStatement) ORDER BY title ASC"
        debugPrint("TaskListScreenVM.\(#function) - Querying for tasks with DQL: [\(observerQuery)]")

        storeObserver = try? ditto.store.registerObserver(query: observerQuery) { [weak self] result in
            guard let self = self else { return }
            self.dittoObserverTasks = result.items.compactMap {
                TaskModel($0.jsonData())
            }
            self.displayedTasks = self.dittoObserverTasks
        }
    }

    private func updateDisplayedTasks() {
        if self.useDQLForSearch {
            updateDittoObserver()
        } else {
            performSearchInMemory()
        }
    }

    private func performSearchInMemory() {
        guard !searchText.isEmpty else {
            self.displayedTasks = dittoObserverTasks
            return
        }
        debugPrint("TaskListScreenVM.\(#function) - Performing Search in Memory from View Model data")

        self.displayedTasks = dittoObserverTasks.filter { task in
            if isCaseSensitive {
                switch searchScope {
                    case .title: task.title.contains(searchText)
                    case .id: task.id.contains(searchText)
                }
            } else {
                switch searchScope {
                    case .title: task.title.lowercased().contains(searchText.lowercased())
                    case .id: task.id.lowercased().contains(searchText.lowercased())
                }
            }
        }
    }

    private func logDisplayResults() {
        let count = displayedTasks.count
        let countCapInt = 5
        let countCapped = count > countCapInt ? countCapInt : count

        let searchText = "Search: \(searchText)"
        let searchResultsCount = "Results Count: \(count)"
        let searchResultsSamples = "\(countCapped) result samples \(Array(displayedTasks.prefix(countCapInt)))"
        let searchCaseSensitiveStatus = "Case Sensitive: \(isCaseSensitive)"
        debugPrint("TaskListScreenVM.\(#function) - \(searchText) | \(searchResultsCount) | \(searchResultsSamples) | \(searchCaseSensitiveStatus)")
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
                searchToggles
                Section(
                    header: VStack(alignment: .leading) {
                        Text("App ID: \(Env.DITTO_APP_ID)")
                        Text("Token: \(Env.DITTO_PLAYGROUND_TOKEN)").padding(.bottom)
                        searchInfoText
                    }
                    .font(.caption)
                    .textCase(nil)
                    .padding(.bottom)
                ) {
                    if viewModel.displayedTasks.count == 0 {
                        searchResultEmptyText
                    } else {
                        ForEach(viewModel.displayedTasks) { task in
                            TaskRow(
                                task: task,
                                onToggle: { task in
                                    viewModel.toggleComplete(task: task)
                                },
                                onClickEdit: { task in
                                    viewModel.onEdit(task: task)
                                },
                                searchScope: viewModel.searchScope
                            )
                        }
                        .onDelete(perform: deleteTaskItems)
                    }
                }
            }
            .animation(.default, value: viewModel.dittoObserverTasks)
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
                            }.padding(.horizontal)
                    }
                }
                ToolbarItem(placement: .bottomBar) {
                    newTaskButton
                }
            }
            .sheet(
                isPresented: $viewModel.isPresentingEditScreen,
                content: {
                    EditScreen(task: viewModel.taskToEdit)
                        .environmentObject(viewModel)
                })
            .dittoTaskSearch(text: $viewModel.searchText,
                             scope: $viewModel.searchScope,
                             placement: .toolbar)
            .dittoTools()
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

    private var newTaskButton: some View {
        Button {
            viewModel.onNewTask()
        } label: {
            HStack {
                Text("New Task")
                Image(systemName: "plus")
            }.frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .padding(.horizontal)
    }

    private var searchResultEmptyText: some View {
        VStack {
            Text("🎉 No tasks - take the day off 🎉\n\nAdd a new task by tapping the '+' button\(viewModel.searchText.isEmpty ? "" : " or change your current search").")
                .multilineTextAlignment(.center)
                .italic()
        }.frame(maxWidth: .infinity)
    }

    private var searchInfoText: some View {
        VStack(alignment: .leading) {
            Text("Sync Subscription Query [DQL]: \(viewModel.syncSubscriptionQuery)")
            Text("Observer Query [DQL]: \(viewModel.observerQuery)")
            if !viewModel.searchText.isEmpty {
                Text("Search Method: \(viewModel.useDQLForSearch ? "Swift DQL Observer" : "In-memory data from view model")").italic().padding(.top)
                Text("Total Search Results: \(viewModel.displayedTasks.count)").italic()
            }
        }
    }

    @ViewBuilder
    private var searchToggles: some View {
        if !viewModel.searchText.isEmpty {
            Toggle(isOn: $viewModel.useDQLForSearch) {
                Text("Search - Use DQL")
            }
            Toggle(isOn: $viewModel.isCaseSensitive) {
                Text("Search - Case Sensitive")
            }
        }
    }

    private func deleteTaskItems(at offsets: IndexSet) {
        let deletedTasks = offsets.map { viewModel.displayedTasks[$0] }
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
