use std::{path::PathBuf, sync::Arc, time::Duration};

use anyhow::{Context, Result, anyhow};
use clap::Parser;
use ditto_quickstart::{Shutdown, ditto_manager::DittoManager, term, tui::TuiTask};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[derive(Debug, Parser)]
pub struct Cli {
    /// The Ditto database ID this app will use to initialize Ditto
    #[clap(long, env = "DITTO_DATABASE_ID")]
    database_id: String,

    /// The Development token this app should use for authentication
    #[clap(long, env = "DITTO_DEVELOPMENT_TOKEN", default_value = "")]
    token: String,

    /// The Ditto server URL this app connects to
    #[clap(long, env = "DITTO_SERVER_URL", default_value = "")]
    server_url: String,

    /// Optional offline-only license token. When set, server credentials are not used.
    #[clap(long, env = "DITTO_OFFLINE_LICENSE_TOKEN", default_value = "")]
    offline_license_token: String,

    /// Optional client name to display in the TUI
    #[clap(long, env = "DITTO_CLIENT_NAME")]
    client_name: Option<String>,

    /// Enable peer-to-peer transports (LAN, Bluetooth). Set to false to force all communication
    /// through Big Peer.
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

    // Initialize and launch app. `DittoManager` owns the Ditto instance and its
    // persistence directory; we share it via `Arc` so both the repository and
    // the TUI can hold it. We start sync here, before spawning the TUI.
    let manager = Arc::new(DittoManager::try_new(
        cli.database_id,
        cli.token,
        cli.server_url,
        cli.offline_license_token,
        cli.p2p_enabled,
    )?);
    manager.start_sync()?;
    let _tui_task = TuiTask::try_spawn(shutdown.clone(), terminal, manager, cli.client_name)
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

/// Load the shared quickstart `.env` file (one directory above this crate).
///
/// `CARGO_MANIFEST_DIR` is the `rust-tui/` crate directory; joining `../.env`
/// resolves to the quickstart configuration file shared by all the sample
/// apps, so every app reads the same credentials from a single `.env`.
///
/// If that file is absent, fall back to `dotenvy::dotenv()` so the app still
/// works when credentials are already in the environment.
fn try_init_dotenv() -> Result<()> {
    let manifest_dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR"));
    let env_path = manifest_dir.join("../.env");
    if env_path.exists() {
        dotenvy::from_path(&env_path)?;
    } else {
        dotenvy::dotenv().ok();
    }
    Ok(())
}
