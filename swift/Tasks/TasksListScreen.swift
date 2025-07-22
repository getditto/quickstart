import Combine
import DittoSwift
import SwiftUI

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    private static let SYNC_ENABLED_KEY = "syncEnabled"

    @StateObject private var viewModel = ViewModel()
    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()
    
    var body: some View {
        NavigationView {
            if viewModel.errorMessage == nil {  // Reversed the condition
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
                                    Task {
                                        await viewModel.toggleComplete(task: task)
                                    }
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
                                    Task {
                                        do {
                                            try await viewModel.setSyncEnabled(newSyncEnabled)
                                        } catch {
                                            syncEnabled = false
                                        }
                                    }
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
                            .buttonStyle(.borderedProminent)
                            .padding(.bottom)
                        }
                    }
                }
            } else {
                if #available(iOS 17, *) {
                    ContentUnavailableView(
                        "Error",
                        systemImage: "exclamationmark.triangle.fill",
                        description: Text(viewModel.errorMessage ?? "An unknown error occurred.")
                    )
                } else {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.red)
                        Text("Error")
                            .font(.title2)
                            .bold()
                        Text(viewModel.errorMessage ?? "An unknown error occurred.")
                            .font(.body)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                }
            }
        }
        .sheet(
            isPresented: $viewModel.isPresentingEditScreen,
            content: {
                EditScreen(task: viewModel.taskToEdit)
            })
        .task {
            do {
                await viewModel.initialize()
                try await viewModel.setSyncEnabled(syncEnabled)
            } catch {
                syncEnabled = false
            }
        }
    }
    
    private func deleteTaskItems(at offsets: IndexSet) {
        let deletedTasks = offsets.map { viewModel.tasks[$0] }
        for task in deletedTasks {
            Task { @MainActor in
                await viewModel.deleteTask(task)
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

// MARK: TasksListScreen ViewModel
extension TasksListScreen {
    
    @MainActor
    class ViewModel: ObservableObject {
        private var cancellables = Set<AnyCancellable>()

        @Published var tasks: [TaskModel] = []
        @Published var isPresentingEditScreen = false
        @Published private(set) var taskToEdit: TaskModel?
        @Published var errorMessage: String? = nil
        
        func initialize() async {
            do {
                try await DittoService.shared.registerObservers(updateTaskModels: { [weak self] newTaskModels in
                    await self?.updateTasks(newTaskModels: newTaskModels)
                })
            } catch {
                //real apps would log this and do something else, we will just prpint the error to the screen
                errorMessage = "Failed to initialize and setup observer for Ditto: \(error.localizedDescription)"
            }
        }
        
        deinit {
            cancellables.removeAll()
        }

        func updateTasks(newTaskModels: [TaskModel]?) async {
            self.tasks.removeAll()
            if let newTaskModels {
                self.tasks.append(contentsOf: newTaskModels)
            }
        }

        func setSyncEnabled(_ newValue: Bool) async throws {
            do {
                try await DittoService.shared.setSyncEnabled(newValue)
            } catch {
                //real apps would log this and do something else, we will just prpint the error to the screen
                errorMessage = "Failed to set sync enabled state: \(error.localizedDescription)"
            }
        }
        
        func toggleComplete(task: TaskModel) async {
                do {
                    try await DittoService.shared.toggleComplete(task: task)
                } catch {
                    //real apps would log this and do something else, we will just prpint the error to the screen
                    errorMessage = "Failed to toggle task completion: \(error.localizedDescription)"
                }
        }
        
        nonisolated func saveEditedTask(_ task: TaskModel) async {
            do {
                try await DittoService.shared.updateTaskModel(task)
            } catch {
                Task { @MainActor in
                    //real apps would log this and do something else, we will just prpint the error to the screen
                    errorMessage = "Failed to save edited task: \(error.localizedDescription)"
                }
            }
        }
        
        nonisolated func saveNewTask(_ task: TaskModel) async {
            do {
                try await DittoService.shared.insertTaskModel(task)
            } catch {
                Task { @MainActor in
                    //real apps would log this and do something else, we will just prpint the error to the screen
                    errorMessage = "Failed to save new task: \(error.localizedDescription)"
                }
            }
        }
        
        nonisolated func deleteTask(_ task: TaskModel) async {
            do {
                try await DittoService.shared.deleteTaskModel(task)
            } catch {
                Task { @MainActor in
                    //real apps would log this and do something else, we will just prpint the error to the screen
                    errorMessage = "Failed to delete task: \(error.localizedDescription)"
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
}
