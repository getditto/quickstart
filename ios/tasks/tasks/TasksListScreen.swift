import Combine
import DittoSwift
import SwiftUI

struct QueryExpr {
    var query: String
    var args: [String: Any?]
    init(_ query: String = "", _ args: [String: Any?] = [:]) {
        self.query = query
        self.args = args
    }
}

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

    init() {
        try? updateSubscription()
        try? updateStoreObserver()
    }

    var baseQuery: QueryExpr {
        var expr = QueryExpr()
        expr.query = """
            SELECT * FROM tasks
            WHERE NOT deleted
            ORDER BY _id
            """
        return expr
    }

    public func updateSubscription() throws {
        do {
            // If subscription changes, it must be cancelled before resetting
            // (Note: base subscription query does not change in this sample app)
            if let sub = subscription {
                sub.cancel()
                subscription = nil
            }

            subscription = try dittoSync.registerSubscription(
                query: baseQuery.query, arguments: baseQuery.args
            )
        } catch {
            print(
                "TaskListScreenVM.\(#function) - ERROR registering subscription: \(error.localizedDescription)"
            )
            throw error
        }
    }

    func updateStoreObserver() throws {
        do {
            // the store observer query expression changes to filter tasks based on selected usesrId
            if let observer = storeObserver {
                observer.cancel()
                storeObserver = nil
            }

            storeObserver = try dittoStore.registerObserver(
                query: baseQuery.query,
                arguments: baseQuery.args
            ) { [weak self] result in
                guard let self = self else { return }

                self.tasks = result.items.compactMap {
                    //                            TaskModel($0.value) // alternative contstructor
                    TaskModel($0.jsonString())
                }
            }
        } catch {
            print(
                "TaskListScreenVM.\(#function) - ERROR registering observer: \(error.localizedDescription)"
            )
            throw error
        }
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

    func clickedBody(task: TaskModel) {
        taskToEdit = task
        isPresentingEditScreen = true
    }

    func clickedNewTask() {
        taskToEdit = nil
        isPresentingEditScreen = true
    }
}

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    @StateObject var viewModel = TasksListScreenViewModel()

    var body: some View {
        NavigationView {
            List {
                ForEach(viewModel.tasks) { task in
                    TaskRow(
                        task: task,
                        onToggle: { task in
                            viewModel.toggleComplete(task: task)
                        },
                        onClickEdit: { task in
                            viewModel.clickedBody(task: task)
                        }
                    )
                }
            }
            .animation(.default, value: viewModel.tasks)
            .navigationTitle("Ditto Tasks")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack {
                        Text("App ID: \(Env.DITTO_APP_ID)")
                            .font(.caption2)
                            .foregroundColor(.gray)
                        Text("Token: \(Env.DITTO_PLAYGROUND_TOKEN)")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Menu {
                        Button("New Task") {
                            viewModel.clickedNewTask()
                        }
                    } label: {
                        Image(systemName: "plus")
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
    }
}

struct TasksListScreen_Previews: PreviewProvider {
    static var previews: some View {
        TasksListScreen()
    }
}
