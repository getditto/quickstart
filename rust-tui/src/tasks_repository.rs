use std::sync::Arc;

use anyhow::Result;
use dittolive_ditto::{store::StoreObserver, sync::SyncSubscription};
use serde::{Deserialize, Serialize};
use tokio::sync::watch;
use uuid::Uuid;

use crate::ditto_manager::DittoManager;

/// A single task document in the "tasks" collection.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Task {
    #[serde(rename = "_id")]
    pub id: String,
    pub title: String,
    pub done: bool,
    pub deleted: bool,
}

impl Task {
    pub fn new(title: String) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            title,
            done: false,
            deleted: false,
        }
    }
}

/// Owns everything specific to the tasks data: the sync subscription, the store
/// observer that streams the current task list, and the task CRUD operations.
/// It talks to Ditto directly through the instance vended by [`DittoManager`].
///
/// The repository shares ownership of the manager via `Arc<DittoManager>`, so
/// the manager (and its persistence directory) cannot be dropped while the
/// repository is still using it.
///
/// [`DittoManager`]: crate::ditto_manager::DittoManager
pub struct TasksRepository {
    /// Ditto observer handles must be held (not dropped) to keep them alive.
    /// Observers provide the callback triggers that let us react to events.
    pub observer: Arc<StoreObserver>,

    /// Ditto subscriptions must also be held to keep them alive. Subscriptions
    /// cause Ditto to sync selected data from other peers.
    pub subscription: Arc<SyncSubscription>,

    /// Our observer sends any document updates into this watch channel.
    tasks_rx: watch::Receiver<Vec<Task>>,

    /// Shared owner of the Ditto instance. The repository reaches Ditto through
    /// [`DittoManager::ditto`]. Declared last so it drops after `observer` and
    /// `subscription` — those borrow from the Ditto instance it owns and must be
    /// torn down before it (Rust drops fields in declaration order).
    manager: Arc<DittoManager>,
}

impl TasksRepository {
    /// Register the tasks subscription and observer against the Ditto instance
    /// owned by the given manager.
    pub fn try_new(manager: Arc<DittoManager>) -> Result<Self> {
        let ditto = manager.ditto();
        let (tasks_tx, tasks_rx) = watch::channel(Vec::new());

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        let subscription = ditto.sync().register_subscription("SELECT * FROM tasks")?;

        // Register observer, which runs against the local database on this peer
        let observer = ditto.store().register_observer(
            "SELECT * FROM tasks WHERE NOT deleted",
            move |query_result| {
                let docs = query_result
                    .into_iter()
                    .flat_map(|it| it.deserialize_value::<Task>().ok())
                    .collect::<Vec<_>>();
                tasks_tx.send_replace(docs);
            },
        )?;

        Ok(Self {
            manager,
            tasks_rx,
            observer,
            subscription,
        })
    }

    /// The current task list, as most recently reported by the observer.
    pub fn tasks(&self) -> Vec<Task> {
        self.tasks_rx.borrow().clone()
    }

    /// Create a new task with the given title.
    pub async fn create_task(&self, title: String) -> Result<()> {
        let task = Task::new(title);
        self.manager
            .ditto()
            .store()
            .execute((
                "INSERT INTO tasks DOCUMENTS (:task)",
                serde_json::json!({ "task": task }),
            ))
            .await?;
        Ok(())
    }

    /// Set the title of the task with the given ID.
    pub async fn edit_task(&self, id: &str, title: &str) -> Result<()> {
        self.manager
            .ditto()
            .store()
            .execute((
                "UPDATE tasks SET title = :title WHERE _id = :id",
                serde_json::json!({ "title": title, "id": id }),
            ))
            .await?;
        Ok(())
    }

    /// Set the "done" flag of the task with the given ID.
    pub async fn toggle_done(&self, id: &str, done: bool) -> Result<()> {
        self.manager
            .ditto()
            .store()
            .execute((
                "UPDATE tasks SET done = :done WHERE _id = :id",
                serde_json::json!({ "id": id, "done": done }),
            ))
            .await?;
        Ok(())
    }

    /// Soft-delete the task with the given ID.
    pub async fn delete_task(&self, id: &str) -> Result<()> {
        self.manager
            .ditto()
            .store()
            .execute((
                "UPDATE tasks SET deleted = true WHERE _id = :id",
                serde_json::json!({ "id": id }),
            ))
            .await?;
        Ok(())
    }
}
