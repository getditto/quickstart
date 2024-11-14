use std::sync::Arc;

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
use tokio::sync::watch;

use crate::key;

pub struct Todolist {
    /// Our handle to the Ditto peer, used to create observers and subscriptions
    pub ditto: Ditto,

    /// Ditto observer handles must be held (not dropped) to keep them alive
    ///
    /// Observers provide the actual callback triggers to allow handling events
    pub tasks_observer: Arc<StoreObserver>,

    /// Our observer sends any document updates into this watch channel
    pub tasks_rx: watch::Receiver<Vec<serde_json::Value>>,

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
}

impl Todolist {
    pub fn new(ditto: Ditto) -> Result<Self> {
        let (tasks_tx, tasks_rx) = watch::channel(Vec::new());
        let tasks_subscription = ditto
            .sync()
            .register_subscription("SELECT * FROM tasks", None)?;
        let tasks_observer = ditto.store().register_observer(
            "SELECT * FROM tasks ORDER BY deleted",
            None,
            move |query_result| {
                let docs = query_result
                    .into_iter()
                    .flat_map(|it| it.deserialize_value::<serde_json::Value>().ok())
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

        let header = ["Done".bold(), "Title".bold(), "Deleted".bold()]
            .into_iter()
            .map(Cell::from)
            .collect::<Row>();
        let rows = tasks
            .iter()
            .map(|doc| {
                let done = doc["done"].as_bool().unwrap_or(false);
                let done = if done { " ✅ " } else { " ☐ " };
                let title = doc["title"].as_str().unwrap_or_default();
                let deleted = doc["deleted"].as_bool().unwrap_or(false);
                let title_style = if deleted {
                    Style::new().crossed_out()
                } else {
                    Style::new()
                };
                let deleted = if deleted {
                    "true".bold().red()
                } else {
                    "false".into()
                };

                [
                    Cell::from(Text::from(done.to_string())),
                    Cell::from(Text::styled(title, title_style)),
                    Cell::from(Text::from(deleted)),
                ]
                .into_iter()
                .collect::<Row>()
            })
            .collect::<Vec<_>>();

        let table = Table::new(rows, Constraint::from_percentages([14, 53, 33]))
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
        let Some(title) = self.create_task_title.as_ref() else {
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

    /// Apply a terminal event to update the todolist state
    pub async fn try_handle_event(&mut self, event: &Event) -> Result<()> {
        match event {
            key!(Esc) if self.create_task_title.is_some() => {
                self.create_task_title = None;
            }
            key!(Up) | key!(Char('k')) if self.create_task_title.is_none() => {
                self.table_state.select_previous();
            }
            key!(Down) | key!(Char('j')) if self.create_task_title.is_none() => {
                self.table_state.select_next();
            }
            key!(Char('c')) if self.create_task_title.is_none() => {
                // Enter "create todo" state by setting Some
                self.create_task_title = Some("".to_string());
            }
            key!(Char('d')) if self.create_task_title.is_none() => {
                self.try_toggle_selected_field("deleted").await?;
            }
            key!(Char(ch)) if self.create_task_title.is_some() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_some
                let title = self.create_task_title.as_mut().unwrap();
                title.push(*ch);
            }
            key!(Backspace) if self.create_task_title.is_some() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_some
                let title = self.create_task_title.as_mut().unwrap();

                // Backspace on empty quits new todo mode
                if title.is_empty() {
                    self.create_task_title = None;
                    return Ok(());
                }

                title.pop();
            }
            key!(Enter) if self.create_task_title.is_none() => {
                self.try_toggle_selected_field("done").await?;
            }
            key!(Enter) if self.create_task_title.is_some() => {
                #[allow(clippy::unwrap_used)] // SAFETY: Checked is_some
                let title = self.create_task_title.take().unwrap();
                self.try_create_new_todo(title).await?;
            }
            _ => {}
        }

        Ok(())
    }

    /// Given the name of a boolean field (e.g. "done" or "deleted"), toggle the state of that field
    pub async fn try_toggle_selected_field(&mut self, field: &str) -> Result<()> {
        let tasks = self.tasks_rx.borrow().clone();
        let task_index = self
            .table_state
            .selected()
            .context("failed to get todolist selected index")?;
        let selected_task = tasks
            .get(task_index)
            .cloned()
            .context("failed to find selected task")?;
        let id = selected_task["_id"]
            .as_str()
            .context("failed to get document _id")?;
        let value = selected_task[field]
            .as_bool()
            .with_context(|| format!("expected '{field}' to be a bool but wasn't"))?;

        self.ditto
            .store()
            .execute(
                format!("UPDATE tasks SET {field}=:value WHERE _id=:id"),
                Some(
                    serde_json::json!({
                        "id": id,
                        "value": !value,
                    })
                    .into(),
                ),
            )
            .await?;

        Ok(())
    }

    pub async fn try_create_new_todo(&mut self, title: String) -> Result<()> {
        self.ditto
            .store()
            .execute(
                "INSERT INTO tasks DOCUMENTS (:task)",
                Some(
                    serde_json::json!({
                        "task": {
                            "title": title,
                            "done": false,
                            "deleted": false
                        }
                    })
                    .into(),
                ),
            )
            .await?;
        Ok(())
    }
}
