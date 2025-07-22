import Combine
import DittoSwift
import SwiftUI

/// View for creating or editing a task
struct EditScreen: View {
    @Environment(\.dismiss) private var dismiss
    @FocusState var titleHasFocus: Bool
    @StateObject private var viewModel: ViewModel
    
    init(task: TaskModel?) {
        _viewModel = StateObject(wrappedValue: ViewModel(task: task))
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Title", text: $viewModel.task.title)
                        .focused($titleHasFocus)
                        .onSubmit(onSubmit)
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
                    onSubmit()
                }
            )
        }
        .onAppear {
            if !viewModel.isExistingTask {
                titleHasFocus = true
            }
        }
    }
    
    func onSubmit() {
        Task { @MainActor in
            await viewModel.save()
            dismiss()
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

extension EditScreen {
    
    /// View model for EditScreen
    class ViewModel: ObservableObject {
        private var dittoManager: DittoStateManager?
        
        @Published var isExistingTask: Bool = false
        @Published var deleteRequested = false
        @Published var task: TaskModel
        
        init(task: TaskModel? = nil) {
            isExistingTask = task != nil
            self.task = task ?? TaskModel()
        }
        
        func save() async {
            do {
                if isExistingTask {
                    task.deleted = deleteRequested
                    try await DittoService.shared.updateTaskModel(task)
                } else {
                    try await DittoService.shared.insertTaskModel(task)
                }
            } catch {
                print("Error saving task: \(error.localizedDescription)")
            }
        }
    }
}
