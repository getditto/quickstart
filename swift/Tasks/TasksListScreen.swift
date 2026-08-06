import DittoSwift
import Foundation
import SwiftUI

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    private static let isSyncEnabledKey = "syncEnabled"

    @StateObject var tasksRepository = TasksRepository()

    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()
    @State private var isPresentingEditScreen: Bool = false
    @State private var taskToEdit: TaskModel?

    var body: some View {
        NavigationView {
            List {
                Section(
                    header: VStack {
                        Text("Database ID: \(Env.DITTO_DATABASE_ID)")
                        Text("Token: \(Env.DITTO_DEVELOPMENT_TOKEN)")
                    }
                    .font(.caption)
                    .textCase(nil)
                    .padding(.bottom)
                ) {
                    ForEach(tasksRepository.tasks) { task in
                        TaskRow(
                            task: task,
                            onToggle: { task in
                                tasksRepository.toggleComplete(task: task)
                            },
                            onClickEdit: { task in
                                onEdit(task: task)
                            }
                        )
                    }
                    .onDelete(perform: deleteTaskItems)
                }
            }
            .animation(.default, value: tasksRepository.tasks)
            .navigationTitle("Ditto Tasks")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack {
                        Toggle("Sync", isOn: $syncEnabled)
                            .padding(.leading, 8)
                            .toggleStyle(SwitchToggleStyle())
                            .onChange(of: syncEnabled) { newSyncEnabled in
                                Self.saveSyncEnabledState(newSyncEnabled)
                                do {
                                    try DittoManager.shared.setSyncEnabled(newSyncEnabled)
                                } catch {
                                    syncEnabled = false
                                }
                            }
                    }
                }
                ToolbarItemGroup(placement: .bottomBar) {
                    Spacer()
                    Button(action: {
                        onNewTask()
                    }, label: {
                        Label("New Task", systemImage: "plus")
                    })
                    .buttonStyle(.borderedProminent)
                    .padding(.bottom)
                    Spacer()
                }
            }
            .sheet(
                isPresented: $isPresentingEditScreen,
                content: {
                    EditScreen(task: taskToEdit)
                        .environmentObject(tasksRepository)
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
                    try DittoManager.shared.setSyncEnabled(syncEnabled)
                } catch {
                    syncEnabled = false
                }
            }
        }
        .onDisappear {
            tasksRepository.teardown()
            DittoManager.shared.stopSync()
        }
    }

    private func onEdit(task: TaskModel) {
        taskToEdit = task
        isPresentingEditScreen = true
    }

    private func onNewTask() {
        taskToEdit = nil
        isPresentingEditScreen = true
    }

    private func deleteTaskItems(at offsets: IndexSet) {
        let deletedTasks = offsets.map { tasksRepository.tasks[$0] }
        for task in deletedTasks {
            tasksRepository.deleteTask(task)
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
    }
}

struct TasksListScreen_Previews: PreviewProvider {
    static var previews: some View {
        TasksListScreen()
    }
}
