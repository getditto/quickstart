import SwiftUI

struct TaskRow: View {
    let task: TaskModel

    var onToggle: ((_ task: TaskModel) -> Void)?
    var onClickEdit: ((_ task: TaskModel) -> Void)?

    var body: some View {
        HStack {
            Image(systemName: task.done ? "circle.fill" : "circle")
                .renderingMode(.template)
                .foregroundColor(.accentColor)
                .frame(minWidth: 32)
                .onTapGesture {
                    onToggle?(task)
                }
            if task.done {
                Text(task.title)
                    .strikethrough()
            } else {
                Text(task.title)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onClickEdit?(task)
        }
    }
}

struct TaskRow_Previews: PreviewProvider {
    static var previews: some View {
        List {
            TaskRow(task: TaskModel(title: "Get Milk", done: true))
            TaskRow(task: TaskModel(title: "Do Homework"))
            TaskRow(task: TaskModel(title: "Take out trash", done: true))
        }
    }
}
