use std::fs;
use tempfile::tempdir;
use kui_cli::cli::command::{levenshtein_distance, Command};
use kui_cli::cli::parser::{CommandParser, ParsedInvocation};
use kui_cli::cli::version::{DISPLAY_NAME, VERSION_STRING};
use kui_cli::cli::help::Help;
use kui_cli::config::{find_project_root, KuiConfig};
use kui_cli::device::parse_adb_devices_output;
use kui_cli::doctor::{find_java, find_kotlinc};
use kui_cli::project::{generate_project, validate_application_id, validate_project_name, NewProjectOptions};
use kui_cli::build::clean::execute_clean;

#[test]
fn test_version_constants() {
    assert_eq!(VERSION_STRING, "0.3.0");
    assert_eq!(DISPLAY_NAME, "kui version 0.3.0");
}

#[test]
fn test_command_recognition() {
    assert_eq!(Command::from_str("new"), Some(Command::New));
    assert_eq!(Command::from_str("NEW"), Some(Command::New));
    assert_eq!(Command::from_str("doctor"), Some(Command::Doctor));
    assert_eq!(Command::from_str("devices"), Some(Command::Devices));
    assert_eq!(Command::from_str("clean"), Some(Command::Clean));
    assert_eq!(Command::from_str("info"), Some(Command::Info));
    assert_eq!(Command::from_str("build"), Some(Command::Build));
    assert_eq!(Command::from_str("run"), Some(Command::Run));
    assert_eq!(Command::from_str("nonexistent"), None);
}

#[test]
fn test_levenshtein_distance_and_suggestion() {
    assert_eq!(levenshtein_distance("doctor", "doctr"), 1);
    assert_eq!(levenshtein_distance("devices", "devics"), 1);
    assert_eq!(Command::find_closest("doctr"), Some(Command::Doctor));
    assert_eq!(Command::find_closest("devics"), Some(Command::Devices));
    assert_eq!(Command::find_closest("biuld"), Some(Command::Build));
    assert_eq!(Command::find_closest("cleen"), Some(Command::Clean));
    assert_eq!(Command::find_closest("xyz123abc"), None);
}

#[test]
fn test_parser_empty() {
    let empty: Vec<&str> = vec![];
    assert_eq!(CommandParser::parse(empty), ParsedInvocation::Empty);
}

#[test]
fn test_parser_version() {
    assert_eq!(CommandParser::parse(["-v"]), ParsedInvocation::ShowVersion);
    assert_eq!(CommandParser::parse(["--version"]), ParsedInvocation::ShowVersion);
    assert_eq!(CommandParser::parse(["version"]), ParsedInvocation::ShowVersion);
}

#[test]
fn test_parser_help() {
    assert_eq!(CommandParser::parse(["-h"]), ParsedInvocation::ShowHelp(None));
    assert_eq!(CommandParser::parse(["--help"]), ParsedInvocation::ShowHelp(None));
    assert_eq!(CommandParser::parse(["help"]), ParsedInvocation::ShowHelp(None));
    assert_eq!(CommandParser::parse(["help", "new"]), ParsedInvocation::ShowHelp(Some(Command::New)));
    assert_eq!(CommandParser::parse(["--help", "doctor"]), ParsedInvocation::ShowHelp(Some(Command::Doctor)));
}

#[test]
fn test_parser_unknown_command() {
    let inv = CommandParser::parse(["doctr"]);
    match inv {
        ParsedInvocation::UnknownCommand { raw_name, suggestion } => {
            assert_eq!(raw_name, "doctr");
            assert_eq!(suggestion, Some(Command::Doctor));
        }
        _ => panic!("Expected UnknownCommand"),
    }
}

#[test]
fn test_parser_flags_and_args() {
    let inv = CommandParser::parse([
        "new",
        "my_app",
        "--minimal",
        "--app-id=com.example.test",
        "--version",
        "1.2.0",
        "-f",
    ]);

    match inv {
        ParsedInvocation::ExecuteCommand { command, args, flags } => {
            assert_eq!(command, Command::New);
            assert_eq!(args, vec!["my_app"]);
            assert_eq!(flags.get("minimal"), Some(&"true".to_string()));
            assert_eq!(flags.get("app-id"), Some(&"com.example.test".to_string()));
            assert_eq!(flags.get("version"), Some(&"1.2.0".to_string()));
            assert_eq!(flags.get("f"), Some(&"true".to_string()));
        }
        _ => panic!("Expected ExecuteCommand"),
    }
}

#[test]
fn test_help_formatting() {
    let general = Help::general_help();
    assert!(general.contains("kui version 0.3.0"));
    assert!(general.contains("Usage:"));
    assert!(general.contains("new <name>"));
    assert!(general.contains("doctor"));

    let new_help = Help::command_help(Command::New);
    assert!(new_help.contains("new - create project"));
    assert!(new_help.contains("kui new <name>"));
}

#[test]
fn test_project_name_validation() {
    assert!(validate_project_name("valid_name").is_ok());
    assert!(validate_project_name("valid-name-123").is_ok());
    assert!(validate_project_name("").is_err());
    assert!(validate_project_name("123invalid").is_err());
    assert!(validate_project_name("invalid name with spaces").is_err());
    assert!(validate_project_name("invalid@name!").is_err());
}

#[test]
fn test_application_id_validation() {
    assert!(validate_application_id("com.example.app").is_ok());
    assert!(validate_application_id("org.kui4.demo_app").is_ok());
    assert!(validate_application_id("").is_err());
    assert!(validate_application_id("singleword").is_err());
    assert!(validate_application_id("com.123start.app").is_err());
    assert!(validate_application_id("com.example.app-dash").is_err());
}

#[test]
fn test_project_generation_full() {
    let tmp = tempdir().unwrap();
    let target_dir = tmp.path().join("full_app");

    let options = NewProjectOptions {
        name: "full_app".to_string(),
        target_dir: target_dir.clone(),
        application_id: None,
        version: "0.1.0".to_string(),
        version_code: 1,
        min_sdk: 24,
        target_sdk: 36,
        minimal: false,
    };

    let result = generate_project(&options);
    assert!(result.is_ok(), "Generation failed: {:?}", result.err());

    // Verify files
    let kui_toml = target_dir.join("kui.toml");
    assert!(kui_toml.is_file());
    let cfg = KuiConfig::load_from_file(&kui_toml).unwrap();
    assert_eq!(cfg.project.name, "full_app");
    assert_eq!(cfg.project.application_id, Some("com.example.full_app".to_string()));
    assert_eq!(cfg.android.as_ref().unwrap().min_sdk, 24);
    assert_eq!(cfg.android.as_ref().unwrap().target_sdk, 36);

    let main_kt = target_dir.join("src").join("main.kt");
    assert!(main_kt.is_file());
    let main_text = fs::read_to_string(&main_kt).unwrap();
    assert!(main_text.contains("import ui4.*"));
    assert!(main_text.contains("Hello, full_app! 👋"));

    let readme = target_dir.join("README.md");
    assert!(readme.is_file());

    assert!(target_dir.join("assets").join("images").join(".gitkeep").is_file());
    assert!(target_dir.join("assets").join("fonts").join(".gitkeep").is_file());
    assert!(target_dir.join("tests").join("AppTest.kt").is_file());
    assert!(target_dir.join(".gitignore").is_file());

    // Test find_project_root
    let found = find_project_root(target_dir.join("src"));
    assert_eq!(found, Some(target_dir.clone()));
}

#[test]
fn test_project_generation_minimal() {
    let tmp = tempdir().unwrap();
    let target_dir = tmp.path().join("mini_app");

    let options = NewProjectOptions {
        name: "mini_app".to_string(),
        target_dir: target_dir.clone(),
        application_id: Some("org.example.mini".to_string()),
        version: "0.2.0".to_string(),
        version_code: 2,
        min_sdk: 26,
        target_sdk: 35,
        minimal: true,
    };

    let result = generate_project(&options);
    assert!(result.is_ok());

    assert!(target_dir.join("kui.toml").is_file());
    assert!(target_dir.join("src").join("main.kt").is_file());
    assert!(target_dir.join("README.md").is_file());
    // In minimal mode, assets and tests are omitted
    assert!(!target_dir.join("assets").exists());
    assert!(!target_dir.join("tests").exists());
}

#[test]
fn test_project_generation_collision() {
    let tmp = tempdir().unwrap();
    let target_dir = tmp.path().join("collision_app");
    fs::create_dir_all(&target_dir).unwrap();
    fs::write(target_dir.join("dummy.txt"), "hello").unwrap();

    let options = NewProjectOptions {
        name: "collision_app".to_string(),
        target_dir,
        application_id: None,
        version: "0.1.0".to_string(),
        version_code: 1,
        min_sdk: 24,
        target_sdk: 36,
        minimal: false,
    };

    let result = generate_project(&options);
    assert!(result.is_err());
    assert!(result.unwrap_err().contains("already exists and is not empty"));
}

#[test]
fn test_clean_command() {
    let tmp = tempdir().unwrap();
    let proj_dir = tmp.path().join("test_clean_proj");
    fs::create_dir_all(&proj_dir).unwrap();
    fs::write(proj_dir.join("kui.toml"), "[project]\nname = \"test_clean_proj\"\n").unwrap();

    let kui_build = proj_dir.join(".kui").join("build");
    let kui_cache = proj_dir.join(".kui").join("cache");
    let app_build = proj_dir.join("build");

    fs::create_dir_all(&kui_build).unwrap();
    fs::write(kui_build.join("artifact.jar"), "dummy").unwrap();
    fs::create_dir_all(&kui_cache).unwrap();
    fs::write(kui_cache.join("hash.txt"), "dummy").unwrap();
    fs::create_dir_all(&app_build).unwrap();
    fs::write(app_build.join("app.apk"), "dummy").unwrap();

    let code = execute_clean(&proj_dir);
    assert_eq!(code, 0);

    assert!(!kui_build.exists());
    assert!(!kui_cache.exists());
    assert!(!app_build.exists());
    assert!(proj_dir.join("kui.toml").is_file());
}

#[test]
fn test_adb_devices_parsing() {
    let sample = r#"
List of devices attached
108321541J013120       device product:TECNO-BG7 model:TECNO_BG7 device:TECNO-BG7 transport_id:1
emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emulator64_arm64 transport_id:2
12345678               unauthorized transport_id:3
98765432               offline transport_id:4
"#;

    let devices = parse_adb_devices_output(sample);
    assert_eq!(devices.len(), 4);

    let d0 = &devices[0];
    assert_eq!(d0.serial, "108321541J013120");
    assert!(d0.is_online());
    assert!(!d0.is_emulator());
    assert_eq!(d0.model, Some("TECNO_BG7".to_string()));

    let d1 = &devices[1];
    assert_eq!(d1.serial, "emulator-5554");
    assert!(d1.is_online());
    assert!(d1.is_emulator());

    let d2 = &devices[2];
    assert_eq!(d2.serial, "12345678");
    assert!(d2.is_unauthorized());

    let d3 = &devices[3];
    assert_eq!(d3.serial, "98765432");
    assert!(d3.is_offline());
}

#[test]
fn test_environment_discovery() {
    let java = find_java();
    assert!(java.is_valid, "Java should be valid on this machine: {}", java.message);
    assert!(java.path.is_some());
    assert!(java.version.is_some());

    let kotlin = find_kotlinc();
    assert!(kotlin.is_valid, "Kotlin compiler should be discovered: {}", kotlin.message);
    assert!(kotlin.path.is_some());
}

#[test]
fn test_native_build_end_to_end() {
    let temp = tempdir().unwrap();
    let root = temp.path();

    let target_dir = root.join("nativeapp");
    let opt = NewProjectOptions {
        name: "nativeapp".to_string(),
        target_dir,
        application_id: Some("com.example.nativeapp".to_string()),
        version: "0.1.0".to_string(),
        version_code: 1,
        min_sdk: 24,
        target_sdk: 36,
        minimal: false,
    };

    let proj_dir = generate_project(&opt).unwrap();
    assert!(proj_dir.join("kui.toml").is_file());

    let (code, apk_path, pkg_name) = kui_cli::build::native::execute_build(&[], &proj_dir);
    assert_eq!(code, 0, "Native build must succeed with exit code 0");
    assert_eq!(pkg_name, Some("com.example.nativeapp".to_string()));
    assert!(apk_path.is_some(), "APK path must be returned");

    let apk = apk_path.unwrap();
    assert!(apk.is_file(), "APK output file must exist on disk");

    let apk_bytes = fs::read(&apk).unwrap();
    assert!(apk_bytes.len() > 100);

    // Verify 4-byte alignment
    assert!(
        kui_packager::apk::ApkWriter::verify_alignment(&apk_bytes),
        "Native generated APK must satisfy 4-byte zipalign"
    );

    // Verify APK Signature Scheme v2
    let verify_res = kui_packager::signing::ApkV2Verifier::verify(&apk_bytes);
    assert!(
        verify_res.is_ok(),
        "Native generated APK must pass APK v2 signature verification: {:?}",
        verify_res.err()
    );
}

