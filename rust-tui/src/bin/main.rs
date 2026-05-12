use std::{path::PathBuf, sync::Arc, time::Duration};

use anyhow::{anyhow, Context, Result};
use clap::Parser;
use ditto_quickstart::{term, tui::TuiTask, Shutdown};
use dittolive_ditto::{
    fs::TempRoot,
    identity::{OfflinePlayground, OnlinePlayground},
    AppId, Ditto,
};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[derive(Debug, Parser)]
pub struct Cli {
    /// The Ditto App ID this app will use to initialize Ditto
    #[clap(long, env = "DITTO_APP_ID")]
    app_id: AppId,

    /// The Online Playground token this app should use for authentication
    #[clap(long, env = "DITTO_PLAYGROUND_TOKEN", default_value = "")]
    token: String,

    /// The custom auth URL this app should use for authentication
    #[clap(long, env = "DITTO_AUTH_URL", default_value = "")]
    custom_auth_url: String,

    /// The websocket URL this app should use for authentication
    #[clap(long, env = "DITTO_WEBSOCKET_URL", default_value = "")]
    websocket_url: String,

    /// Optional offline-only license token. When non-empty, the app
    /// initializes in offline-only mode and the playground/auth/websocket
    /// values above are not used.
    #[clap(long, env = "DITTO_OFFLINE_LICENSE_TOKEN", default_value = "")]
    offline_license_token: String,

    /// Optional client name to display in the TUI
    #[clap(long, env = "DITTO_CLIENT_NAME")]
    client_name: Option<String>,

    /// Enable peer-to-peer transports (LAN, Bluetooth). Set to false to force all communication through Big Peer.
    #[clap(long, env = "DITTO_P2P_ENABLED", default_value = "true")]
    p2p_enabled: bool,

    /// Path to write logs on disk
    #[clap(long, default_value = "/tmp/ditto-quickstart.log")]
    log: PathBuf,
}

impl Cli {
    pub fn try_init_tracing(&self) -> Result<()> {
        let logfile = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(&self.log)
            .with_context(|| format!("failed to open logfile {}", self.log.display()))?;
        tracing_subscriber::registry()
            .with(tracing_subscriber::fmt::layer().with_writer(logfile))
            .try_init()?;
        Ok(())
    }
}

#[tokio::main]
async fn main() -> Result<()> {
    try_init_dotenv().ok();
    let cli = Cli::parse();
    cli.try_init_tracing()?;
    let shutdown = <Shutdown>::new();
    let (terminal, _cleanup) = term::init_crossterm()?;

    // Initialize and launch app
    let ditto = try_init_ditto(
        cli.app_id,
        cli.token,
        cli.custom_auth_url,
        cli.websocket_url.clone(),
        cli.offline_license_token.clone(),
        cli.p2p_enabled,
    )
    .await?;
    let _tui_task = TuiTask::try_spawn(
        shutdown.clone(),
        terminal,
        ditto,
        cli.websocket_url,
        cli.client_name,
    )
    .context("failed to start tui task")?;
    tracing::info!(success = true, "Initialized!");

    // Wait for shutdown trigger
    tokio::select! {
        reason = shutdown.wait_shutdown_triggered() => {
            tracing::info!(%reason, "[SHUTDOWN] Shutdown triggered, cleaning up");
        }
        _ = tokio::signal::ctrl_c() => {
            _ = shutdown.trigger_shutdown(anyhow!("Received SIGTERM (^C)").into());
            tracing::info!("[SHUTDOWN] Received shutdown signal, cleaning up");
        }
    }

    // Wait for shutdown to complete or timeout
    drop(_cleanup);
    tokio::select! {
        _ = shutdown.wait_shutdown_complete() => {
            tracing::info!("[SHUTDOWN] Graceful shutdown complete, quitting");
        }
        _ = tokio::time::sleep(Duration::from_secs(2)) => {
            tracing::error!("[SHUTDOWN] Graceful shutdown timer expired, force-quitting!");
            std::process::exit(1);
        }
    }

    tracing::info!("Moving to quit");
    Ok(())
}

async fn try_init_ditto(
    app_id: AppId,
    token: String,
    custom_auth_url: String,
    websocket_url: String,
    offline_license_token: String,
    p2p_enabled: bool,
) -> Result<Ditto> {
    let mode = select_mode(&offline_license_token);

    if mode == DittoMode::OnlinePlayground {
        let missing: Vec<&str> = [
            ("DITTO_PLAYGROUND_TOKEN", token.trim()),
            ("DITTO_AUTH_URL", custom_auth_url.trim()),
            ("DITTO_WEBSOCKET_URL", websocket_url.trim()),
        ]
        .into_iter()
        .filter(|(_, v)| v.is_empty())
        .map(|(k, _)| k)
        .collect();
        if !missing.is_empty() {
            anyhow::bail!(
                "Online Playground mode requires: {}. Set DITTO_OFFLINE_LICENSE_TOKEN to use offline mode instead.",
                missing.join(", ")
            );
        }
    }

    // We use a temporary directory to store Ditto's local database.
    // This means that data will not be persistent between runs of the
    // application, but it allows us to run multiple instances of the
    // application concurrently on the same machine.  For a production
    // application, we would want to store the database in a more permanent
    // location, and if multiple instances are needed, ensure that each
    // instance has its own persistence directory.
    let builder = Ditto::builder().with_root(Arc::new(TempRoot::new()));
    let ditto = match mode {
        DittoMode::Offline => builder
            .with_identity(|root| OfflinePlayground::new(root, app_id.clone()))?
            .build()?,
        DittoMode::OnlinePlayground => builder
            .with_identity(|root| {
                OnlinePlayground::new(
                    root,
                    app_id.clone(),
                    token,
                    false, // This is required to be set to false to use the correct URLs
                    Some(custom_auth_url.as_str()),
                )
            })?
            .build()?,
    };

    if let DittoMode::Offline = mode {
        ditto.set_offline_only_license_token(offline_license_token.trim())?;
    }

    ditto.update_transport_config(|config| {
        if p2p_enabled {
            // Enable all peer-to-peer transports (original behavior)
            config.enable_all_peer_to_peer();
        } else {
            // Explicitly disable all peer-to-peer transports - only use Big Peer
            config.peer_to_peer.bluetooth_le.enabled = false;
            config.peer_to_peer.lan.enabled = false;
        }

        if let DittoMode::OnlinePlayground = mode {
            // Set WebSocket URL for Big Peer connection (online mode only)
            config.connect.websocket_urls.insert(websocket_url);
        }
    });

    // disable sync with v3 peers, required for DQL
    _ = ditto.disable_sync_with_v3();

    // disable DQL strict mode
    // https://docs.ditto.live/dql/strict-mode
    _ = ditto
        .store()
        .execute_v2("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        .await?;

    // Start sync
    _ = ditto.start_sync();

    tracing::info!(%app_id, "Started Ditto!");
    Ok(ditto)
}

/// Identity-mode selection based on env vars. Non-empty
/// `DITTO_OFFLINE_LICENSE_TOKEN` (after trim) selects [`DittoMode::Offline`];
/// otherwise the app uses [`DittoMode::OnlinePlayground`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DittoMode {
    OnlinePlayground,
    Offline,
}

pub fn select_mode(offline_license_token: &str) -> DittoMode {
    if offline_license_token.trim().is_empty() {
        DittoMode::OnlinePlayground
    } else {
        DittoMode::Offline
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_token_selects_online() {
        assert_eq!(DittoMode::OnlinePlayground, select_mode(""));
    }

    #[test]
    fn whitespace_only_token_selects_online() {
        assert_eq!(DittoMode::OnlinePlayground, select_mode("   \t\n  "));
    }

    #[test]
    fn non_empty_token_selects_offline() {
        assert_eq!(DittoMode::Offline, select_mode("any-real-license-token"));
    }
}

/// Load .env file from git repo root rather than `rust/`
fn try_init_dotenv() -> Result<()> {
    let git_toplevel_output = std::process::Command::new("git")
        .args(["rev-parse", "--show-toplevel"])
        .output()
        .context("failed to exec 'git rev-parse --show-toplevel'")?;
    let path = String::from_utf8(git_toplevel_output.stdout)?;
    let path = std::path::Path::new(path.trim());
    let path = path.join(".env");
    dotenvy::from_path(&path)?;
    Ok(())
}
