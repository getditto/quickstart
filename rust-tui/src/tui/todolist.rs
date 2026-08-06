use anyhow::{Context, Result};
use crossterm::event::Event;
use ratatui::{
    prelude::*,
    widgets::{Block, BorderType, Cell, Clear, Padding, Row, StatefulWidget, Table, TableState},
};

use super::EventResult;
use crate::{ditto_manager::DittoManager, key, tasks_repository::TasksRepository};

pub struct Todolist {
    /// Owns the tasks data and CRUD against Ditto.
    ///
    /// Declared before `manager` so that, on drop, its `Arc<Ditto>` (and the
    /// observer/subscription) are released before the manager drops the last
    /// `Arc<Ditto>` and deletes the persistence directory.
    repository: TasksRepository,

    /// Owns and configures the Ditto instance and controls sync.
    manager: DittoManager,

    /// Optional client name for display purposes
    pub client_name: Option<String>,

    // TUI state below
    pub mode: TodoMode,

    /// Table scrolling state
    pub table_state: TableState,
}

/// Mode enum used to decide how to interpret keystrokes
#[derive(Debug)]
pub enum TodoMode {
    Normal,
    CreateTask { buffer: String },
    EditTask { id: String, buffer: String },
}

impl Todolist {
    pub fn new(manager: DittoManager, client_name: Option<String>) -> Result<Self> {
        let repository = TasksRepository::try_new(manager.ditto())?;

        Ok(Self {
            repository,
            manager,
            table_state: Default::default(),
            client_name,
            mode: TodoMode::Normal,
        })
    }

    /// Top-level render function for the Todolist
    pub fn render(&mut self, area: Rect, buf: &mut Buffer) {
        self.render_todo_table(area, buf);
        self.render_todo_prompt(area, buf);
    }

    /// Render a table displaying each todo and its current status
    fn render_todo_table(&mut self, area: Rect, buf: &mut Buffer) {
        let tasks = self.repository.tasks();

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

        let sync_state = if self.manager.is_sync_active() {
            " 🟢 Sync Active ".green()
        } else {
            " 🔴 Sync Inactive ".red()
        };
        let sync_line = [sync_state, "(s: toggle sync) ".into()]
            .into_iter()
            .collect::<Line>();

        // Show the client name (if set) in the footer.
        let connection_info = match &self.client_name {
            Some(client_name) => format!(" {} ", client_name),
            None => String::new(),
        };
        let connection_line = Line::raw(connection_info).cyan();

        let table = Table::new(rows, Constraint::from_percentages([30, 70]))
            .header(header)
            .highlight_symbol("❯❯ ")
            .row_highlight_style(Style::new().bold().blue())
            .block(
                Block::bordered()
                    .border_type(BorderType::Rounded)
                    .title_top(Line::raw(" Tasks (j↓, k↑, ⏎ toggle done) ").left_aligned())
                    .title_top(sync_line.right_aligned())
                    .title_bottom(
                        Line::raw(" (c: create) (d: delete) (e: edit) (q: quit) ").left_aligned(),
                    )
                    .title_bottom(connection_line.right_aligned()),
            );
        StatefulWidget::render(table, area, buf, &mut self.table_state);
    }

    /// Render create/edit prompt dialog when in CreateTask or EditTask mode
    fn render_todo_prompt(&self, area: Rect, buf: &mut Buffer) {
        let (dialog_title, input) = match &self.mode {
            TodoMode::CreateTask { buffer } => (" New Todo ", buffer),
            TodoMode::EditTask { buffer, .. } => (" Edit Todo ", buffer),
            _ => {
                return;
            }
        };

        let space = area.inner(Margin::new(2, 2));
        Clear.render(space, buf);
        Block::bordered()
            .border_type(BorderType::Rounded)
            .title(dialog_title)
            .title_bottom(" (Esc: back) ")
            .padding(Padding::uniform(1))
            .render(space, buf);
        let space = space.inner(Margin::new(2, 2));
        Line::raw(input).render(space, buf);
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
                    .repository
                    .tasks()
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
        if self.manager.is_sync_active() {
            self.manager.stop_sync();
        } else {
            self.manager.start_sync()?;
        }
        Ok(())
    }

    /// Toggle "done" for the currently selected item in the list
    async fn try_toggle_done(&self) -> Result<()> {
        let tasks = self.repository.tasks();
        let task_index = self
            .table_state
            .selected()
            .context("failed to get todolist selected index")?;
        let selected_task = tasks
            .get(task_index)
            .cloned()
            .context("failed to find selected task")?;

        self.repository
            .toggle_done(&selected_task.id, !selected_task.done)
            .await
    }

    /// Delete the task item currently selected in the list
    pub async fn try_delete_task(&mut self) -> Result<()> {
        let tasks = self.repository.tasks();
        let task_index = self
            .table_state
            .selected()
            .context("failed to get todolist selected index")?;
        let selected_task = tasks
            .get(task_index)
            .cloned()
            .context("failed to find selected task")?;

        self.repository.delete_task(&selected_task.id).await
    }

    /// Create a new task todo with the given title
    pub async fn try_create_new_todo(&mut self, title: String) -> Result<()> {
        self.repository.create_task(title).await
    }

    /// Set the title of the task with the given ID
    pub async fn try_edit_todo(&mut self, id: &str, title: &str) -> Result<()> {
        self.repository.edit_task(id, title).await
    }
}
