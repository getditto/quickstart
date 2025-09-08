import DittoSwift
import Foundation

/// Owner of the Ditto object
class DittoManager: ObservableObject {
    let ditto: Ditto
    static let shared = DittoManager()

    private init() {
        // https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
        ditto = try! Ditto.openSync(
            config: DittoConfig(
                databaseID: Env.DITTO_APP_ID,
                connect: .server(url: URL(string: Env.DITTO_AUTH_URL)!)
            )
        )
        ditto.auth?.expirationHandler = { [weak self] ditto, secondsRemaining in
            ditto.auth?.login(token: Env.DITTO_PLAYGROUND_TOKEN, provider: .development
            ) { clientInfo, error in
                if let error = error {
                    print("Authentication failed: \(error)")
                } else {
                    print("Authentication successful")
                }
            }
        }

        // disable sync with v3 peers, required for DQL
        do {
            try ditto.disableSyncWithV3()
        } catch let error {
            print(
                "DittoManger - ERROR: disableSyncWithV3() failed with error \"\(error)\""
            )
        }

        let isPreview: Bool =
            ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if !isPreview {
            DittoLogger.minimumLogLevel = .debug
        }
    }
}
