import DittoSwift
import Foundation

/// Owner of the Ditto object
class DittoManager: ObservableObject {
    let ditto: Ditto
    static let shared = DittoManager()

    private init() {
        var config = DittoConfig.default
        config.databaseID = Env.DITTO_APP_ID
        config.connect = .server(url: URL(string: Env.DITTO_AUTH_URL)!)

        // https://docs.ditto.live/sdk/latest/install-guides/swift#integrating-and-initializing-sync
        do {
            ditto = try Ditto.openSync(config: config)
        } catch {
            fatalError("Opening Ditto failed with error: \(error)")
        }

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
