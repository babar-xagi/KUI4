use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use crate::doctor::{find_java, find_kotlinc};

pub fn find_kui_home() -> Option<PathBuf> {
    // 1. Explicit KUI_HOME
    if let Ok(home) = env::var("KUI_HOME") {
        let p = PathBuf::from(home);
        if p.join("platform").join("kui").is_dir() {
            return Some(p);
        }
    }

    // 2. Relative to current exe
    if let Ok(exe_path) = env::current_exe() {
        if let Some(parent) = exe_path.parent() {
            // If in <kui_home>/bin/kui.exe
            if parent.file_name().and_then(|n| n.to_str()) == Some("bin") {
                if let Some(grandparent) = parent.parent() {
                    if grandparent.join("platform").join("kui").is_dir() {
                        return Some(grandparent.to_path_buf());
                    }
                }
            }
            // If in target/debug or target/release inside workspace
            let mut search = parent.to_path_buf();
            for _ in 0..4 {
                let kui4_candidate = search.join("KUI4");
                if kui4_candidate.join("platform").join("kui").is_dir() {
                    return Some(kui4_candidate);
                }
                if search.join("platform").join("kui").is_dir() {
                    return Some(search);
                }
                if !search.pop() {
                    break;
                }
            }
        }
    }

    // 3. Search upwards from cwd
    if let Ok(cwd) = env::current_dir() {
        let mut search = cwd;
        loop {
            let candidate1 = search.join("KUI4");
            if candidate1.join("platform").join("kui").is_dir() {
                return Some(candidate1);
            }
            if search.join("platform").join("kui").is_dir() {
                return Some(search);
            }
            if !search.pop() {
                break;
            }
        }
    }

    // 4. Default fallback on Windows if D:\rust_kot\KUI4 exists
    let fallback = PathBuf::from(r"D:\rust_kot\KUI4");
    if fallback.join("platform").join("kui").is_dir() {
        return Some(fallback);
    }

    None
}

pub fn ensure_kui_jar(kui_home: &Path) -> Result<PathBuf, String> {
    let build_dir = kui_home.join(".kui").join("build");
    let jar_path = build_dir.join("kui.jar");

    // 1. If precompiled kui.jar exists and is valid, use it directly!
    // In installed environments (C:\Program Files\KUI), kui.jar is shipped precompiled.
    if jar_path.is_file() {
        if let Ok(meta) = fs::metadata(&jar_path) {
            if meta.len() > 1000 {
                // If not explicitly in development mode with a .git repo, use the precompiled JAR directly.
                let is_git_repo = kui_home.join(".git").exists() || kui_home.join("..").join(".git").exists();
                let is_dev_mode = env::var("KUI_DEV").map(|v| v == "1").unwrap_or(false);
                if !is_git_repo || !is_dev_mode {
                    return Ok(jar_path);
                }
            }
        }
    }

    let platform_kui = kui_home.join("platform").join("kui");
    let platform_ui4 = kui_home.join("platform").join("ui4");

    let mut source_files = Vec::new();
    collect_kotlin_sources(&platform_kui, &mut source_files);
    collect_kotlin_sources(&platform_ui4, &mut source_files);

    if source_files.is_empty() {
        if jar_path.is_file() {
            return Ok(jar_path);
        }
        return Err(format!(
            "No Kotlin source files found in {} or {}",
            platform_kui.display(),
            platform_ui4.display()
        ));
    }

    let mut need_compile = !jar_path.is_file();
    if !need_compile {
        if let Ok(jar_meta) = fs::metadata(&jar_path) {
            if let Ok(jar_time) = jar_meta.modified() {
                for src in &source_files {
                    if let Ok(src_meta) = fs::metadata(src) {
                        if let Ok(src_time) = src_meta.modified() {
                            if src_time > jar_time {
                                need_compile = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    if need_compile {
        println!("[KUI] Bootstrapping KUI platform jar (kui.jar)...");
        let kotlinc_report = find_kotlinc();
        let kotlinc_path = kotlinc_report.path.ok_or_else(|| {
            format!("Cannot compile kui.jar: {}", kotlinc_report.message)
        })?;

        fs::create_dir_all(&build_dir).ok();
        let sources_file = env::temp_dir().join("kui_sources_kui.txt");
        let sources_content = source_files
            .iter()
            .map(|p| p.to_string_lossy().to_string())
            .collect::<Vec<_>>()
            .join("\n");
        fs::write(&sources_file, sources_content)
            .map_err(|e| format!("Failed to write sources file: {}", e))?;

        let status = Command::new(&kotlinc_path)
            .arg(format!("@{}", sources_file.display()))
            .arg("-include-runtime")
            .arg("-d")
            .arg(&jar_path)
            .status()
            .map_err(|e| format!("Failed to run kotlinc: {}", e))?;

        if !status.success() {
            return Err("KUI platform jar compilation failed.".to_string());
        }
        println!("[KUI] Platform jar compiled successfully -> {}", jar_path.display());
    }

    Ok(jar_path)
}

fn collect_kotlin_sources(dir: &Path, out: &mut Vec<PathBuf>) {
    if !dir.is_dir() {
        return;
    }
    if let Ok(entries) = fs::read_dir(dir) {
        for entry in entries.flatten() {
            let p = entry.path();
            if p.is_dir() {
                collect_kotlin_sources(&p, out);
            } else if p.is_file() && p.extension().and_then(|s| s.to_str()) == Some("kt") {
                out.push(p);
            }
        }
    }
}

pub fn bridge_to_jvm(raw_args: &[String]) -> i32 {
    let kui_home = match find_kui_home() {
        Some(h) => h,
        None => {
            eprintln!("kui: Could not locate KUI platform installation directory (KUI_HOME).");
            eprintln!("Please ensure KUI_HOME environment variable is set or run from a valid KUI repository.");
            return 1;
        }
    };

    let jar_path = match ensure_kui_jar(&kui_home) {
        Ok(j) => j,
        Err(e) => {
            eprintln!("kui bridge error: {}", e);
            return 1;
        }
    };

    let java_report = find_java();
    let java_path = match java_report.path {
        Some(p) => p,
        None => {
            eprintln!("kui: {}", java_report.message);
            return 1;
        }
    };

    let kui_home_canon = kui_home.canonicalize().unwrap_or(kui_home);

    let mut cmd = Command::new(&java_path);
    cmd.arg(format!("-Dkui.home={}", kui_home_canon.display()));
    cmd.arg("-cp");
    cmd.arg(&jar_path);
    cmd.arg("kui.cli.MainKt");
    cmd.args(raw_args);

    if let Some(ref kotlinc_path) = find_kotlinc().path {
        if let Some(bin_dir) = kotlinc_path.parent() {
            if let Some(home_dir) = bin_dir.parent() {
                cmd.env("KOTLIN_HOME", home_dir);
                cmd.env("KOTLINC_HOME", home_dir);
            }
        }
    }

    cmd.stdin(Stdio::inherit());
    cmd.stdout(Stdio::inherit());
    cmd.stderr(Stdio::inherit());

    match cmd.status() {
        Ok(status) => status.code().unwrap_or(1),
        Err(e) => {
            eprintln!("Failed to execute Java process: {}", e);
            1
        }
    }
}
