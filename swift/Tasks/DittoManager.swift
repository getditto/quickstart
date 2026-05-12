import DittoSwift
import Foundation

enum DittoManagerError: LocalizedError {
    case invalidAuthURL(String)

    var errorDescription: String? {
        switch self {
        case .invalidAuthURL(let value):
            return "DITTO_AUTH_URL is missing or not a valid URL: \"\(value)\". Check your .env configuration."
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
                databaseID: Env.DITTO_APP_ID,
                connect: .smallPeersOnly(privateKey: nil))
        } else {
            guard let authURL = URL(string: Env.DITTO_AUTH_URL) else {
                throw DittoManagerError.invalidAuthURL(Env.DITTO_AUTH_URL)
            }
            config = DittoConfig(
                databaseID: Env.DITTO_APP_ID,
                connect: .server(url: authURL))
        }

        do {
            let dittoOpened = try await Ditto.open(config: config)
            if isOffline {
                try dittoOpened.setOfflineOnlyLicenseToken(offlineLicenseToken)
            } else {
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
            }
            self.ditto = dittoOpened

        } catch {
            self.ditto = nil
            throw error
        }
    }
}
