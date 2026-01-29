import SwiftUI

@main
struct TasksApp: App {
    @State private var isLoading = true

    var body: some Scene {
        WindowGroup {
            Group {
                if isLoading {
                    ProgressView("Loading ...")
                } else {
                    TasksListScreen()
                }
            }
            .task {
                do {
                    try await DittoManager.shared.initDitto()
                    isLoading = false
                } catch {
                    fatalError("Failed to initialize Ditto: \(error)")
                }
            }
        }
    }
}
