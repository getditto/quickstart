use anyhow::{Context, Result};
use indexmap::IndexMap;
use resolve_path::PathResolveExt;
use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct DittoConfig {
    pub profiles: IndexMap<String, Profile>,
}

impl DittoConfig {
    pub fn from_default_path() -> Result<Self> {
        const DEFAULT_PATH: &str = "~/.ditto/config.yaml";
        let config_path = DEFAULT_PATH.resolve();
        let config = Self::from_path(&config_path)?;
        Ok(config)
    }

    pub fn from_path(config_path: &Path) -> Result<Self> {
        let parent = config_path.parent().with_context(|| {
            format!(
                "failed to find parent directory to config file {}",
                config_path.display()
            )
        })?;

        let exists = config_path
            .try_exists()
            .context("failed to inspect config file")?;

        // If the config directory doesn't exist, create it and write a default config
        if !exists {
            std::fs::create_dir_all(parent).with_context(|| {
                format!("failed to create config directory {}", parent.display())
            })?;

            let default_config = DittoConfig::default();
            let config_string = serde_yaml::to_string(&default_config)
                .context("failed to serialize default config")?;
            std::fs::write(config_path, &config_string).with_context(|| {
                format!(
                    "failed to write default config to {}",
                    config_path.display()
                )
            })?;

            return Ok(default_config);
        }

        let config_string = std::fs::read_to_string(config_path)
            .with_context(|| format!("failed to read config file {}", config_path.display()))?;
        let config =
            serde_yaml::from_str::<Self>(&config_string).context("failed to deserialize config")?;

        Ok(config)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub name: Option<String>,
    pub app_id: String,
    pub root: Option<PathBuf>,
    pub identity: Identity,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "kind")]
pub enum Identity {
    OnlinePlayground(OnlinePlayground),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OnlinePlayground {
    pub token: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_config() {
        let config = DittoConfig {
            profiles: [(
                "ditto-tui".into(),
                Profile {
                    name: Some("App".to_string()),
                    app_id: "0xdeadbeef".to_string(),
                    root: Some("./ditto".into()),
                    identity: Identity::OnlinePlayground(OnlinePlayground {
                        token: "tokentokentoken".to_string(),
                    }),
                },
            )]
            .into(),
        };

        let config_yaml = serde_yaml::to_string(&config).unwrap();
        println!("{config_yaml}");
    }
}
