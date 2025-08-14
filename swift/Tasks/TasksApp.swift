import SwiftUI

@main
struct TasksApp: App {
    @State private var isLoading = true
    private let ditto = DittoManager.shared.ditto

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
                    // Disable DQL strict mode. When set to false, collection
                    // definitions are no longer required. SELECT queries will
                    // return and display all fields by default.
                    // https://docs.ditto.live/dql/strict-mode
                    try await ditto.store.execute(query: "ALTER SYSTEM SET DQL_STRICT_MODE = false")
                    isLoading = false
                } catch {
                    fatalError("Internal inconsistency, expected setting DQL strict mode to false to always succeed but it failed: \(error)")
                }
            }
        }
    }
}
