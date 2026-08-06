use std::{env, time::Duration};

use anyhow::{Context, Result};
use ditto_quickstart::{ditto_manager::DittoManager, tasks_repository::TasksRepository};
use dittolive_ditto::prelude::*;
use tokio::time::sleep;

#[tokio::main]
async fn main() -> Result<()> {
    println!("🦀 Starting Rust TUI Integration Test");

    // Load .env from the quickstart root (one directory above this crate);
    // fall back to dotenvy's CWD-upward search if the file is absent.
    let manifest_dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR"));
    let env_path = manifest_dir.join("../.env");
    if env_path.exists() {
        dotenvy::from_path(&env_path).ok();
    } else {
        dotenvy::dotenv().ok();
    }

    let database_id = env::var("DITTO_DATABASE_ID").context("DITTO_DATABASE_ID not found")?;

    let token = env::var("DITTO_DEVELOPMENT_TOKEN").context("DITTO_DEVELOPMENT_TOKEN not found")?;
    let server_url = env::var("DITTO_SERVER_URL")
        .unwrap_or_else(|_| "https://auth.cloud.ditto.live".to_string());

    // Get task to find from environment
    let task_to_find =
        env::var("DITTO_CLOUD_TASK_TITLE").context("DITTO_CLOUD_TASK_TITLE not found")?;

    println!("🔍 Looking for task: {}", task_to_find);

    // Configure the Ditto instance (same pattern as main.rs) with all
    // peer-to-peer transports enabled.
    let manager =
        DittoManager::try_new(database_id, token.clone(), server_url, String::new(), true)?;

    // Explicitly log in before starting sync so the first sync round has a
    // valid JWT/X.509 cert. Without this we rely on the expiration handler
    // firing during startup, which races with the initial sync attempt.
    manager
        .ditto()
        .auth()
        .context("failed to get authenticator")?
        .login(token.as_str(), &identity::get_development_provider())
        .context("failed to log in to Ditto server")?;
    println!("🔑 Authenticated with Ditto server");

    manager.start_sync()?;
    println!("✅ Created Ditto instance and started sync");

    // Create the tasks repository (registers the subscription + observer)
    let repository = TasksRepository::try_new(manager.ditto())?;
    println!("📝 App loaded - Created tasks repository");

    // Wait for sync and check for the seeded task
    println!("🕐 Waiting for sync and checking for seeded task...");
    let mut attempts = 0;
    let max_attempts = 30; // 30 seconds timeout (matches cpp-tui)
    let mut found_task = false;

    while attempts < max_attempts && !found_task {
        sleep(Duration::from_secs(1)).await;
        attempts += 1;

        let tasks = repository.tasks();
        for task in &tasks {
            if task.title == task_to_find {
                found_task = true;
                println!("✅ Found seeded task: {}", task.title);
                break;
            }
        }

        if attempts % 3 == 0 {
            let count = repository.tasks().len();
            println!(
                "   ... still syncing ({}/{}), {} tasks visible locally",
                attempts, max_attempts, count
            );
        }
    }

    if !found_task {
        let tasks = repository.tasks();
        println!(
            "❌ Seeded task '{}' not found after {} seconds",
            task_to_find, max_attempts
        );
        println!("📊 Found {} tasks total", tasks.len());
        let rust_tui_tasks: Vec<&str> = tasks
            .iter()
            .filter(|t| t.title.contains("_rust-tui_"))
            .map(|t| t.title.as_str())
            .take(10)
            .collect();
        if rust_tui_tasks.is_empty() {
            println!("   No rust-tui CI tasks visible locally");
        } else {
            println!("   First rust-tui CI tasks visible:");
            for title in rust_tui_tasks {
                println!("   - {}", title);
            }
        }
        anyhow::bail!("Integration test failed - seeded task not found");
    }

    manager.stop_sync();
    println!("🛑 Stopped sync");

    println!("🎉 Integration test passed! App loads and syncs with Ditto server successfully.");
    Ok(())
}
