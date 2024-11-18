use anyhow::{anyhow, Context, Result};
use crossterm::event::{Event, EventStream};
use dittolive_ditto::prelude::*;
use futures::{FutureExt, Stream, StreamExt};
use ratatui::prelude::*;
use std::{io::Stdout, ops::ControlFlow, time::Duration};
use todolist::Todolist;
use tokio::task::JoinHandle;

use crate::{should_quit, Shutdown};

pub mod todolist;

/// External handle for callers to interact with the tui task
pub struct TuiTask {
    /// Tokio handle for task shutdown
    pub tokio_handle: JoinHandle<()>,
}

impl TuiTask {
    /// Entrypoint to the tui task. This will spawn the application on a
    /// tokio task and return a [`TuiTask`] handle for interacting with it
    pub fn try_spawn(
        shutdown: Shutdown,
        terminal: Terminal<CrosstermBackend<Stdout>>,
        ditto: Ditto,
    ) -> Result<TuiTask> {
        let todolist_state = Todolist::new(ditto)?;
        let task_context = TuiContext {
            terminal,
            shutdown,
            todolist: todolist_state,
        };
        let task_shutdown = task_context.shutdown.clone();
        let task_future = task_context.run();
        let task_future = task_shutdown.wrap_trigger_shutdown(
            anyhow!("Tui run loop quit unexpectedly").into(),
            task_future,
        );
        let tokio_handle = tokio::spawn(task_future);
        Ok(TuiTask { tokio_handle })
    }
}

/// The set of input events that the Tui app can react to
pub enum TuiEvent {
    /// One tick of our framerate has passed, we should redraw
    FrameTick,

    /// An input was received from the terminal
    Terminal(std::io::Result<Event>),

    /// Shutdown event received, need to cleanup and quit
    Shutdown,
}

/// Internal state and resources for the tui app
pub struct TuiContext {
    /// Crossterm terminal handle
    terminal: Terminal<CrosstermBackend<Stdout>>,

    /// Task-local shutdown
    shutdown: Shutdown,

    /// Ratatui todolist app state
    todolist: Todolist,
}

impl TuiContext {
    async fn run(mut self) {
        loop {
            let future = async {
                let mut stream = self
                    .try_create_stream()
                    .await
                    .context("failed to create tui stream")?;
                let flow = self.try_run(&mut stream).await?;
                Ok::<_, anyhow::Error>(flow)
            };

            let result = future.await;
            match result {
                Ok(ControlFlow::Break(())) => {
                    return;
                }
                Err(error) => {
                    tracing::error!(%error, "Error in tui loop, continuing");
                }
                _ => {} // Continue
            }
        }
    }

    async fn try_run(
        &mut self,
        input_stream: &mut (impl Stream<Item = TuiEvent> + Unpin),
    ) -> Result<ControlFlow<()>> {
        loop {
            let input = input_stream
                .next()
                .await
                .context("tui input stream ended unexpectedly")?;

            let flow = self.try_handle_event(input).await?;
            if flow.is_break() {
                // Graceful shutdown
                return Ok(ControlFlow::Break(()));
            }

            self.terminal
                .draw(|f| {
                    self.todolist.render(f.area(), f.buffer_mut());
                })
                .context("failed to draw tui frame")?;
        }
    }

    async fn try_handle_event(&mut self, input_event: TuiEvent) -> Result<ControlFlow<()>> {
        match input_event {
            TuiEvent::FrameTick => {
                // Fall through to draw
            }
            TuiEvent::Terminal(result) => {
                let event = result.context("terminal input error")?;

                if should_quit(&event) {
                    self.shutdown
                        .trigger_shutdown(anyhow!("Pressed q!").into())?;
                    return Ok(ControlFlow::Break(()));
                }

                self.todolist.try_handle_event(&event).await?;
            }
            TuiEvent::Shutdown => {
                return Ok(ControlFlow::Break(()));
            }
        }

        Ok(ControlFlow::Continue(()))
    }

    /// Construct an async Stream that yields events relevant to our application
    ///
    /// This is done by constructing individual event streams and merging them
    /// together into a stream of `TuiEvent`s.
    async fn try_create_stream(&self) -> Result<impl Stream<Item = TuiEvent> + 'static> {
        use futures_concurrency::prelude::*;

        let shutdown_stream = self
            .shutdown
            .wait_shutdown_triggered()
            .into_stream()
            .map(|_| TuiEvent::Shutdown);
        let term_stream = EventStream::new().map(TuiEvent::Terminal);
        let framerate = tokio_stream::wrappers::IntervalStream::new(tokio::time::interval(
            Duration::from_millis(20),
        ))
        .map(|_| TuiEvent::FrameTick);

        let merged_stream = (shutdown_stream, term_stream, framerate).merge();
        Ok(merged_stream)
    }
}
