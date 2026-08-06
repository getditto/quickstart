import DittoSwift
import Foundation

enum DittoManagerError: LocalizedError {
    case invalidServerURL(String)

    var errorDescription: String? {
        switch self {
        case .invalidServerURL(let value):
            return "DITTO_SERVER_URL is missing or not a valid URL: \"\(value)\". Check your .env configuration."
        }
    }
}

/// Owner of the Ditto object
@MainActor
class DittoManager: ObservableObject {
    @Published var ditto: Ditto?
    static let shared = DittoManager()

    private init() {}

    /// Initializes Ditto and configures logging. Handles thrown errors.
    func initDitto() async throws {
         // Configure logging for non-preview runs
        let isPreview: Bool = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if !isPreview {
            DittoLogger.minimumLogLevel = .debug
        }

        let offlineLicenseToken = Env.DITTO_OFFLINE_LICENSE_TOKEN
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let isOffline = !offlineLicenseToken.isEmpty

        // https://docs.ditto.live/sdk/latest/ditto-config
        let config: DittoConfig
        if isOffline {
            config = DittoConfig(
                databaseID: Env.DITTO_DATABASE_ID,
                connect: .smallPeersOnly(privateKey: nil))
        } else {
            guard let serverURL = URL(string: Env.DITTO_SERVER_URL) else {
                throw DittoManagerError.invalidServerURL(Env.DITTO_SERVER_URL)
            }
            config = DittoConfig(
                databaseID: Env.DITTO_DATABASE_ID,
                connect: .server(url: serverURL))
        }

        do {
            let dittoOpened = try await Ditto.open(config: config)
            if isOffline {
                try dittoOpened.setOfflineOnlyLicenseToken(offlineLicenseToken)
            } else {
                dittoOpened.auth?.expirationHandler = { ditto, secondsRemaining in
                // Authenticate when token is expiring. This closure must not throw.
                ditto.auth?.login(token: Env.DITTO_DEVELOPMENT_TOKEN,
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
            }
            self.ditto = dittoOpened

        } catch {
            self.ditto = nil
            throw error
        }
    }

    /// Enables or disables sync, starting or stopping the sync engine to match
    /// the requested state. Registering *what* syncs (the tasks subscription) is
    /// owned by `TasksRepository`; this only toggles the replication engine.
    func setSyncEnabled(_ newValue: Bool) throws {
        if let ditto = self.ditto {
            if !ditto.sync.isActive && newValue {
                try startSync()
            } else if ditto.sync.isActive && !newValue {
                stopSync()
            }
        }
    }

    /// Starts the sync engine so this peer replicates with other peers and the
    /// Ditto server. Subscriptions determine what data syncs; see `TasksRepository`.
    func startSync() throws {
        do {
            if let ditto = self.ditto {
                try ditto.sync.start()
            }
        } catch {
            print(
                "DittoManager.\(#function) - ERROR starting sync operations: \(error.localizedDescription)"
            )
            throw error
        }
    }

    /// Stops the sync engine. Subscriptions stay registered, so toggling sync
    /// back on resumes syncing the tasks collection.
    func stopSync() {
        if let ditto = self.ditto, ditto.sync.isActive {
            ditto.sync.stop()
        }
    }
}
