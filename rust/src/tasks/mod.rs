use anyhow::{Context, Result};
use ratatui::{backend::CrosstermBackend, Terminal};
use std::io::Stdout;
use tui::TuiTask;

use crate::{config::DittoConfig, Shutdown};

pub mod tui;

pub fn spawn_tasks(shutdown: Shutdown, terminal: Terminal<CrosstermBackend<Stdout>>) -> Result<()> {
    let config = DittoConfig::from_default_path().context("failed to read default config")?;

    let _tui_task = TuiTask::try_spawn(shutdown.clone(), terminal, config)
        .context("failed to start tui task")?;

    Ok(())
}
