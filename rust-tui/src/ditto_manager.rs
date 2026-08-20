use std::{sync::Arc, time::Duration};

use anyhow::{Context, Result};
use dittolive_ditto::{fs::TempRoot, prelude::*, Ditto};

/// Owns everything about configuring and running the Ditto instance: opening
/// it, installing the development-mode authentication handler, and configuring
/// transports. It knows nothing about tasks.
///
/// It vends the configured Ditto instance (via [`DittoManager::ditto`]) for the
/// rest of the app to use the real Ditto API directly, and owns sync control.
/// Callers start sync explicitly with [`DittoManager::start_sync`].
pub struct DittoManager {
    // `ditto` is declared before `temp_root` so that, on drop, the Ditto
    // instance is torn down before `TempRoot` deletes the persistence
    // directory. `TempRoot` deletes its directory when its last `Arc` drops,
    // and Ditto does not hold that `Arc` — so a live `Arc<Ditto>` must not
    // outlive it.
    ditto: Arc<Ditto>,

    /// Held only to keep the persistence directory alive for the lifetime of
    /// the Ditto instance (see the field-ordering note above).
    pub temp_root: Arc<TempRoot>,
}

impl DittoManager {
    /// Open a Ditto instance, install the development-mode auth expiration
    /// handler, and configure transports. Does not start sync — call
    /// [`DittoManager::start_sync`].
    pub fn try_new(
        database_id: String,
        token: String,
        server_url: String,
        p2p_enabled: bool,
    ) -> Result<Self> {
        let connect_config = DittoConfigConnect::Server {
            url: server_url.parse().context("failed to parse server URL")?,
        };

        // We use a temporary directory to store Ditto's local database. This
        // means data is not persistent between runs, but it lets us run
        // multiple instances concurrently on the same machine. A production
        // application would store the database in a more permanent location.
        //
        // `TempRoot` deletes its directory when its last `Arc` is dropped, so
        // the manager holds it for the lifetime of the Ditto instance.
        let temp_root = Arc::new(TempRoot::new());
        let config = DittoConfig::new(database_id.clone(), connect_config)
            .with_persistence_directory(temp_root.root_path());

        let ditto = Ditto::open_sync(config)?;

        ditto
            .auth()
            .context("failed to get authenticator")?
            .set_expiration_handler(TokenHandler { token });

        ditto.update_transport_config(|config| {
            if p2p_enabled {
                // Enable all peer-to-peer transports (original behavior)
                config.enable_all_peer_to_peer();
            } else {
                // Explicitly disable all peer-to-peer transports - only use Big Peer
                config.peer_to_peer.bluetooth_le.enabled = false;
                config.peer_to_peer.lan.enabled = false;
            }
        });

        tracing::info!(database_id = %database_id, "Opened Ditto!");
        Ok(Self {
            ditto: Arc::new(ditto),
            temp_root,
        })
    }

    /// The configured Ditto instance, so callers can use the real Ditto API
    /// directly.
    pub fn ditto(&self) -> Arc<Ditto> {
        self.ditto.clone()
    }

    /// Start syncing data with other peers.
    pub fn start_sync(&self) -> Result<()> {
        self.ditto.sync().start()?;
        Ok(())
    }

    /// Stop syncing data with other peers.
    pub fn stop_sync(&self) {
        self.ditto.sync().stop();
    }

    /// Whether sync is currently active.
    pub fn is_sync_active(&self) -> bool {
        self.ditto.sync().is_active()
    }
}

struct TokenHandler {
    token: String,
}

impl DittoAuthExpirationHandler for TokenHandler {
    async fn on_expiration(&self, ditto: &Ditto, _duration_remaining: Duration) {
        let Some(auth) = ditto.auth() else {
            tracing::error!("Failed to get authenticator during token refresh");
            return;
        };
        match auth.login(self.token.as_str(), &identity::get_development_provider()) {
            Ok(_) => tracing::info!("Authentication successful"),
            Err(e) => tracing::error!(%e, "Authentication failed"),
        }
    }
}
