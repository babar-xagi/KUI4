use std::fs;
use std::path::Path;
use crate::config::find_project_root;

pub fn execute_clean(start_dir: &Path) -> i32 {
    let root = match find_project_root(start_dir) {
        Some(r) => r,
        None => {
            eprintln!("kui: No 'kui.toml' found in current directory or any parent directories.");
            return 1;
        }
    };

    let project_name = root
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("unknown");

    println!("Cleaning project '{}'...", project_name);

    let kui_build = root.join(".kui").join("build");
    let kui_cache = root.join(".kui").join("cache");
    let app_build = root.join("build");

    let mut success = true;

    if kui_build.exists() {
        if let Err(e) = fs::remove_dir_all(&kui_build) {
            eprintln!("[KUI] Warning: Failed to remove {}: {}", kui_build.display(), e);
            success = false;
        }
    }

    if kui_cache.exists() {
        if let Err(e) = fs::remove_dir_all(&kui_cache) {
            eprintln!("[KUI] Warning: Failed to remove {}: {}", kui_cache.display(), e);
            success = false;
        }
    }

    if app_build.exists() {
        if let Err(e) = fs::remove_dir_all(&app_build) {
            eprintln!("[KUI] Warning: Failed to remove {}: {}", app_build.display(), e);
            success = false;
        }
    }

    if success {
        println!("[KUI] Clean complete: Removed build and cache artifacts.");
        0
    } else {
        eprintln!("[KUI] Warning: Failed to cleanly remove some build artifacts.");
        1
    }
}
