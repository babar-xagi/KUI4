use std::fs;
use std::process::Command;
use kui_dex::compiler::ClassToDexCompiler;
use kui_dex::dex::builder::DexFileBuilder;
use kui_dex::dex::constants::*;

#[test]
fn test_build_main_activity_dex() {
    let main_activity = ClassToDexCompiler::build_main_activity(
        "com.example.todo",
        "Hello from Todo App!\n\nRunning on KUI 🚀",
    );

    let mut builder = DexFileBuilder::new();
    builder.add_class(main_activity);

    let dex_bytes = builder.build().expect("DEX build failed");

    // 1. Verify Header Magic
    assert_eq!(&dex_bytes[0..8], DEX_FILE_MAGIC);

    // 2. Verify Endian Tag
    let endian = u32::from_le_bytes([dex_bytes[40], dex_bytes[41], dex_bytes[42], dex_bytes[43]]);
    assert_eq!(endian, ENDIAN_CONSTANT);

    // 3. Verify Header Size
    let header_size = u32::from_le_bytes([dex_bytes[36], dex_bytes[37], dex_bytes[38], dex_bytes[39]]);
    assert_eq!(header_size, HEADER_SIZE);

    // 4. Verify File Size in header matches buffer length
    let file_size = u32::from_le_bytes([dex_bytes[32], dex_bytes[33], dex_bytes[34], dex_bytes[35]]);
    assert_eq!(file_size as usize, dex_bytes.len());

    // 5. Test with Google Android build-tools dexdump if available on system
    let dexdump_paths = [
        "C:\\Users\\DELL\\AppData\\Local\\Android\\Sdk\\build-tools\\36.0.0\\dexdump.exe",
        "C:\\Users\\DELL\\AppData\\Local\\Android\\Sdk\\build-tools\\35.0.0\\dexdump.exe",
    ];

    let temp_dir = tempfile::tempdir().unwrap();
    let dex_path = temp_dir.path().join("classes.dex");
    fs::write(&dex_path, &dex_bytes).unwrap();

    for dexdump in &dexdump_paths {
        if std::path::Path::new(dexdump).is_file() {
            println!("Testing with dexdump: {}", dexdump);

            // -c : verify checksum and exit
            let status_c = Command::new(dexdump)
                .arg("-c")
                .arg(&dex_path)
                .status()
                .expect("Failed to execute dexdump -c");
            assert!(status_c.success(), "dexdump -c checksum verification failed!");

            // -f : display dex file header
            let output_f = Command::new(dexdump)
                .arg("-f")
                .arg(&dex_path)
                .output()
                .expect("Failed to execute dexdump -f");
            assert!(output_f.status.success(), "dexdump -f failed: {}", String::from_utf8_lossy(&output_f.stderr));

            // -d : disassemble code sections
            let output_d = Command::new(dexdump)
                .arg("-d")
                .arg(&dex_path)
                .output()
                .expect("Failed to execute dexdump -d");
            assert!(output_d.status.success(), "dexdump -d failed: {}", String::from_utf8_lossy(&output_d.stderr));
            let stdout_d = String::from_utf8_lossy(&output_d.stdout);
            println!("DEXDUMP STDOUT:\n{}", stdout_d);
            assert!(stdout_d.contains("com.example.todo.MainActivity") || stdout_d.contains("MainActivity"));

            println!("Dexdump validation passed perfectly!");
            break;
        }
    }
}

#[test]
fn test_parse_method_descriptor() {
    let (params, ret) = ClassToDexCompiler::parse_method_descriptor("()V");
    assert!(params.is_empty());
    assert_eq!(ret, "V");

    let (params, ret) = ClassToDexCompiler::parse_method_descriptor("(ILjava/lang/String;[B)Z");
    assert_eq!(params, vec!["I", "Ljava/lang/String;", "[B"]);
    assert_eq!(ret, "Z");

    let (params, ret) = ClassToDexCompiler::parse_method_descriptor("([[Ljava/lang/Object;)Ljava/util/List;");
    assert_eq!(params, vec!["[[Ljava/lang/Object;"]);
    assert_eq!(ret, "Ljava/util/List;");
}

#[test]
fn test_parse_and_compile_real_jvm_class() {
    let class_path = std::path::Path::new("D:/rust_kot/KUI4/examples/myaapp/.kui/build/classes/MainKt.class");
    if !class_path.is_file() {
        return;
    }

    let class_file = kui_dex::classfile::ClassFileReader::read_file(class_path)
        .expect("Failed to parse JVM class file");

    assert_eq!(class_file.this_class_name, "MainKt");
    assert!(!class_file.methods.is_empty());

    let dex_class = ClassToDexCompiler::compile_class(&class_file);
    assert_eq!(dex_class.class_descriptor, "LMainKt;");

    let mut builder = DexFileBuilder::new();
    builder.add_class(dex_class);

    let dex_bytes = builder.build().expect("Failed to build DEX from compiled JVM class");
    assert!(!dex_bytes.is_empty());

    // Verify with dexdump if available
    let dexdump = "C:\\Users\\DELL\\AppData\\Local\\Android\\Sdk\\build-tools\\36.0.0\\dexdump.exe";
    if std::path::Path::new(dexdump).is_file() {
        let temp_dir = tempfile::tempdir().unwrap();
        let dex_out = temp_dir.path().join("classes.dex");
        fs::write(&dex_out, &dex_bytes).unwrap();

        let status = Command::new(dexdump)
            .arg("-c")
            .arg(&dex_out)
            .status()
            .expect("dexdump -c execution");
        assert!(status.success(), "dexdump -c verification failed for real JVM class");

        let output_d = Command::new(dexdump)
            .arg("-d")
            .arg(&dex_out)
            .output()
            .expect("dexdump -d execution");
        assert!(output_d.status.success());
        let stdout = String::from_utf8_lossy(&output_d.stdout);
        assert!(stdout.contains("MainKt"));
        println!("Real JVM class MainKt compiled to DEX and verified by dexdump!");
    }
}
