import SwiftUI

@main
struct TasksApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var dittoManager = DittoManager()
    @State private var error: Error?
    @State private var isInitialized = false

    var body: some Scene {
        WindowGroup {
            Group {
                if dittoManager.isInitialized {
                    TasksListScreen()
                        .environmentObject(dittoManager)
                } else {
                    ProgressView("Loading...")
                }
            }
            .alert(
                "Error",
                isPresented: Binding(
                    get: { error != nil },
                    set: { if !$0 { error = nil } }
                )
            ) {
                Button("OK", role: .cancel) {
                    error = nil
                }
            } message: {
                Text(error?.localizedDescription ?? "Unknown Error")
            }
        }
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .background:
                // An example on how you might handle the app going to background
                // by closing connections and sync so that the OS doesn't terminate the app
                // for using too much resources
                deinitializeDitto()
                break
            case .inactive:
                // An example on how you might handle the app going inactive
                // by closing connections and sync so that the OS doesn't terminate the app
                // for using too much resources
                deinitializeDitto()
                break
            case .active:
                // App became active again - check if Ditto is initialized
                    Task {
                        do {
                            try await dittoManager.initializeIfNeeded()
                        } catch {
                            self.error = error
                        }
                    }
                break
            default:
                break
            }
        }
    }
    
    func deinitializeDitto() {
        dittoManager.deinitialize()
    }
}
