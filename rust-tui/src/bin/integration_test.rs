use anyhow::{Context, Result};
use ditto_quickstart::tui::Todolist;
use dittolive_ditto::prelude::*;
use dittolive_ditto::{fs::TempRoot, Ditto};
use std::time::Duration;
use std::{env, sync::Arc};
use tokio::time::sleep;

#[tokio::main]
async fn main() -> Result<()> {
    println!("🦀 Starting Rust TUI Integration Test");

    // Load environment variables
    dotenvy::dotenv().ok();

    let database_id = env::var("DITTO_APP_ID").context("DITTO_APP_ID not found")?;

    let token = env::var("DITTO_PLAYGROUND_TOKEN").context("DITTO_PLAYGROUND_TOKEN not found")?;
    let custom_auth_url =
        env::var("DITTO_AUTH_URL").unwrap_or_else(|_| "https://auth.cloud.ditto.live".to_string());

    let websocket_url =
        env::var("DITTO_WEBSOCKET_URL").unwrap_or_else(|_| "wss://cloud.ditto.live".to_string());

    // Get task to find from environment
    let task_to_find =
        env::var("DITTO_CLOUD_TASK_TITLE").context("DITTO_CLOUD_TASK_TITLE not found")?;

    println!("🔍 Looking for task: {}", task_to_find);

    let connect_config = DittoConfigConnect::Server {
        url: custom_auth_url.parse().unwrap(),
    };

    let config = DittoConfig::new(database_id, connect_config)
        .with_persistence_directory(Arc::new(TempRoot::new()).root_path());

    // Create Ditto instance (using same pattern as main.rs)
    let ditto = Ditto::open_sync(config)?;

    ditto.auth().unwrap().set_expiration_handler(TokenHandler {
        token: token.clone(),
    });

    ditto.update_transport_config(|config| {
        config.enable_all_peer_to_peer();
    });

    // Disable sync with v3 peers and DQL strict mode

    // Start sync
    let _ = ditto.sync().start();
    println!("✅ Created Ditto instance and started sync");

    // Create todolist instance (loads the app)
    let client_name = env::var("DITTO_CLIENT_NAME").ok();
    let todolist = Todolist::new(ditto, websocket_url, client_name)?;
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

    todolist.ditto.sync().stop();
    println!("🛑 Stopped sync");

    println!("🎉 Integration test passed! App loads and syncs with Ditto Cloud successfully.");
    Ok(())
}

struct TokenHandler {
    token: String,
}

impl DittoAuthExpirationHandler for TokenHandler {
    async fn on_expiration(&self, ditto: &Ditto, _duration_remaining: Duration) {
        match ditto
            .auth()
            .unwrap()
            .login(self.token.as_str(), &identity::get_development_provider())
        {
            Ok(_) => println!("Authentication successful"),
            Err(e) => println!("Authentication failed: {}", e),
        }
    }
}
