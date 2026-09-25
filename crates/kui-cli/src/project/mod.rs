use std::collections::HashMap;
use std::fs;
use std::path::{Path, PathBuf};
use regex::Regex;
use crate::cli::version::VERSION_STRING;
use crate::config::{find_project_root, KuiConfig};

pub struct NewProjectOptions {
    pub name: String,
    pub target_dir: PathBuf,
    pub application_id: Option<String>,
    pub version: String,
    pub version_code: u32,
    pub min_sdk: u32,
    pub target_sdk: u32,
    pub minimal: bool,
}

impl NewProjectOptions {
    pub fn resolve_application_id(&self) -> String {
        if let Some(ref id) = self.application_id {
            return id.clone();
        }
        let clean = self.name.replace('-', "_").to_lowercase();
        format!("com.example.{}", clean)
    }
}

pub fn validate_project_name(name: &str) -> Result<(), String> {
    if name.trim().is_empty() {
        return Err("Project name cannot be empty.".to_string());
    }
    let re = Regex::new(r"^[a-zA-Z][a-zA-Z0-9_-]*$").unwrap();
    if !re.is_match(name) {
        return Err(format!(
            "Invalid project name '{}'. Must start with a letter and contain only alphanumeric characters, dashes, or underscores.",
            name
        ));
    }
    Ok(())
}

pub fn validate_application_id(app_id: &str) -> Result<(), String> {
    if app_id.trim().is_empty() {
        return Err("Application ID cannot be empty.".to_string());
    }
    let re = Regex::new(r"^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$").unwrap();
    if !re.is_match(app_id) {
        return Err(format!(
            "Invalid application ID '{}'. Must be in package format (e.g. 'com.example.myapp').",
            app_id
        ));
    }
    Ok(())
}

pub fn generate_project(options: &NewProjectOptions) -> Result<PathBuf, String> {
    validate_project_name(&options.name)?;
    let app_id = options.resolve_application_id();
    validate_application_id(&app_id)?;

    let root = &options.target_dir;
    if root.exists() {
        let entries = fs::read_dir(root)
            .map_err(|e| format!("Failed to read target directory {}: {}", root.display(), e))?;
        if entries.count() > 0 {
            let canon = root.canonicalize().unwrap_or_else(|_| root.clone());
            return Err(format!(
                "Directory '{}' already exists and is not empty.",
                canon.display()
            ));
        }
    } else {
        fs::create_dir_all(root)
            .map_err(|e| format!("Failed to create directory {}: {}", root.display(), e))?;
    }

    // 1. Generate kui.toml
    let kui_toml_content = format!(
        "[project]\nname = \"{}\"\nversion = \"{}\"\nversion_code = {}\napplication_id = \"{}\"\ngenerator_version = \"{}\"\n\n[android]\nmin_sdk = {}\ntarget_sdk = {}\n\n[ui]\ntheme = \"system\"\n",
        options.name,
        options.version,
        options.version_code,
        app_id,
        VERSION_STRING,
        options.min_sdk,
        options.target_sdk
    );
    fs::write(root.join("kui.toml"), kui_toml_content)
        .map_err(|e| format!("Failed to write kui.toml: {}", e))?;

    // 2. Generate src/main.kt
    let src_dir = root.join("src");
    fs::create_dir_all(&src_dir)
        .map_err(|e| format!("Failed to create src dir: {}", e))?;

    let main_kt_content = format!(
        "import ui4.*\n\nfun main() = app {{\n    screen {{\n        center {{\n            text(\"Hello, {}! 👋\")\n        }}\n    }}\n}}\n",
        options.name
    );
    fs::write(src_dir.join("main.kt"), main_kt_content)
        .map_err(|e| format!("Failed to write src/main.kt: {}", e))?;

    // 3. Generate README.md
    let mut readme_content = format!(
        "# {}\n\nCreated with KUI (version {}).\n\n## Quick Start\n\n```powershell\nkui run\n```\n\n## Project Layout\n- `kui.toml`: Project metadata and configuration\n- `src/main.kt`: UI4 declarative application entry point\n",
        options.name, VERSION_STRING
    );
    if !options.minimal {
        readme_content.push_str("- `assets/`: App images and font resources\n- `tests/`: Automated unit and UI tests\n");
    }
    fs::write(root.join("README.md"), readme_content)
        .map_err(|e| format!("Failed to write README.md: {}", e))?;

    // 4. Non-minimal extras
    if !options.minimal {
        let assets_dir = root.join("assets");
        let images_dir = assets_dir.join("images");
        let fonts_dir = assets_dir.join("fonts");
        fs::create_dir_all(&images_dir).map_err(|e| e.to_string())?;
        fs::create_dir_all(&fonts_dir).map_err(|e| e.to_string())?;
        fs::write(images_dir.join(".gitkeep"), "").map_err(|e| e.to_string())?;
        fs::write(fonts_dir.join(".gitkeep"), "").map_err(|e| e.to_string())?;

        let tests_dir = root.join("tests");
        fs::create_dir_all(&tests_dir).map_err(|e| e.to_string())?;
        let pkg_last = app_id.rsplit('.').next().unwrap_or("app");
        let app_test_content = format!(
            "package {}\n\nfun main() {{\n    println(\"Running {} tests: PASS\")\n}}\n",
            pkg_last, options.name
        );
        fs::write(tests_dir.join("AppTest.kt"), app_test_content).map_err(|e| e.to_string())?;

        let gitignore_content = ".kui/\n*.class\n*.jar\n*.log\n";
        fs::write(root.join(".gitignore"), gitignore_content).map_err(|e| e.to_string())?;
    }

    let canon = root.canonicalize().unwrap_or_else(|_| root.clone());
    Ok(canon)
}

pub fn execute_new(args: &[String], flags: &HashMap<String, String>, working_dir: &Path) -> i32 {
    if args.is_empty() {
        eprintln!("kui new: Missing project name.");
        eprintln!("Usage: kui new <name> [--minimal] [--app-id=<id>]");
        return 1;
    }

    let name = &args[0];
    let target_dir = working_dir.join(name);

    let app_id = flags.get("app-id").or_else(|| flags.get("application-id")).cloned();
    let version = flags.get("version").cloned().unwrap_or_else(|| "0.1.0".to_string());
    let version_code = flags.get("version-code").and_then(|v| v.parse().ok()).unwrap_or(1);
    let min_sdk = flags.get("min-sdk").and_then(|v| v.parse().ok()).unwrap_or(24);
    let target_sdk = flags.get("target-sdk").and_then(|v| v.parse().ok()).unwrap_or(36);
    let minimal = flags.contains_key("minimal") || flags.contains_key("m");

    let options = NewProjectOptions {
        name: name.clone(),
        target_dir,
        application_id: app_id,
        version,
        version_code,
        min_sdk,
        target_sdk,
        minimal,
    };

    match generate_project(&options) {
        Ok(path) => {
            println!("Created project '{}' at: {}", options.name, path.display());
            println!();
            println!("Next steps:");
            println!("  cd {}", options.name);
            println!("  kui run");
            0
        }
        Err(err) => {
            eprintln!("kui new error:");
            eprintln!("  - {}", err);
            1
        }
    }
}

pub fn execute_info(start_dir: &Path) -> i32 {
    let root = match find_project_root(start_dir) {
        Some(r) => r,
        None => {
            eprintln!("kui: Not in a KUI project (no 'kui.toml' found).");
            return 1;
        }
    };

    let config_path = root.join("kui.toml");
    match KuiConfig::load_from_file(&config_path) {
        Ok(cfg) => {
            println!("KUI Project Information:");
            println!("  Name:            {}", cfg.project.name);
            println!("  Version:         {}", cfg.project.version);
            if let Some(ref app_id) = cfg.project.application_id {
                println!("  Application ID:  {}", app_id);
            }
            if let Some(ref desc) = cfg.project.description {
                println!("  Description:     {}", desc);
            }
            if let Some(ref android) = cfg.android {
                println!("  Min SDK:         {}", android.min_sdk);
                println!("  Target SDK:      {}", android.target_sdk);
            } else {
                println!("  Min SDK:         24");
                println!("  Target SDK:      36");
            }
            if let Some(ref ui) = cfg.ui {
                println!("  UI Theme:        {}", ui.theme);
            } else {
                println!("  UI Theme:        system");
            }
            let canon = root.canonicalize().unwrap_or_else(|_| root.clone());
            println!("  Project Root:    {}", canon.display());
            0
        }
        Err(e) => {
            eprintln!("kui: Failed to read project configuration: {}", e);
            1
        }
    }
}
