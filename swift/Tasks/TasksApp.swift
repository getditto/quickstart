import SwiftUI

@main
struct TasksApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @State private var error: Error?
    @StateObject private var dittoStateManager = DittoStateManager()

    var body: some Scene {
        WindowGroup {
            Group {
                if (dittoStateManager.isInitialized) {
                    TasksListScreen()
                } else{
                    ProgressView("Initializing...")
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
            .onChange(of: scenePhase) { newPhase in
                switch newPhase {
                case .background:
                    // An example on how you might handle the app going to background
                    // by closing connections and sync so that the OS doesn't terminate the app
                    // for using too much resources
                    Task {
                        await deinitializeDitto()
                    }
                    break
                case .inactive:
                    // An example on how you might handle the app going inactive
                    // by closing connections and sync so that the OS doesn't terminate the app
                    // for using too much resources
                    Task {
                        await deinitializeDitto()
                    }
                    break
                case .active:
                    // App became active again - check if Ditto is initialized
                    Task {
                        do {
                            try await DittoService.shared.initializeIfNeeded(dittoStateManager: dittoStateManager)
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
    }

    func deinitializeDitto() async {
        await DittoService.shared.deinitialize(dittoStateManager: dittoStateManager)
    }
}
