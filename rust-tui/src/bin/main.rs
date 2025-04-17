use std::{path::PathBuf, sync::Arc, time::Duration};

use anyhow::{anyhow, Context, Result};
use clap::Parser;
use ditto_quickstart::{term, tui::TuiTask, Shutdown};
use dittolive_ditto::{fs::TempRoot, AppId, Ditto, prelude::*};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[derive(Debug, Parser)]
pub struct Cli {
    /// The Ditto App ID this app will use to initialize Ditto
    #[clap(long, env = "DITTO_APP_ID")]
    app_id: AppId,

    /// The Online Playground token this app should use for authentication
    #[clap(long, env = "DITTO_PLAYGROUND_TOKEN")]
    token: String,

    /// The custom auth URL this app should use for authentication
    #[clap(long, env = "DITTO_AUTH_URL")]
    custom_auth_url: String,

    /// The websocket URL this app should use for authentication
    #[clap(long, env = "DITTO_WEBSOCKET_URL")]
    websocket_url: String,
    
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
        cli.websocket_url)?;
    let _tui_task = TuiTask::try_spawn(shutdown.clone(), terminal, ditto)
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

fn try_init_ditto(
    app_id: AppId, 
    token: String, 
    custom_auth_url: String, 
    websocket_url: String) -> Result<Ditto> {
    // We use a temporary directory to store Ditto's local database.  
    // This means that data will not be persistent between runs of the
    // application, but it allows us to run multiple instances of the
    // application concurrently on the same machine.  For a production
    // application, we would want to store the database in a more permanent
    // location, and if multiple instances are needed, ensure that each
    // instance has its own persistence directory.
    let ditto = Ditto::builder()
        .with_root(Arc::new(TempRoot::new()))
        .with_identity(|ditto_root| {
            let provider = "dummyProvider".to_string();
            struct EventHandler {
                token: String,
                provider: String,
            }
            impl DittoAuthenticationEventHandler for EventHandler {
                fn authentication_required(&self, auth: DittoAuthenticator) {
                    let _feedback = auth
                        .login_with_token_and_feedback(&self.token, &self.provider)
                        .unwrap();
                }

                fn authentication_expiring_soon(&self, auth: DittoAuthenticator, _: Duration) {
                    let _feedback = auth
                        .login_with_token_and_feedback(&self.token, &self.provider)
                        .unwrap();
                }
            }
            let event_handler = EventHandler {
                token,
                provider,
            };
            OnlineWithAuthentication::new(
                ditto_root,
                app_id.clone(),
                event_handler,
                true,
                Some(&custom_auth_url),
            )
        })?
        .build()?;

    ditto.update_transport_config(|config| {
        config.enable_all_peer_to_peer();
        //set websocket url
        config.connect.websocket_urls.insert(websocket_url);
    });

    // Disable sync with v3 peers, required for syncing with the Ditto Cloud (BigPeer)
    _ = ditto.disable_sync_with_v3();

    // Start sync
    _ = ditto.start_sync();

    tracing::info!(%app_id, "Started Ditto!");
    Ok(ditto)
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
