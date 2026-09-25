use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ProjectSection {
    pub name: String,
    #[serde(default = "default_version")]
    pub version: String,
    #[serde(default = "default_version_code")]
    pub version_code: u32,
    pub application_id: Option<String>,
    pub generator_version: Option<String>,
    pub description: Option<String>,
}

fn default_version() -> String {
    "0.1.0".to_string()
}

fn default_version_code() -> u32 {
    1
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct AndroidSection {
    #[serde(default = "default_min_sdk")]
    pub min_sdk: u32,
    #[serde(default = "default_target_sdk")]
    pub target_sdk: u32,
}

fn default_min_sdk() -> u32 {
    24
}

fn default_target_sdk() -> u32 {
    36
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct UiSection {
    #[serde(default = "default_theme")]
    pub theme: String,
}

fn default_theme() -> String {
    "system".to_string()
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct PlatformSection {
    pub kotlin_version: Option<String>,
    pub target_jvm: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct KuiConfig {
    pub project: ProjectSection,
    #[serde(default)]
    pub android: Option<AndroidSection>,
    #[serde(default)]
    pub ui: Option<UiSection>,
    #[serde(default)]
    pub platform: Option<PlatformSection>,
}

impl KuiConfig {
    pub fn load_from_file<P: AsRef<Path>>(path: P) -> Result<Self, String> {
        let content = fs::read_to_string(path.as_ref())
            .map_err(|e| format!("Failed to read config file {}: {}", path.as_ref().display(), e))?;
        toml::from_str(&content)
            .map_err(|e| format!("Failed to parse TOML configuration: {}", e))
    }

    pub fn save_to_file<P: AsRef<Path>>(&self, path: P) -> Result<(), String> {
        let content = toml::to_string_pretty(self)
            .map_err(|e| format!("Failed to serialize TOML configuration: {}", e))?;
        fs::write(path.as_ref(), content)
            .map_err(|e| format!("Failed to write config file {}: {}", path.as_ref().display(), e))
    }
}

pub fn find_project_root<P: AsRef<Path>>(start_dir: P) -> Option<PathBuf> {
    let mut current = start_dir.as_ref().to_path_buf();
    loop {
        let toml_path = current.join("kui.toml");
        if toml_path.is_file() {
            return Some(current);
        }
        if !current.pop() {
            break;
        }
    }
    None
}
