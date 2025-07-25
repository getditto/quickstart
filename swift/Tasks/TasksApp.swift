import SwiftUI

@main
struct TasksApp: App {
    let ditto = DittoManager.shared.ditto

    var body: some Scene {
        WindowGroup {
            TasksListScreen().task {
                do {
                    try await ditto.store.execute(query: "ALTER SYSTEM SET DQL_STRICT_MODE = false")
                } catch {
                    fatalError("Internal inconsistency, expected setting DQL strict mode to false to always succeed but it failed: \(error)")
                }
            }
        }
    }
}
