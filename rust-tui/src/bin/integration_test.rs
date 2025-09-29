use anyhow::{Context, Result};
use ditto_quickstart::tui::Todolist;
use dittolive_ditto::{fs::TempRoot, identity::OnlinePlayground, AppId, Ditto};
use std::time::Duration;
use std::{env, sync::Arc};
use tokio::time::sleep;

#[tokio::main]
async fn main() -> Result<()> {
    println!("🦀 Starting Rust TUI Integration Test");

    // Load environment variables
    dotenvy::dotenv().ok();

    let app_id: AppId = env::var("DITTO_APP_ID")
        .context("DITTO_APP_ID not found")?
        .parse()
        .context("Invalid DITTO_APP_ID format")?;
    let playground_token =
        env::var("DITTO_PLAYGROUND_TOKEN").context("DITTO_PLAYGROUND_TOKEN not found")?;
    let custom_auth_url =
        env::var("DITTO_AUTH_URL").unwrap_or_else(|_| "https://auth.cloud.ditto.live".to_string());
    let websocket_url =
        env::var("DITTO_WEBSOCKET_URL").unwrap_or_else(|_| "wss://cloud.ditto.live".to_string());

    // Get task to find from environment
    let task_to_find = env::var("TASK_TO_FIND").unwrap_or_else(|_| "Basic Test Task".to_string());

    println!("🔍 Looking for task: {}", task_to_find);

    // Create Ditto instance (using same pattern as main.rs)
    let ditto = Ditto::builder()
        .with_root(Arc::new(TempRoot::new()))
        .with_identity(|root| {
            OnlinePlayground::new(
                root,
                app_id.clone(),
                playground_token,
                false,
                Some(custom_auth_url.as_str()),
            )
        })?
        .build()?;

    ditto.update_transport_config(|config| {
        config.enable_all_peer_to_peer();
        config.connect.websocket_urls.insert(websocket_url);
    });

    // Disable sync with v3 peers and DQL strict mode
    let _ = ditto.disable_sync_with_v3();
    let _ = ditto
        .store()
        .execute_v2("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        .await?;

    // Start sync
    let _ = ditto.start_sync();
    println!("✅ Created Ditto instance and started sync");

    // Create todolist instance (loads the app)
    let todolist = Todolist::new(ditto)?;
    println!("📝 App loaded - Created todolist instance");

    // Wait for sync and check for the seeded task
    println!("🕐 Waiting for sync and checking for seeded task...");
    let mut attempts = 0;
    let max_attempts = 15; // 15 seconds timeout
    let mut found_task = false;

    while attempts < max_attempts && !found_task {
        sleep(Duration::from_secs(1)).await;
        attempts += 1;

        let tasks = todolist.tasks_rx.borrow().clone();
        for task in &tasks {
            if task.title == task_to_find {
                found_task = true;
                println!("✅ Found seeded task: {}", task.title);
                break;
            }
        }

        if attempts % 3 == 0 {
            println!("   ... still syncing ({}/{})", attempts, max_attempts);
        }
    }

    if !found_task {
        println!(
            "❌ Seeded task '{}' not found after {} seconds",
            task_to_find, max_attempts
        );
        println!("📊 Found {} tasks total:", todolist.tasks_rx.borrow().len());
        for task in todolist.tasks_rx.borrow().iter().take(5) {
            println!("   - {}", task.title);
        }
        anyhow::bail!("Integration test failed - seeded task not found");
    }

    todolist.ditto.stop_sync();
    println!("🛑 Stopped sync");

    println!("🎉 Integration test passed! App loads and syncs with Ditto Cloud successfully.");
    Ok(())
}
