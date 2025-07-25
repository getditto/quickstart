import DittoSwift
import Foundation

/// Owner of the Ditto object
class DittoManager: ObservableObject {
    let ditto: Ditto
    static let shared = DittoManager()

    init() {
        // https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
        ditto = Ditto(
            identity: .onlinePlayground(
                appID: Env.DITTO_APP_ID,
                token: Env.DITTO_PLAYGROUND_TOKEN,
                // This is required to be set to false to use the correct URLs
                // This only disables cloud sync when the webSocketURL is not set explicitly
                enableDittoCloudSync: false, 
                customAuthURL: URL(string: Env.DITTO_AUTH_URL)
            )
        )

        // Set the Ditto Websocket URL
        ditto.updateTransportConfig { transportConfig in
            transportConfig.connect.webSocketURLs.insert(Env.DITTO_WEBSOCKET_URL)
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
