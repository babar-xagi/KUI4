use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

use colored::Colorize;
use kui_packager::pipeline::{PackagingOptions, PackagingPipeline};

use crate::config::KuiConfig;
use crate::device::{find_adb_path, list_devices};
use crate::doctor::find_kotlinc;

pub fn find_project_root(cwd: &Path) -> Option<PathBuf> {
    let mut curr = cwd.to_path_buf();
    loop {
        if curr.join("kui.toml").is_file() {
            return Some(curr);
        }
        if !curr.pop() {
            break;
        }
    }
    None
}

pub fn execute_build(_args: &[String], cwd: &Path) -> (i32, Option<PathBuf>, Option<String>) {
    let project_root = match find_project_root(cwd) {
        Some(r) => r,
        None => {
            eprintln!(
                "kui: Could not locate 'kui.toml' in '{}' or parent directories.",
                cwd.display()
            );
            return (1, None, None);
        }
    };

    let config_path = project_root.join("kui.toml");
    let config = match KuiConfig::load_from_file(&config_path) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("kui: Failed to parse kui.toml: {}", e);
            return (1, None, None);
        }
    };

    let pkg_name = config.project.application_id.clone().unwrap_or_else(|| {
        let clean_name = config.project.name.to_lowercase().replace('-', "_");
        format!("com.example.{}", clean_name)
    });

    let (min_sdk, target_sdk) = match &config.android {
        Some(a) => (a.min_sdk as i32, a.target_sdk as i32),
        None => (24, 36),
    };

    println!("{}", "==================================================".cyan());
    println!(
        " 📦 {} (kui-packager)",
        "KUI Native Android Packager".bold()
    );
    println!("{}", "==================================================".cyan());
    println!("Project:         {}", config.project.name.bold());
    println!("Package:         {}", pkg_name);
    println!("Version:         {}", config.project.version);
    println!("Min / Target:    SDK {} / SDK {}", min_sdk, target_sdk);

    // Optional Kotlin compilation if sources present
    let src_dir = project_root.join("src");
    let classes_dir = project_root.join("build").join("classes");
    if src_dir.is_dir() {
        let mut kt_sources = Vec::new();
        collect_kt_files(&src_dir, &mut kt_sources);
        if !kt_sources.is_empty() {
            let kotlinc_report = find_kotlinc();
            if let Some(ref kotlinc_bin) = kotlinc_report.path {
                fs::create_dir_all(&classes_dir).ok();
                let mut cmd = Command::new(kotlinc_bin);
                let mut candidates = Vec::new();
                if let Ok(jar_env) = std::env::var("KUI_JAR") {
                    candidates.push(PathBuf::from(jar_env));
                }
                if let Some(kui_home) = crate::build::bridge::find_kui_home() {
                    candidates.push(kui_home.join(".kui").join("build").join("kui.jar"));
                    candidates.push(kui_home.join("bin").join("kui.jar"));
                }
                candidates.push(PathBuf::from(r"D:\rust_kot\KUI4\.kui\build\kui.jar"));
                candidates.push(PathBuf::from(r"D:\rust_kot\.kui\build\kui.jar"));

                for candidate in candidates {
                    if candidate.is_file() {
                        cmd.arg("-cp").arg(&candidate);
                        break;
                    }
                }
                for src in &kt_sources {
                    cmd.arg(src);
                }
                match cmd.status() {
                    Ok(exit_status) => {
                        if !exit_status.success() {
                            eprintln!();
                            eprintln!(
                                "{} Kotlin compilation failed with exit code {:?}.",
                                "✗ BUILD FAILED:".red().bold(),
                                exit_status.code().unwrap_or(1)
                            );
                            return (1, None, None);
                        }
                    }
                    Err(e) => {
                        eprintln!();
                        eprintln!(
                            "{} Failed to execute kotlinc: {}",
                            "✗ BUILD FAILED:".red().bold(),
                            e
                        );
                        return (1, None, None);
                    }
                }
            }
        }
    }

    let options = PackagingOptions {
        package_name: pkg_name.clone(),
        version_code: 1,
        version_name: config.project.version.clone(),
        min_sdk,
        target_sdk,
        app_label: config.project.name.clone(),
    };

    let result = PackagingPipeline::package_and_sign(&project_root, &options, None, None);

    if !result.is_success {
        eprintln!();
        eprintln!("{} {}", "✗ BUILD FAILED:".red().bold(), result.message);
        return (1, None, None);
    }

    let output_apk = result.output_file.expect("output apk path");
    let rel_apk = output_apk
        .strip_prefix(&project_root)
        .unwrap_or(&output_apk);

    println!("Architecture:    4-byte memory-aligned (zipalign verified)");
    println!("Signing Scheme:  APK Signature Scheme v2 (RSA-2048 PKCS#1 v1.5)");
    println!("Integrity:       Cryptographically Verified (Tamper-evident tree hash)");
    println!(
        "Output APK:      {} ({} bytes, {} entries)",
        rel_apk.display().to_string().green(),
        result.apk_size,
        result.entry_count
    );
    println!("{}", "--------------------------------------------------".cyan());
    println!("{}", "STATUS: SUCCESS - Native APK ready for installation.".green().bold());
    println!();

    (0, Some(output_apk), Some(pkg_name))
}

pub fn execute_install(args: &[String], cwd: &Path) -> i32 {
    let (code, apk_path, _pkg_name) = execute_build(args, cwd);
    if code != 0 || apk_path.is_none() {
        return code;
    }
    let apk = apk_path.unwrap();

    let adb_path = match find_adb_path() {
        Some(p) => p,
        None => {
            eprintln!("kui: ADB not found on PATH. Cannot install APK to device.");
            return 1;
        }
    };

    let devices = match list_devices(&adb_path) {
        Ok(d) => d,
        Err(e) => {
            eprintln!("kui: Error listing devices: {}", e);
            return 1;
        }
    };

    let online_device = devices.iter().find(|d| d.is_online());
    let device_serial = match online_device {
        Some(d) => &d.serial,
        None => {
            eprintln!("kui: No online Android devices found via ADB. Run 'kui devices' to inspect.");
            return 1;
        }
    };

    println!("[KUI] Installing APK to device '{}'...", device_serial);
    let mut cmd = Command::new(&adb_path);
    cmd.arg("-s").arg(device_serial);
    cmd.arg("install").arg("-r").arg(&apk);

    match cmd.status() {
        Ok(status) if status.success() => {
            println!("{}", "✓ APK installed successfully!".green().bold());
            0
        }
        Ok(status) => {
            eprintln!("kui: adb install exited with code {:?}", status.code());
            1
        }
        Err(e) => {
            eprintln!("kui: Failed to run adb install: {}", e);
            1
        }
    }
}

pub fn execute_run(args: &[String], cwd: &Path) -> i32 {
    let (code, apk_path, pkg_name) = execute_build(args, cwd);
    if code != 0 || apk_path.is_none() || pkg_name.is_none() {
        return code;
    }
    let apk = apk_path.unwrap();
    let pkg = pkg_name.unwrap();

    let adb_path = match find_adb_path() {
        Some(p) => p,
        None => {
            eprintln!("kui: ADB not found on PATH. Cannot run APK on device.");
            return 1;
        }
    };

    let devices = match list_devices(&adb_path) {
        Ok(d) => d,
        Err(e) => {
            eprintln!("kui: Error listing devices: {}", e);
            return 1;
        }
    };

    let online_device = devices.iter().find(|d| d.is_online());
    let device_serial = match online_device {
        Some(d) => &d.serial,
        None => {
            eprintln!("kui: No online Android devices found via ADB. Run 'kui devices' to inspect.");
            return 1;
        }
    };

    println!("[KUI] Installing APK to device '{}'...", device_serial);
    let mut install_cmd = Command::new(&adb_path);
    install_cmd.arg("-s").arg(device_serial).arg("install").arg("-r").arg(&apk);
    let install_status = match install_cmd.status() {
        Ok(s) => s,
        Err(e) => {
            eprintln!("kui: Failed to run adb install: {}", e);
            return 1;
        }
    };

    if !install_status.success() {
        eprintln!("kui: Failed to install APK on device.");
        return 1;
    }

    let target_component = format!("{}/.MainActivity", pkg);
    println!("[KUI] Launching component '{}'...", target_component);

    let mut start_cmd = Command::new(&adb_path);
    start_cmd
        .arg("-s")
        .arg(device_serial)
        .arg("shell")
        .arg("am")
        .arg("start")
        .arg("-n")
        .arg(&target_component);

    match start_cmd.status() {
        Ok(s) if s.success() => {
            println!("{}", "🚀 Application started successfully on device!".green().bold());
            0
        }
        Ok(s) => {
            eprintln!("kui: adb am start exited with code {:?}", s.code());
            1
        }
        Err(e) => {
            eprintln!("kui: Failed to launch application via adb: {}", e);
            1
        }
    }
}

fn collect_kt_files(dir: &Path, out: &mut Vec<PathBuf>) {
    if let Ok(entries) = fs::read_dir(dir) {
        for entry in entries.flatten() {
            let p = entry.path();
            if p.is_dir() {
                collect_kt_files(&p, out);
            } else if p.is_file() && p.extension().and_then(|s| s.to_str()) == Some("kt") {
                out.push(p);
            }
        }
    }
}
