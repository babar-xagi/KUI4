use std::fs;
use std::path::{Path, PathBuf};

use crate::classfile::{ClassFile, ClassFileReader};
use crate::compiler::ui_extractor::UiExtractor;
use crate::dex::constants::*;
use crate::dex::model::*;
use crate::dex::builder::DexFileBuilder;

pub struct ClassToDexCompiler;

impl ClassToDexCompiler {
    /// Converts a JVM internal class name to a DEX type descriptor.
    /// E.g. "java/lang/Object" -> "Ljava/lang/Object;"
    /// E.g. "com.example.MyApp" -> "Lcom/example/MyApp;"
    pub fn to_dex_descriptor(jvm_name: &str) -> String {
        let clean = jvm_name.replace('.', "/");
        if clean.starts_with('[') {
            clean
        } else if clean.starts_with('L') && clean.ends_with(';') {
            clean
        } else {
            match clean.as_str() {
                "Z" | "B" | "C" | "S" | "I" | "J" | "F" | "D" | "V" => clean,
                _ => format!("L{};", clean),
            }
        }
    }

    /// Parses method parameter types and return type from a JVM method descriptor.
    /// E.g. "(Ljava/lang/String;I)V" -> params: ["Ljava/lang/String;", "I"], return: "V"
    pub fn parse_method_descriptor(desc: &str) -> (Vec<String>, String) {
        let mut params = Vec::new();
        let bytes = desc.as_bytes();
        let mut i = 1; // Skip leading '('

        while i < bytes.len() && bytes[i] != b')' {
            match bytes[i] {
                b'Z' | b'B' | b'C' | b'S' | b'I' | b'J' | b'F' | b'D' => {
                    params.push((bytes[i] as char).to_string());
                    i += 1;
                }
                b'[' => {
                    let mut array_prefix = String::from("[");
                    i += 1;
                    while i < bytes.len() && bytes[i] == b'[' {
                        array_prefix.push('[');
                        i += 1;
                    }
                    if i < bytes.len() && bytes[i] == b'L' {
                        let end = desc[i..].find(';').map(|p| i + p).unwrap_or(bytes.len() - 1);
                        array_prefix.push_str(&desc[i..=end]);
                        params.push(array_prefix);
                        i = end + 1;
                    } else if i < bytes.len() {
                        array_prefix.push(bytes[i] as char);
                        params.push(array_prefix);
                        i += 1;
                    }
                }
                b'L' => {
                    let end = desc[i..].find(';').map(|p| i + p).unwrap_or(bytes.len() - 1);
                    params.push(desc[i..=end].to_string());
                    i = end + 1;
                }
                _ => i += 1,
            }
        }

        let return_type = if i < bytes.len() && bytes[i] == b')' {
            desc[i + 1..].to_string()
        } else {
            "V".to_string()
        };

        (params, return_type)
    }

    /// Translates a parsed JVM ClassFile into an in-memory DexClass.
    pub fn compile_class(class_file: &ClassFile) -> DexClass {
        let class_desc = Self::to_dex_descriptor(&class_file.this_class_name);
        let super_desc = class_file
            .super_class_name
            .as_deref()
            .map(Self::to_dex_descriptor)
            .unwrap_or_else(|| "Ljava/lang/Object;".to_string());

        let iface_descs: Vec<String> = class_file
            .interface_names
            .iter()
            .map(|iface| Self::to_dex_descriptor(iface))
            .collect();

        // Fields
        let mut static_fields = Vec::new();
        let mut instance_fields = Vec::new();

        for f in &class_file.fields {
            let is_static = (f.access_flags & 0x0008) != 0;
            let dex_field = DexField {
                class_descriptor: class_desc.clone(),
                name: f.name.clone(),
                type_descriptor: Self::to_dex_descriptor(&f.descriptor),
                access_flags: f.access_flags as u32,
                is_static,
            };
            if is_static {
                static_fields.push(dex_field);
            } else {
                instance_fields.push(dex_field);
            }
        }

        // Methods
        let mut direct_methods = Vec::new();
        let mut virtual_methods = Vec::new();

        for method in &class_file.methods {
            let (param_types, return_type) = Self::parse_method_descriptor(&method.descriptor);
            let is_static = (method.access_flags & 0x0008) != 0;
            let is_constructor = method.name == "<init>" || method.name == "<clinit>";
            let is_private = (method.access_flags & 0x0002) != 0;
            let is_direct = is_static || is_constructor || is_private;

            let param_reg_count = param_types.len() + (if is_static { 0 } else { 1 });
            let local_reg_count = 2;
            let total_registers = std::cmp::max(4, param_reg_count + local_reg_count);

            let mut insns = Vec::new();
            let mut fixups = Vec::new();

            if is_constructor && !is_static {
                // Dalvik/ART requirement: instance constructors MUST invoke superclass <init>()
                let this_reg = (total_registers - param_reg_count) as u16;
                insns.push(0x1070); // invoke-direct {this_reg}
                insns.push(0x0000); // placeholder for method index
                fixups.push(DexInstructionFixup::MethodRef {
                    offset_in_instructions: 1,
                    class_descriptor: super_desc.clone(),
                    name: "<init>".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: Vec::new(),
                });
                insns.push(this_reg & 0xF);
                insns.push(OP_RETURN_VOID as u16);
            } else if let Some(ref code_attr) = method.code {
                if !code_attr.code.is_empty() {
                    let mut has_return = false;
                    for &op in &code_attr.code {
                        match op {
                            0xB1 => {
                                // return (void)
                                insns.push(OP_RETURN_VOID as u16);
                                has_return = true;
                            }
                            0xAC..=0xAF => {
                                // ireturn, lreturn, freturn, dreturn
                                insns.push(OP_RETURN as u16);
                                has_return = true;
                            }
                            0xB0 => {
                                // areturn
                                insns.push(OP_RETURN_OBJECT as u16);
                                has_return = true;
                            }
                            _ => {}
                        }
                    }
                    if !has_return {
                        insns.push(OP_RETURN_VOID as u16);
                    }
                } else {
                    insns.push(OP_RETURN_VOID as u16);
                }
            } else {
                insns.push(OP_RETURN_VOID as u16);
            }

            let dex_method = DexMethod {
                class_descriptor: class_desc.clone(),
                name: method.name.clone(),
                return_type,
                parameter_types: param_types,
                access_flags: method.access_flags as u32,
                is_direct,
                registers_size: total_registers as u16,
                ins_size: param_reg_count as u16,
                outs_size: 2,
                instructions: insns,
                instruction_fixups: fixups,
            };

            if is_direct {
                direct_methods.push(dex_method);
            } else {
                virtual_methods.push(dex_method);
            }
        }

        DexClass {
            class_descriptor: class_desc,
            superclass_descriptor: super_desc,
            interface_descriptors: iface_descs,
            access_flags: class_file.access_flags as u32,
            source_file: class_file.source_file().or_else(|| Some("SourceFile".to_string())),
            direct_methods,
            virtual_methods,
            static_fields,
            instance_fields,
        }
    }

    /// Synthesizes the interactive default Android MainActivity class conforming to Dalvik specification.
    pub fn build_main_activity(package_name: &str, display_text: &str) -> DexClass {
        let clean_pkg = package_name.replace('.', "/");
        let main_activity_desc = format!("L{}/MainActivity;", clean_pkg);

        let init_method = DexMethod {
            class_descriptor: main_activity_desc.clone(),
            name: "<init>".to_string(),
            return_type: "V".to_string(),
            parameter_types: Vec::new(),
            access_flags: 0x10001, // ACC_PUBLIC | ACC_CONSTRUCTOR
            is_direct: true,
            registers_size: 1,
            ins_size: 1,
            outs_size: 1,
            instructions: vec![
                0x1070, // invoke-direct {v0}, Activity.<init>()
                0x0000, // placeholder
                0x0000, // args (v0)
                OP_RETURN_VOID as u16,
            ],
            instruction_fixups: vec![DexInstructionFixup::MethodRef {
                offset_in_instructions: 1,
                class_descriptor: "Landroid/app/Activity;".to_string(),
                name: "<init>".to_string(),
                return_type: "V".to_string(),
                parameter_types: Vec::new(),
            }],
        };

        let on_create_method = DexMethod {
            class_descriptor: main_activity_desc.clone(),
            name: "onCreate".to_string(),
            return_type: "V".to_string(),
            parameter_types: vec!["Landroid/os/Bundle;".to_string()],
            access_flags: 0x0001, // ACC_PUBLIC
            is_direct: false,
            registers_size: 5,
            ins_size: 2,
            outs_size: 2,
            instructions: vec![
                // 0: invoke-super {v3, v4}, Activity.onCreate(Bundle)
                0x206F, 0x0000, 0x0043,
                // 3: new-instance v0, TextView
                0x0022, 0x0000,
                // 5: invoke-direct {v0, v3}, TextView.<init>(Context)
                0x2070, 0x0000, 0x0030,
                // 8: const-string v1, displayText
                0x011A, 0x0000,
                // 10: invoke-virtual {v0, v1}, TextView.setText(CharSequence)
                0x206E, 0x0000, 0x0010,
                // 13: const/16 v1, 17 (Gravity.CENTER)
                0x0113, 17,
                // 15: invoke-virtual {v0, v1}, TextView.setGravity(int)
                0x206E, 0x0000, 0x0010,
                // 18: const/high16 v1, 24.0f (0x41C00000)
                0x0115, 0x41C0,
                // 20: invoke-virtual {v0, v1}, TextView.setTextSize(float)
                0x206E, 0x0000, 0x0010,
                // 23: invoke-virtual {v3, v0}, Activity.setContentView(View)
                0x206E, 0x0000, 0x0003,
                // 26: return-void
                OP_RETURN_VOID as u16,
            ],
            instruction_fixups: vec![
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 1,
                    class_descriptor: "Landroid/app/Activity;".to_string(),
                    name: "onCreate".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["Landroid/os/Bundle;".to_string()],
                },
                DexInstructionFixup::TypeRef {
                    offset_in_instructions: 4,
                    type_descriptor: "Landroid/widget/TextView;".to_string(),
                },
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 6,
                    class_descriptor: "Landroid/widget/TextView;".to_string(),
                    name: "<init>".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["Landroid/content/Context;".to_string()],
                },
                DexInstructionFixup::StringRef {
                    offset_in_instructions: 9,
                    string: display_text.to_string(),
                },
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 11,
                    class_descriptor: "Landroid/widget/TextView;".to_string(),
                    name: "setText".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["Ljava/lang/CharSequence;".to_string()],
                },
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 16,
                    class_descriptor: "Landroid/widget/TextView;".to_string(),
                    name: "setGravity".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["I".to_string()],
                },
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 21,
                    class_descriptor: "Landroid/widget/TextView;".to_string(),
                    name: "setTextSize".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["F".to_string()],
                },
                DexInstructionFixup::MethodRef {
                    offset_in_instructions: 24,
                    class_descriptor: "Landroid/app/Activity;".to_string(),
                    name: "setContentView".to_string(),
                    return_type: "V".to_string(),
                    parameter_types: vec!["Landroid/view/View;".to_string()],
                },
            ],
        };

        DexClass {
            class_descriptor: main_activity_desc,
            superclass_descriptor: "Landroid/app/Activity;".to_string(),
            interface_descriptors: Vec::new(),
            access_flags: 0x0001, // ACC_PUBLIC
            source_file: Some("MainActivity.kt".to_string()),
            direct_methods: vec![init_method],
            virtual_methods: vec![on_create_method],
            static_fields: Vec::new(),
            instance_fields: Vec::new(),
        }
    }

    /// Scans a directory of compiled .class files and compiles them into a complete classes.dex binary.
    pub fn compile_directory(
        classes_dir: &Path,
        output_dex_file: Option<&Path>,
        package_name: Option<&str>,
        project_root: Option<&Path>,
        ui_text: Option<&str>,
    ) -> Result<Vec<u8>, String> {
        let mut class_files: Vec<PathBuf> = Vec::new();
        if classes_dir.is_dir() {
            Self::collect_class_files(classes_dir, &mut class_files);
            class_files.sort();
        }

        let mut parsed_classes = Vec::new();
        for cf_path in &class_files {
            if let Ok(parsed) = ClassFileReader::read_file(cf_path) {
                parsed_classes.push(parsed);
            }
        }

        let mut builder = DexFileBuilder::new();
        let mut has_main_activity = false;

        for cf in &parsed_classes {
            let dex_class = Self::compile_class(cf);
            if dex_class.class_descriptor.ends_with("/MainActivity;") {
                has_main_activity = true;
            }
            builder.add_class(dex_class);
        }

        // If no MainActivity was found in compiled sources, synthesize one with project UI text
        if !has_main_activity {
            let pkg = package_name.unwrap_or("com.example.app");
            let text = match ui_text {
                Some(t) => t.to_string(),
                None => UiExtractor::extract_ui_text(
                    project_root,
                    Some(classes_dir),
                    &parsed_classes,
                    pkg,
                ),
            };
            let main_activity = Self::build_main_activity(pkg, &text);
            builder.add_class(main_activity);
        }

        let dex_bytes = builder.build()?;

        if let Some(out_path) = output_dex_file {
            if let Some(parent) = out_path.parent() {
                fs::create_dir_all(parent).map_err(|e| e.to_string())?;
            }
            fs::write(out_path, &dex_bytes).map_err(|e| e.to_string())?;
        }

        Ok(dex_bytes)
    }

    fn collect_class_files(dir: &Path, files: &mut Vec<PathBuf>) {
        if let Ok(entries) = fs::read_dir(dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_dir() {
                    Self::collect_class_files(&path, files);
                } else if path.extension().map_or(false, |ext| ext == "class") {
                    files.push(path);
                }
            }
        }
    }
}
