use chrono::{DateTime, Utc};
use clap::Parser;
use core::f32;
use dittolive_ditto::prelude::*;
use rand::Rng;
use serde::{Deserialize, Serialize};
use std::{
    sync::Arc,
    time::{Duration, SystemTime},
};

// This app mimics the periodic update and observation of sensor data.
// Run multiple instances of the app in different terminals, e.g.:
// $ cargo run -- --id kitchen
// $ cargo run -- --id bedroom"

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Parse CLI arguments
    let cli = Cli::parse();

    // Initialize Ditto OnlinePlayground and allow for local peer-to-peer connections
    let ditto = Ditto::builder()
        .with_root(Arc::new(TempRoot::new()))
        .with_identity(|ditto_root| {
            identity::OnlinePlayground::new(
                ditto_root,
                cli.app_id,
                cli.token,
                false,
                Some(&cli.custom_auth_url),
            )
        })?
        .with_transport_config(|_identity| -> TransportConfig {
            let mut transport_config = TransportConfig::new();
            transport_config
                .connect
                .websocket_urls
                .insert(cli.websocket_url.clone());
            transport_config.enable_all_peer_to_peer();
            transport_config
        })?
        .build()?;

    // Disable backward compatibility
    ditto.disable_sync_with_v3()?;

    // Start syncing data with all the peers
    ditto.start_sync()?;

    // Register a subscription to receive data from the sensors collection
    let _subscription = ditto
        .sync()
        .register_subscription_v2("SELECT * FROM sensors")?;

    // Spawn the insertion/update task to periodically update the sensor's value
    let store = ditto.store().clone();
    let id = DocumentId::new(&cli.id)?;

    tokio::task::spawn(async move {
        loop {
            let sample = Sample {
                id: id.clone(),
                value: rand::rng().random(), // Randomly generate the measurement value
                timestamp: SystemTime::now().into(), // Generate the timestamp
            };

            // Insert the sensor measurement. If a measurement for the sensor already exists, then update it.
            println!("> Updating\t{}", sample);
            store
                .execute_v2((
                    "INSERT INTO sensors DOCUMENTS (:sample) ON ID CONFLICT DO UPDATE",
                    serde_json::json!({"sample": sample}),
                ))
                .await
                .unwrap();

            // Sleep for 5 seconds to not flood the system
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
    });

    // Observe changes from all the other sensors
    // Build a channel to receive the results of the query from the observer
    let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel();

    // Register an observer and filter out our own updates
    let query = format!(
        "SELECT * FROM sensors WHERE NOT _id='{}' ORDER BY _id",
        cli.id
    );
    let _observer = ditto
        .store()
        .register_observer_v2(query, move |query_result| {
            query_result
                .into_iter()
                .flat_map(|it| it.deserialize_value::<Sample>().ok())
                .for_each(|d| tx.send(d).unwrap());
        })?;

    // Loop over the query results and print it at screen
    while let Some(d) = rx.recv().await {
        println!("< Observing\t{}", d);
    }

    ditto.close();

    Ok(())
}

#[derive(Serialize, Deserialize, Debug)]
struct Sample {
    // The sensor ID
    #[serde(rename = "_id")]
    id: DocumentId,
    // The value measured
    value: f32,
    // The timestamp of the measurement
    timestamp: DateTime<Utc>,
}

impl std::fmt::Display for Sample {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}\tValue: {:.4}\tSensor: {}",
            self.timestamp, self.value, self.id
        )
    }
}

#[derive(Debug, Parser)]
#[command(
    about = "This app mimics the periodic update and observation of sensor data. 
Run multiple instances of the app in different terminals, e.g.:

$ cargo run -- --id kitchen
$ cargo run -- --id bedroom"
)]
pub struct Cli {
    /// The Ditto App ID this app will use to initialize Ditto
    #[clap(long, env = "DITTO_APP_ID")]
    app_id: AppId,

    /// The Online Playground token this app should use for authentication
    #[clap(long, env = "DITTO_PLAYGROUND_TOKEN")]
    token: String,

    /// The custom auth URL this app should use for authentication
    #[clap(long, env = "DITTO_AUTH_URL")]
    custom_auth_url: String,

    /// The websocket URL this app should use for authentication
    #[clap(long, env = "DITTO_WEBSOCKET_URL")]
    websocket_url: String,

    /// The id of the sensor. E.g.: kitchen, bedroom
    #[clap(long)]
    id: String,
}
