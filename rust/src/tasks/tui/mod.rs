use anyhow::{anyhow, Context, Error, Result};
use crossterm::event::EventStream;
use dittolive_ditto::prelude::*;
use fehler::throws;
use futures::{Stream, StreamExt};
use indexmap::IndexMap;
use profile_container::{ProfileListItem, ProfilesContainer, ProfilesState};
use ratatui::{
    prelude::*,
    widgets::{Block, BorderType},
};
use std::{io::Stdout, ops::ControlFlow, sync::Arc, time::Duration};
use todolist_container::{TodolistContainer, TodolistState};
use tokio::task::JoinHandle;

use crate::{
    config::{DittoConfig, Identity, Profile},
    input::{
        self,
        Event::{self},
    },
    Shutdown,
};

pub mod profile_container;
pub mod todolist_container;

/// External handle for callers to interact with the tui task
pub struct TuiTask {
    /// Sender to inject actions to the Tui task
    pub actions_tx: flume::Sender<TuiInput>,

    /// Tokio handle for task shutdown
    pub tokio_handle: JoinHandle<()>,
}

impl TuiTask {
    #[throws]
    pub fn try_spawn(
        shutdown: Shutdown,
        terminal: Terminal<CrosstermBackend<Stdout>>,
        config: DittoConfig,
    ) -> TuiTask {
        let mut root_state = RootContainerState::new();
        root_state.profiles_state.profiles_list_state.items = config
            .profiles
            .iter()
            .map(|(_name, profile)| ProfileListItem::from_profile(profile))
            .collect();
        let (actions_tx, actions_rx) = flume::bounded(100);
        let task_context = TuiContext {
            terminal,
            shutdown,
            root_state,
            actions_tx: actions_tx.clone(),
            actions_rx,
            config,
        };
        let task_shutdown = task_context.shutdown.clone();
        let task_future = task_context.run();
        let task_future = task_shutdown.wrap_trigger_shutdown(
            anyhow!("Tui run loop quit unexpectedly").into(),
            task_future,
        );
        let tokio_handle = tokio::spawn(task_future);
        TuiTask {
            actions_tx,
            tokio_handle,
        }
    }
}

/// The set of input events that the Tui app can react to
pub enum TuiInput {
    /// One tick of our framerate has passed, we should redraw
    FrameTick,

    /// An input was received from the terminal
    Terminal(std::io::Result<Event>),

    /// Select a profile and launch a Ditto peer
    EnterProfile(Profile),

    /// Navigate up and out of a Ditto app's profile
    ExitProfile,
}

pub struct TuiActions<'a> {
    tx: &'a flume::Sender<TuiInput>,
}

impl<'a> TuiActions<'a> {
    pub fn new(tx: &'a flume::Sender<TuiInput>) -> Self {
        Self { tx }
    }

    pub fn enter_profile(&self, profile: Profile) {
        self.tx
            .send(TuiInput::EnterProfile(profile))
            .expect("enter_profile");
    }

    pub fn exit_profile(&self) {
        self.tx.send(TuiInput::ExitProfile).expect("exit_profile");
    }
}

/// Internal state and resources for the tui app
pub struct TuiContext {
    /// Crossterm terminal handle
    terminal: Terminal<CrosstermBackend<Stdout>>,

    /// Task-local shutdown
    shutdown: Shutdown,

    /// Ratatui app state
    root_state: RootContainerState,

    /// Channel for sending app events
    actions_tx: flume::Sender<TuiInput>,

    /// Channel for receiving app events
    actions_rx: flume::Receiver<TuiInput>,

    /// Ditto profiles config
    config: DittoConfig,
}

impl TuiContext {
    async fn run(mut self) {
        loop {
            let future = async {
                let mut stream = self
                    .try_create_stream()
                    .await
                    .context("failed to create tui stream")?;
                self.try_run(&mut stream).await?;
                Ok::<_, anyhow::Error>(())
            };

            let result = future.await;
            if let Err(error) = result {
                tracing::error!(%error, "Error in tui loop, continuing");
            }
        }
    }

    async fn try_run(
        &mut self,
        input_stream: &mut (impl Stream<Item = TuiInput> + Unpin),
    ) -> Result<()> {
        loop {
            let input = input_stream
                .next()
                .await
                .context("tui input stream ended unexpectedly")?;

            let flow = self.try_handle_event(input).await?;
            if flow.is_break() {
                // Return Ok(()) means graceful shutdown
                return Ok(());
            }

            self.terminal
                .draw(|f| {
                    RootContainer::new().render(f.area(), f.buffer_mut(), &mut self.root_state);
                })
                .context("failed to draw tui frame")?;
        }
    }

    async fn try_handle_event(&mut self, input_event: TuiInput) -> Result<ControlFlow<()>> {
        match input_event {
            TuiInput::FrameTick => {
                // Fall through to draw
            }
            TuiInput::Terminal(result) => {
                let event = result.context("terminal input error")?;

                if input::should_quit(&event) {
                    self.shutdown
                        .trigger_shutdown(anyhow!("Pressed q!").into())?;
                    return Ok(ControlFlow::Break(()));
                }

                tracing::debug!("Received {event:?}");
                let actions = TuiActions::new(&self.actions_tx);
                self.root_state.try_handle_event(actions, &event).await?;
            }
            TuiInput::EnterProfile(profile) => {
                let app_id = profile.app_id.parse::<AppId>()?;
                let app_id_string = profile.app_id.to_string();

                let app_exists = self
                    .root_state
                    .ditto_app_states
                    .contains_key(&app_id_string);
                if app_exists {
                    self.root_state.active_ditto_app = Some(app_id_string);
                    self.root_state.focus = RootContainerFocus::TodoList;
                    return Ok(ControlFlow::Continue(()));
                }

                let root: Arc<dyn DittoRoot> = match &profile.root {
                    None => Arc::new(TempRoot::new()),
                    Some(root) => Arc::new(PersistentRoot::new(root)?),
                };
                let Identity::OnlinePlayground(crate::config::OnlinePlayground { token }) =
                    &profile.identity
                else {
                    panic!("Wrong identity");
                };

                let ditto = Ditto::builder()
                    .with_root(root)
                    .with_identity(|root| {
                        OnlinePlayground::new(root, app_id, token.to_string(), true, None)
                    })?
                    .build()?;
                _ = ditto.disable_sync_with_v3();
                _ = ditto.start_sync();
                tracing::info!(app_id=%app_id_string, "Started Ditto!");

                let app_state = TodolistState::new(profile, ditto)?;
                self.root_state
                    .ditto_app_states
                    .insert(app_id_string.to_string(), app_state);

                self.root_state.active_ditto_app = Some(app_id_string);
                self.root_state.focus = RootContainerFocus::TodoList;
            }
            TuiInput::ExitProfile => {
                self.root_state.focus = RootContainerFocus::Profiles;
            }
        }

        Ok(ControlFlow::Continue(()))
    }

    async fn try_create_stream(&self) -> Result<impl Stream<Item = TuiInput> + 'static> {
        use futures_concurrency::prelude::*;

        let term_stream = EventStream::new().map(TuiInput::Terminal);
        let framerate = tokio_stream::wrappers::IntervalStream::new(tokio::time::interval(
            Duration::from_millis(20),
        ))
        .map(|_| TuiInput::FrameTick);

        // NOTE: Avoid cloning this RX elsewhere
        let actions_stream = self.actions_rx.clone().into_stream();

        let merged_stream = (term_stream, framerate, actions_stream).merge();
        Ok(merged_stream)
    }
}

pub struct RootContainer {}

pub struct RootContainerState {
    focus: RootContainerFocus,
    profiles_state: ProfilesState,

    /// Map of Ditto App states, indexed by AppID
    ditto_app_states: IndexMap<String, TodolistState>,
    active_ditto_app: Option<String>,
}

pub enum RootContainerFocus {
    Profiles,
    TodoList,
}

impl RootContainer {
    pub fn new() -> Self {
        Self {}
    }
}

impl RootContainerState {
    pub fn new() -> Self {
        RootContainerState {
            focus: RootContainerFocus::Profiles,
            profiles_state: ProfilesState::new(),

            ditto_app_states: Default::default(),
            active_ditto_app: None,
        }
    }

    fn active_ditto_app(&self) -> Option<&TodolistState> {
        let active_app = self.active_ditto_app.as_deref()?;
        let state = self.ditto_app_states.get(active_app)?;
        Some(state)
    }

    fn active_ditto_app_mut(&mut self) -> Option<&mut TodolistState> {
        let active_app = self.active_ditto_app.as_deref()?;
        let state = self.ditto_app_states.get_mut(active_app)?;
        Some(state)
    }

    async fn try_handle_event(&mut self, actions: TuiActions<'_>, event: &Event) -> Result<()> {
        match self.focus {
            RootContainerFocus::Profiles => {
                self.profiles_state.handle_event(actions, event);
            }
            RootContainerFocus::TodoList => {
                let maybe_app_state = self.active_ditto_app_mut();
                if let Some(app_state) = maybe_app_state {
                    app_state.try_handle_event(actions, event).await?;
                }
            }
        }

        Ok(())
    }

    pub fn breadcrumb(&self) -> Vec<Span<'_>> {
        let mut crumbs = vec![" ❯❯ ".bold().blue()];

        match self.focus {
            RootContainerFocus::Profiles => {
                crumbs.extend(self.profiles_state.breadcrumbs());
            }
            RootContainerFocus::TodoList => {
                let maybe_app_state = self.active_ditto_app();
                if let Some(todo_state) = maybe_app_state {
                    crumbs.extend(todo_state.breadcrumbs());
                }
            }
        }

        crumbs
    }
}

impl StatefulWidget for RootContainer {
    type State = RootContainerState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        Block::bordered()
            .border_type(BorderType::Rounded)
            .title(state.breadcrumb())
            .title_bottom(" <Esc> j↓ k↑ <Enter> ")
            .render(area, buf);
        let body = area.inner(Margin::new(1, 1));

        match state.focus {
            RootContainerFocus::Profiles => {
                ProfilesContainer::new().render(body, buf, &mut state.profiles_state);
            }
            RootContainerFocus::TodoList => {
                let Some(browser_state) = state.active_ditto_app_mut() else {
                    return;
                };
                TodolistContainer::new().render(body, buf, browser_state);
            }
        }
    }
}
