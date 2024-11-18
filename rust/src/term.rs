//! Handy terminal initialization and drop tool
//!
//! Credit to [`hawkw`][0].
//!
//! [0]: https://github.com/tokio-rs/console/blob/cbf6f56a16036ecf13548c4209fcc62f8a84bae2/tokio-console/src/term.rs

use anyhow::{Context, Result};
pub use ratatui::{backend::CrosstermBackend, Terminal};
use std::io;

pub fn init_crossterm() -> Result<(Terminal<CrosstermBackend<io::Stdout>>, OnShutdown)> {
    use crossterm::terminal::{self, EnterAlternateScreen};
    terminal::enable_raw_mode().context("Failed to enable crossterm raw mode")?;

    let mut stdout = std::io::stdout();
    crossterm::execute!(stdout, EnterAlternateScreen)
        .context("Failed to enable crossterm alternate screen")?;
    let backend = CrosstermBackend::new(io::stdout());
    let term = Terminal::new(backend).context("Failed to create crossterm terminal")?;

    let cleanup = OnShutdown::new(exit_crossterm);

    // Setup panic handler to restore terminal
    let original_hook = std::panic::take_hook();
    std::panic::set_hook(Box::new(move |panic_info| {
        _ = exit_crossterm();
        original_hook(panic_info);
    }));

    Ok((term, cleanup))
}

fn exit_crossterm() -> Result<()> {
    use crossterm::terminal::{self, LeaveAlternateScreen};
    // Being a good terminal citizen...
    let mut stdout = std::io::stdout();
    crossterm::execute!(stdout, LeaveAlternateScreen)
        .context("Failed to disable crossterm alternate screen")?;
    terminal::disable_raw_mode().context("Failed to enable crossterm raw mode")?;
    Ok(())
}

pub struct OnShutdown {
    action: fn() -> Result<()>,
}

impl OnShutdown {
    fn new(action: fn() -> Result<()>) -> Self {
        Self { action }
    }
}

impl Drop for OnShutdown {
    fn drop(&mut self) {
        if let Err(error) = (self.action)() {
            tracing::error!(%error, "error running terminal cleanup");
        }
    }
}
