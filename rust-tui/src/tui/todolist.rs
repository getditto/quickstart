use anyhow::Context;
use anyhow::Result;
use crossterm::event::Event;
use dittolive_ditto::store::StoreObserver;
use dittolive_ditto::sync::SyncSubscription;
use dittolive_ditto::Ditto;
use ratatui::prelude::*;
use ratatui::widgets::Block;
use ratatui::widgets::BorderType;
use ratatui::widgets::Clear;
use ratatui::widgets::Padding;
use ratatui::widgets::{Cell, Row, StatefulWidget, Table, TableState};
use serde::Deserialize;
use serde::Serialize;
use std::sync::Arc;
use tokio::sync::watch;
use uuid::Uuid;

use crate::key;

use super::EventResult;

pub struct Todolist {
    /// Our handle to the Ditto peer, used to create observers and subscriptions
    pub ditto: Ditto,

    /// Ditto observer handles must be held (not dropped) to keep them alive
    ///
    /// Observers provide the actual callback triggers to allow handling events
    pub tasks_observer: Arc<StoreObserver>,

    /// Our observer sends any document updates into this watch channel
    pub tasks_rx: watch::Receiver<Vec<TodoItem>>,

    /// Ditto subscriptions must also be held to keep them alive
    ///
    /// Subscriptions cause Ditto to sync selected data from other peers
    pub tasks_subscription: Option<Arc<SyncSubscription>>,

    // TUI state below
    pub mode: TodoMode,

    /// Table scrolling state
    pub table_state: TableState,

    /// Holds the contents of a "new todo" dialog
    ///
    /// When this is "None", the dialog is closed. When "Some", it contains
    /// the title being typed by the user.
    pub create_task_title: Option<String>,

    /// Holds the contents of an existing TODO title to be edited
    pub edit_task: Option<(String, String)>, // (ID, title)
}

/// Mode enum used to decide how to interpret keystrokes
#[derive(Debug)]
pub enum TodoMode {
    Normal,
    CreateTask { buffer: String },
    EditTask { id: String, buffer: String },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TodoItem {
    #[serde(rename = "_id")]
    id: String,
    title: String,
    done: bool,
    deleted: bool,
}

impl TodoItem {
    pub fn new(title: String) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            title,
            done: false,
            deleted: false,
        }
    }
}

impl Todolist {
    pub fn new(ditto: Ditto) -> Result<Self> {
        let (tasks_tx, tasks_rx) = watch::channel(Vec::new());

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        let tasks_subscription = ditto
            .sync()
            .register_subscription("SELECT * FROM tasks", None)?;
        let tasks_subscription = Some(tasks_subscription);

        // register observer for live query
        // Register observer, which runs against the local database on this peer
        let tasks_observer = ditto.store().register_observer(
            "SELECT * FROM tasks WHERE deleted=false ORDER BY _id",
            None,
            move |query_result| {
                let docs = query_result
                    .into_iter()
                    .flat_map(|it| it.deserialize_value::<TodoItem>().ok())
                    .collect::<Vec<_>>();
                tasks_tx.send_replace(docs);
            },
        )?;

        Ok(Self {
            ditto,
            table_state: Default::default(),
            tasks_rx,
            tasks_observer,
            tasks_subscription,
            mode: TodoMode::Normal,
            create_task_title: None,
            edit_task: None,
        })
    }

    /// Top-level render function for the Todolist
    pub fn render(&mut self, area: Rect, buf: &mut Buffer) {
        self.render_todo_table(area, buf);
        self.render_new_todo_prompt(area, buf);
    }

    /// Render a table displaying each todo and its current status
    fn render_todo_table(&mut self, area: Rect, buf: &mut Buffer) {
        let tasks = self.tasks_rx.borrow().clone();

        let header = ["Done".bold(), "Title".bold()]
            .into_iter()
            .map(Cell::from)
            .collect::<Row>();
        let rows = tasks
            .iter()
            .map(|doc| {
                let done = doc.done;
                let done = if done { " ✅ " } else { " ☐ " };
                let title = &doc.title;

                [
                    Cell::from(Text::from(done.to_string())),
                    Cell::from(Text::raw(title)),
                ]
                .into_iter()
                .collect::<Row>()
            })
            .collect::<Vec<_>>();

        let sync_state = if self.tasks_subscription.is_some() {
            " 🟢 Sync Active ".green()
        } else {
            " 🔴 Sync Inactive ".red()
        };
        let sync_line = [sync_state, "(s: toggle sync) ".into()]
            .into_iter()
            .collect::<Line>();

        let table = Table::new(rows, Constraint::from_percentages([30, 70]))
            .header(header)
            .highlight_symbol("❯❯ ")
            .row_highlight_style(Style::new().bold().blue())
            .block(
                Block::bordered()
                    .border_type(BorderType::Rounded)
                    .title_top(Line::raw(" Tasks (j↓, k↑, ⏎ toggle done) ").left_aligned())
                    .title_top(sync_line.right_aligned())
                    .title_bottom(" (c: create) (d: delete) (e: edit) (q: quit) "),
            );
        StatefulWidget::render(table, area, buf, &mut self.table_state);
    }

    /// Render "new todo" prompt if `create_task_title` is "Some"
    fn render_new_todo_prompt(&self, area: Rect, buf: &mut Buffer) {
        let title = match &self.mode {
            TodoMode::CreateTask { buffer } => buffer,
            TodoMode::EditTask { buffer, .. } => buffer,
            _ => {
                return;
            }
        };

        let space = area.inner(Margin::new(2, 2));
        Clear.render(space, buf);
        Block::bordered()
            .border_type(BorderType::Rounded)
            .title(" New Todo ")
            .title_bottom(" (Esc: back) ")
            .padding(Padding::uniform(1))
            .render(space, buf);
        let space = space.inner(Margin::new(2, 2));
        Line::raw(title).render(space, buf);
    }

    /// Apply a terminal event to update the todolist state
    pub async fn try_handle_event(&mut self, event: &Event) -> Result<EventResult> {
        match (&mut self.mode, event) {
            // Normal:c -> Goto create mode
            (TodoMode::Normal, key!(Char('c'))) => {
                self.mode = TodoMode::CreateTask {
                    buffer: String::new(),
                };
            }
            // Normal:d -> Delete task
            (TodoMode::Normal, key!(Char('d'))) => {
                self.try_delete_task().await?;
            }
            // Normal:e -> Goto edit mode
            (TodoMode::Normal, key!(Char('e'))) => {
                let selected = self
                    .table_state
                    .selected()
                    .context("failed to get selected index")?;
                let item = self
                    .tasks_rx
                    .borrow()
                    .get(selected)
                    .cloned()
                    .context("failed to get todo from list")?;
                self.mode = TodoMode::EditTask {
                    id: item.id.to_string(),
                    buffer: item.title.to_string(),
                };
            }
            (TodoMode::Normal, key!(Char('s'))) => {
                self.toggle_sync()?;
            }
            // Non-Normal:Esc -> Normal
            (TodoMode::CreateTask { .. } | TodoMode::EditTask { .. }, key!(Esc)) => {
                self.mode = TodoMode::Normal;
            }
            // Scroll up
            (TodoMode::Normal, key!(Up) | key!(Char('k'))) => {
                self.table_state.select_previous();
            }
            // Scroll down
            (TodoMode::Normal, key!(Down) | key!(Char('j'))) => {
                self.table_state.select_next();
            }
            // Toggle done
            (TodoMode::Normal, key!(Enter)) => {
                self.try_toggle_done().await?;
            }
            // Create task typing
            (TodoMode::CreateTask { buffer }, key!(Char(ch))) => {
                buffer.push(*ch);
            }
            // Submit create task
            (TodoMode::CreateTask { buffer }, key!(Enter)) => {
                if !buffer.is_empty() {
                    let title = std::mem::take(buffer);
                    self.try_create_new_todo(title).await?;
                    self.mode = TodoMode::Normal;
                }
            }
            // Submit edit task
            (TodoMode::EditTask { id, buffer }, key!(Enter)) => {
                if !buffer.is_empty() {
                    let title = std::mem::take(buffer);
                    let id = id.clone();
                    self.try_edit_todo(&id, &title).await?;
                    self.mode = TodoMode::Normal;
                }
            }
            // Edit task typing
            (TodoMode::EditTask { buffer, .. }, key!(Char(ch))) => {
                buffer.push(*ch);
            }
            // Backspace
            (
                TodoMode::CreateTask { buffer } | TodoMode::EditTask { buffer, .. },
                key!(Backspace),
            ) => {
                if buffer.is_empty() {
                    self.mode = TodoMode::Normal;
                } else {
                    buffer.pop();
                }
            }
            _ => {
                return Ok(EventResult::Ignored);
            }
        }

        Ok(EventResult::Consumed)
    }

    fn toggle_sync(&mut self) -> Result<()> {
        self.tasks_subscription = match &self.tasks_subscription {
            Some(subscription) => {
                subscription.cancel();
                None
            }
            None => {
                let sub = self
                    .ditto
                    .sync()
                    .register_subscription("SELECT * FROM tasks", None)?;
                Some(sub)
            }
        };
        Ok(())
    }

    /// Toggle "done" for the currently selected item in the list
    async fn try_toggle_done(&self) -> Result<()> {
        let tasks = self.tasks_rx.borrow().clone();
        let task_index = self
            .table_state
            .selected()
            .context("failed to get todolist selected index")?;
        let selected_task = tasks
            .get(task_index)
            .cloned()
            .context("failed to find selected task")?;

        let id = selected_task.id.to_string();
        let done = selected_task.done;
        self.ditto
            .store()
            .execute(
                "UPDATE tasks SET done=:done WHERE _id=:id",
                Some(
                    serde_json::json!({
                        "id": id,
                        "done": !done,
                    })
                    .into(),
                ),
            )
            .await?;

        Ok(())
    }

    /// Delete the task item currently selected in the list
    pub async fn try_delete_task(&mut self) -> Result<()> {
        let tasks = self.tasks_rx.borrow().clone();
        let task_index = self
            .table_state
            .selected()
            .context("failed to get todolist selected index")?;
        let selected_task = tasks
            .get(task_index)
            .cloned()
            .context("failed to find selected task")?;

        let id = selected_task.id;
        self.ditto
            .store()
            .execute(
                "UPDATE tasks SET deleted=true WHERE _id=:id",
                Some(serde_json::json!({"id": id}).into()),
            )
            .await?;

        Ok(())
    }

    /// Create a new task todo with the given title
    pub async fn try_create_new_todo(&mut self, title: String) -> Result<()> {
        let task = TodoItem::new(title);
        self.ditto
            .store()
            .execute(
                "INSERT INTO tasks DOCUMENTS (:task)",
                Some(serde_json::json!({"task": task}).into()),
            )
            .await?;
        Ok(())
    }

    /// Set the title of the task with the given ID
    pub async fn try_edit_todo(&mut self, id: &str, title: &str) -> Result<()> {
        self.ditto
            .store()
            .execute(
                "UPDATE tasks SET title=:title WHERE _id=:id",
                Some(serde_json::json!({ "title": title, "id": id }).into()),
            )
            .await?;

        Ok(())
    }
}
