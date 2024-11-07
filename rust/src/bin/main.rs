use std::time::Duration;

use anyhow::{anyhow, bail, Context as _, Result};
use ditto_quickstart::{tasks, term, Shutdown};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt, EnvFilter, Layer};

#[tokio::main]
async fn main() -> Result<()> {
    let shutdown = <Shutdown>::new();
    let (terminal, _cleanup) = term::init_crossterm()?;

    let tmpfile_writer = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open("/tmp/ditto-quickstart.log")
        .context("failed to open /tmp/ditto-quickstart.log for logging")?;
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::fmt::layer()
                .with_writer(tmpfile_writer)
                .with_filter(EnvFilter::from_default_env()),
        )
        .try_init()
        .context("failed to initialize tracing-subscriber")?;

    tasks::spawn_tasks(shutdown.clone(), terminal).context("failed to launch app tasks")?;

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
    tokio::select! {
        reason = shutdown.wait_shutdown_complete() => {
            tracing::info!(%reason, "[SHUTDOWN] Shutdown complete, quitting!");
        }
        _ = tokio::time::sleep(Duration::from_secs(5)) => {
            tracing::warn!("[SHUTDOWN] Failed to cleanup within timeout, force-quitting!");
            bail!("ditto-tui force-quit after cleanup timeout");
        }
    }

    Ok(())
}
