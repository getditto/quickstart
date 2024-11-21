import Combine
import DittoSwift
import SwiftUI

/// View model for EditScreen
class EditScreenViewModel: ObservableObject {
    @Published var taskTitleText: String
    @Published var isExistingTask: Bool = false
    @Published var deleteRequested = false
    @Published var task: TaskModel
    private var _taskToEdit: TaskModel?  // TODO: why is this needed?

    init(task: TaskModel?) {
        self._taskToEdit = task
        self.task = task ?? TaskModel.new()
        self.taskTitleText = task?.title ?? ""
        isExistingTask = task != nil
    }

    func save(listVM: TasksListScreenViewModel) {
        if isExistingTask {
            task.title = taskTitleText
            task.deleted = deleteRequested
            listVM.saveEditedTask(task)
        } else {
            task.title = taskTitleText
            listVM.saveNewTask(task)
        }
    }
}

/// View for creating or editing a task
struct EditScreen: View {
    @EnvironmentObject var listVM: TasksListScreenViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject var viewModel: EditScreenViewModel
    @FocusState var titleHasFocus: Bool

    init(task: TaskModel?) {
        self._viewModel = StateObject(
            wrappedValue: EditScreenViewModel(task: task)
        )
    }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Title", text: $viewModel.taskTitleText)
                        .focused($titleHasFocus)

                    Toggle("Is Completed", isOn: $viewModel.task.done)
                }

                if viewModel.isExistingTask {
                    Section {
                        HStack {
                            Button(
                                action: {
                                    viewModel.deleteRequested.toggle()
                                },
                                label: {
                                    Text("Delete Task")
                                        .fontWeight(.bold)
                                        .foregroundColor(
                                            viewModel.deleteRequested
                                                ? .white : .red)
                                })

                            Spacer()

                            if viewModel.deleteRequested {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.white)
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .listRowBackground(
                        viewModel.deleteRequested ? Color.red : nil)
                }
            }
            .navigationTitle(
                viewModel.isExistingTask ? "Edit Task" : "Create Task"
            )
            .navigationBarItems(
                leading: Button("Cancel") {
                    dismiss()
                },
                trailing: Button(viewModel.isExistingTask ? "Save" : "Create") {
                    viewModel.save(listVM: listVM)
                    dismiss()
                }
            )
        }
        .onAppear {
            if !viewModel.isExistingTask {
                titleHasFocus = true
            }
        }
    }
}

struct EditScreen_Previews: PreviewProvider {
    static var previews: some View {
        EditScreen(
            task: TaskModel(title: "Get Milk", done: true)
        )
    }
}
