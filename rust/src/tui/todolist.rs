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
use std::ops::Not;
use std::sync::Arc;
use tokio::sync::watch;
use uuid::Uuid;

use crate::key;

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
    pub tasks_subscription: Arc<SyncSubscription>,

    // TUI state below
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
        let tasks_subscription = ditto
            .sync()
            .register_subscription("SELECT * FROM tasks", None)?;
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

        let table = Table::new(rows, Constraint::from_percentages([30, 70]))
            .header(header)
            .highlight_symbol("❯❯ ")
            .highlight_style(Style::new().bold().blue())
            .block(
                Block::bordered()
                    .border_type(BorderType::Rounded)
                    .title_bottom(" j↓ k↑ (Enter: toggle done) (c: create) (d: delete) (q: quit) "),
            );
        StatefulWidget::render(table, area, buf, &mut self.table_state);
    }

    /// Render "new todo" prompt if `create_task_title` is "Some"
    fn render_new_todo_prompt(&self, area: Rect, buf: &mut Buffer) {
        let Some(title) = self.active_buffer() else {
            return;
        };

        let space = area.inner(Margin::new(2, 2));
        Clear.render(space, buf);
        Block::bordered()
            .border_type(BorderType::Rounded)
            .title(" New Todo ")
            .padding(Padding::uniform(1))
            .render(space, buf);
        let space = space.inner(Margin::new(2, 2));
        Line::raw(title).render(space, buf);
    }

    /// Whether either the create or edit buffers is open
    fn is_popup_open(&self) -> bool {
        self.create_task_title.is_some() || self.edit_task.is_some()
    }

    /// Provide a mutable reference to an open buffer, if one exists
    fn active_buffer_mut(&mut self) -> Option<&mut String> {
        if !self.is_popup_open() {
            return None;
        }

        if let Some(buffer) = self.create_task_title.as_mut() {
            return Some(buffer);
        }

        if let Some((_id, buffer)) = self.edit_task.as_mut() {
            return Some(buffer);
        }

        None
    }

    /// Provide a reference to an open buffer, if one exists
    fn active_buffer(&self) -> Option<&String> {
        if !self.is_popup_open() {
            return None;
        }

        if let Some(buffer) = self.create_task_title.as_ref() {
            return Some(buffer);
        }

        if let Some((_id, buffer)) = self.edit_task.as_ref() {
            return Some(buffer);
        }

        None
    }

    /// Apply a terminal event to update the todolist state
    pub async fn try_handle_event(&mut self, event: &Event) -> Result<()> {
        match event {
            // Esc in popup closes popup
            key!(Esc) if self.is_popup_open() => {
                self.create_task_title = None;
                self.edit_task = None;
            }
            // Scroll up
            key!(Up) | key!(Char('k')) if self.is_popup_open().not() => {
                self.table_state.select_previous();
            }
            // Scroll down
            key!(Down) | key!(Char('j')) if self.is_popup_open().not() => {
                self.table_state.select_next();
            }
            // c for Create
            key!(Char('c')) if self.is_popup_open().not() => {
                // Enter "create todo" state by setting Some
                self.create_task_title = Some("".to_string());
            }
            // d for Delete
            key!(Char('d')) if self.is_popup_open().not() => {
                self.try_delete_task().await?;
            }
            // e for Edit
            key!(Char('e')) if self.is_popup_open().not() => {
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
                self.edit_task = Some((item.id.clone(), item.title.to_string()));
            }
            // Typing into open buffer (edit or create)
            key!(Char(ch)) if self.is_popup_open() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_popup_open
                let buffer = self.active_buffer_mut().unwrap();
                buffer.push(*ch);
            }
            // Backspace either pops from active buffer, or closes prompt
            key!(Backspace) if self.is_popup_open() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_popup_open
                let buffer = self.active_buffer_mut().unwrap();

                // Backspace on empty quits open buffers
                if buffer.is_empty() {
                    self.create_task_title = None;
                    self.edit_task = None;
                    return Ok(());
                }

                buffer.pop();
            }
            // Enter when popup is closed just toggles "done"
            key!(Enter) if self.is_popup_open().not() => {
                self.try_toggle_done().await?;
            }
            // Submit "create task"
            key!(Enter) if self.create_task_title.is_some() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_some
                let title = self.create_task_title.take().unwrap();
                self.try_create_new_todo(title).await?;
            }
            // Submit "edit task"
            key!(Enter) if self.edit_task.is_some() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_some
                let (id, title) = self.edit_task.take().unwrap();
                self.try_edit_todo(&id, &title).await?;
            }
            _ => {}
        }

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
