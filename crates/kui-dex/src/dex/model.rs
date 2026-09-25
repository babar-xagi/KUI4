/// In-memory DEX representations for classes, methods, fields, and instructions.

/// Computes the Dalvik shorty descriptor for a method signature.
/// E.g. returnType: "V", params: ["Ljava/lang/String;", "I"] -> "VLI"
pub fn compute_shorty(return_type: &str, parameter_types: &[String]) -> String {
    let mut shorty = String::with_capacity(1 + parameter_types.len());
    let ret_ch = if return_type.starts_with('L') || return_type.starts_with('[') {
        'L'
    } else {
        return_type.chars().next().unwrap_or('V')
    };
    shorty.push(ret_ch);

    for p in parameter_types {
        let p_ch = if p.starts_with('L') || p.starts_with('[') {
            'L'
        } else {
            p.chars().next().unwrap_or('V')
        };
        shorty.push(p_ch);
    }
    shorty
}

/// Symbolic reference fixups for Dalvik instructions.
/// Enables instructions to reference method IDs, type IDs, and string IDs
/// that are resolved and patched at DEX layout time.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DexInstructionFixup {
    MethodRef {
        offset_in_instructions: usize,
        class_descriptor: String,
        name: String,
        return_type: String,
        parameter_types: Vec<String>,
    },
    TypeRef {
        offset_in_instructions: usize,
        type_descriptor: String,
    },
    StringRef {
        offset_in_instructions: usize,
        string: String,
    },
}

/// In-memory DEX method definition with Dalvik instructions.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DexMethod {
    pub class_descriptor: String,
    pub name: String,
    pub return_type: String,
    pub parameter_types: Vec<String>,
    pub access_flags: u32,
    pub is_direct: bool,
    pub registers_size: u16,
    pub ins_size: u16,
    pub outs_size: u16,
    pub instructions: Vec<u16>,
    pub instruction_fixups: Vec<DexInstructionFixup>,
}

/// In-memory DEX field definition.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DexField {
    pub class_descriptor: String,
    pub name: String,
    pub type_descriptor: String,
    pub access_flags: u32,
    pub is_static: bool,
}

/// In-memory DEX class definition.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DexClass {
    pub class_descriptor: String,
    pub superclass_descriptor: String,
    pub interface_descriptors: Vec<String>,
    pub access_flags: u32,
    pub source_file: Option<String>,
    pub direct_methods: Vec<DexMethod>,
    pub virtual_methods: Vec<DexMethod>,
    pub static_fields: Vec<DexField>,
    pub instance_fields: Vec<DexField>,
}

impl DexClass {
    pub fn new(class_descriptor: impl Into<String>) -> Self {
        Self {
            class_descriptor: class_descriptor.into(),
            superclass_descriptor: "Ljava/lang/Object;".to_string(),
            interface_descriptors: Vec::new(),
            access_flags: 1, // ACC_PUBLIC
            source_file: Some("SourceFile".to_string()),
            direct_methods: Vec::new(),
            virtual_methods: Vec::new(),
            static_fields: Vec::new(),
            instance_fields: Vec::new(),
        }
    }
}
