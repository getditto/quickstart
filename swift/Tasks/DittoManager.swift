import DittoSwift
import Foundation

/// Owner of the Ditto object
class DittoManager: ObservableObject {
    @Published var ditto: Ditto?
    static let shared = DittoManager()

    private init() {}

    /// Initializes Ditto and configures logging. Handles thrown errors.
    func initDitto() async throws {
        // https://docs.ditto.live/sdk/latest/ditto-config
        let config = DittoConfig(
            databaseID: Env.DITTO_APP_ID,
            connect: .server(url: URL(string: Env.DITTO_AUTH_URL)!))

        do {
            let dittoOpened = try await Ditto.open(config: config)
            dittoOpened.auth?.expirationHandler = { ditto, secondsRemaining in
                // Authenticate when token is expiring. This closure must not throw.
                ditto.auth?.login(token: Env.DITTO_PLAYGROUND_TOKEN,
                                  provider: .development) { clientInfo, error in
                    if let error = error {
                        // Cannot throw from here; log the error instead.
                        print(
                            "Ditto auth refresh failed: \(error), " +
                            "client info: \(String(describing: clientInfo)), " +
                            "seconds remaining \(secondsRemaining)"
                        )
                    }
                }
            }
            self.ditto = dittoOpened

        } catch {
            self.ditto = nil
            throw error
        }

        // Configure logging for non-preview runs
        let isPreview: Bool = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if !isPreview {
            DittoLogger.minimumLogLevel = .debug
        }
    }
}
