use std::env;
use std::path::{Path, PathBuf};
use std::process::Command;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DeviceInfo {
    pub serial: String,
    pub state: String,
    pub product: Option<String>,
    pub model: Option<String>,
    pub device: Option<String>,
    pub transport_id: Option<String>,
}

impl DeviceInfo {
    pub fn is_emulator(&self) -> bool {
        self.serial.starts_with("emulator-")
    }

    pub fn is_online(&self) -> bool {
        self.state.eq_ignore_ascii_case("device")
    }

    pub fn is_unauthorized(&self) -> bool {
        self.state.eq_ignore_ascii_case("unauthorized")
    }

    pub fn is_offline(&self) -> bool {
        self.state.eq_ignore_ascii_case("offline")
    }
}

pub fn find_adb_path() -> Option<PathBuf> {
    // 1. Search PATH
    if let Ok(path_var) = env::var("PATH") {
        let separator = if cfg!(windows) { ';' } else { ':' };
        for part in path_var.split(separator) {
            let p = Path::new(part);
            let candidate = if cfg!(windows) {
                p.join("adb.exe")
            } else {
                p.join("adb")
            };
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }

    // 2. Check ANDROID_HOME / ANDROID_SDK_ROOT
    for var in &["ANDROID_HOME", "ANDROID_SDK_ROOT"] {
        if let Ok(home) = env::var(var) {
            let candidate = Path::new(&home).join("platform-tools").join(if cfg!(windows) { "adb.exe" } else { "adb" });
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }

    // 3. Common Windows locations
    if cfg!(windows) {
        if let Ok(local_app_data) = env::var("LOCALAPPDATA") {
            let candidate = Path::new(&local_app_data)
                .join("Android")
                .join("Sdk")
                .join("platform-tools")
                .join("adb.exe");
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }

    None
}

pub fn parse_adb_devices_output(output: &str) -> Vec<DeviceInfo> {
    let mut devices = Vec::new();
    for line in output.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('*') || trimmed.starts_with("List of devices") {
            continue;
        }

        let parts: Vec<&str> = trimmed.split_whitespace().collect();
        if parts.len() < 2 {
            continue;
        }

        let serial = parts[0].to_string();
        let state = parts[1].to_string();

        let mut product = None;
        let mut model = None;
        let mut device = None;
        let mut transport_id = None;

        for part in &parts[2..] {
            if let Some(val) = part.strip_prefix("product:") {
                product = Some(val.to_string());
            } else if let Some(val) = part.strip_prefix("model:") {
                model = Some(val.to_string());
            } else if let Some(val) = part.strip_prefix("device:") {
                device = Some(val.to_string());
            } else if let Some(val) = part.strip_prefix("transport_id:") {
                transport_id = Some(val.to_string());
            }
        }

        devices.push(DeviceInfo {
            serial,
            state,
            product,
            model,
            device,
            transport_id,
        });
    }
    devices
}

pub fn list_devices(adb_path: &Path) -> Result<Vec<DeviceInfo>, String> {
    let output = Command::new(adb_path)
        .arg("devices")
        .arg("-l")
        .output()
        .map_err(|e| format!("Failed to execute adb: {}", e))?;

    let stdout = String::from_utf8_lossy(&output.stdout);
    Ok(parse_adb_devices_output(&stdout))
}

pub fn execute_devices() -> i32 {
    println!("==================================================");
    println!(" 📱 Android Devices (via ADB)");
    println!("==================================================");

    let adb_path = match find_adb_path() {
        Some(p) => p,
        None => {
            println!("ADB Path: NOT FOUND");
            println!();
            println!("Android Debug Bridge (adb) was not found on PATH or Android SDK locations.");
            return 1;
        }
    };

    println!("ADB Path: {}", adb_path.display());
    println!();

    let devices = match list_devices(&adb_path) {
        Ok(d) => d,
        Err(e) => {
            eprintln!("Error querying ADB devices: {}", e);
            return 1;
        }
    };

    if devices.is_empty() {
        println!("No connected devices or emulators found.");
        println!();
        println!("Troubleshooting tips:");
        println!("  1. Connect your Android phone with a USB data cable (not charge-only).");
        println!("  2. In phone Settings -> Developer Options -> Turn ON 'USB Debugging'.");
        println!("  3. (Xiaomi/Tecno/Realme): Also turn ON 'Install via USB'.");
        println!("  4. Set USB mode in phone notification shade to 'File Transfer' / 'MTP'.");
        println!("  5. Check phone screen for 'Allow USB debugging?' dialog and tap 'Allow'.");
        return 0;
    }

    println!("Found {} connected device(s):", devices.len());
    for d in &devices {
        let type_str = if d.is_emulator() {
            "Emulator"
        } else {
            "Physical Device"
        };
        let model_str = d.model.as_deref().or(d.product.as_deref()).unwrap_or("Android Device");
        let padded_serial = format!("{:<20}", d.serial);

        if d.is_online() {
            println!("  [ONLINE]       {} ({}: {})", padded_serial, type_str, model_str);
        } else if d.is_unauthorized() {
            println!("  [UNAUTHORIZED] {} ({}: {})", padded_serial, type_str, model_str);
            println!("                 ⚠️ ACTION: Unlock phone screen and tap 'Allow USB debugging'!");
        } else if d.is_offline() {
            println!("  [OFFLINE]      {} ({}: {})", padded_serial, type_str, model_str);
            println!("                 ⚠️ ACTION: Reconnect USB cable or toggle USB debugging off and on.");
        } else {
            let padded_state = format!("[{:<12}]", d.state.to_uppercase());
            println!("  {} {} ({}: {})", padded_state, padded_serial, type_str, model_str);
        }
    }
    println!();
    0
}
