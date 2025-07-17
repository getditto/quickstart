import SwiftUI

@main
struct TasksApp: App {
    @StateObject private var dittoManager = DittoManager()
    @State private var error: Error?
    @State private var isInitialized = false
    
    var body: some Scene {
        WindowGroup {
            Group {
                if isInitialized {
                    TasksListScreen()
                        .environmentObject(dittoManager)
                } else {
                    ProgressView("Loading...")
                }
            }
            .alert("Error", isPresented: Binding(
                get: { error != nil },
                set: { if !$0 { error = nil } }
            )) {
                Button("OK", role: .cancel) {
                    error = nil
                }
            } message: {
                Text(error?.localizedDescription ?? "Unknown Error")
            }
            .task {
                if !isInitialized {
                    do {
                        try await dittoManager.initialize()
                        isInitialized = true
                    } catch {
                        self.error = error
                    }
                }
            }
        }
    }
}
