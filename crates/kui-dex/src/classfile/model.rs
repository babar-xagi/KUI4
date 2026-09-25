use std::collections::HashMap;

/// JVM Constant Pool Entry representations.
#[derive(Debug, Clone, PartialEq)]
pub enum CpInfo {
    Utf8(String),
    Integer(i32),
    Float(f32),
    Long(i64),
    Double(f64),
    Class(u16),
    StringCp(u16),
    Fieldref {
        class_index: u16,
        name_and_type_index: u16,
    },
    Methodref {
        class_index: u16,
        name_and_type_index: u16,
    },
    InterfaceMethodref {
        class_index: u16,
        name_and_type_index: u16,
    },
    NameAndType {
        name_index: u16,
        descriptor_index: u16,
    },
    MethodHandle {
        reference_kind: u8,
        reference_index: u16,
    },
    MethodType {
        descriptor_index: u16,
    },
    Dynamic {
        bootstrap_method_attr_index: u16,
        name_and_type_index: u16,
    },
    InvokeDynamic {
        bootstrap_method_attr_index: u16,
        name_and_type_index: u16,
    },
    Module(u16),
    Package(u16),
    Empty, // Follows Long or Double in the constant pool
}

/// JVM Field representation.
#[derive(Debug, Clone, PartialEq)]
pub struct FieldInfo {
    pub access_flags: u16,
    pub name: String,
    pub descriptor: String,
    pub attributes: HashMap<String, Vec<u8>>,
}

/// Exception entry in method Code attribute.
#[derive(Debug, Clone, PartialEq)]
pub struct ExceptionEntry {
    pub start_pc: u16,
    pub end_pc: u16,
    pub handler_pc: u16,
    pub catch_type: u16,
}

/// Method Code attribute containing JVM bytecode.
#[derive(Debug, Clone, PartialEq)]
pub struct CodeAttribute {
    pub max_stack: u16,
    pub max_locals: u16,
    pub code: Vec<u8>,
    pub exception_table: Vec<ExceptionEntry>,
}

/// JVM Method representation.
#[derive(Debug, Clone, PartialEq)]
pub struct MethodInfo {
    pub access_flags: u16,
    pub name: String,
    pub descriptor: String,
    pub code: Option<CodeAttribute>,
    pub attributes: HashMap<String, Vec<u8>>,
}

/// Parsed JVM .class binary file representation.
#[derive(Debug, Clone, PartialEq)]
pub struct ClassFile {
    pub minor_version: u16,
    pub major_version: u16,
    pub constant_pool: Vec<Option<CpInfo>>, // 1-indexed (index 0 is None)
    pub access_flags: u16,
    pub this_class_name: String,
    pub super_class_name: Option<String>,
    pub interface_names: Vec<String>,
    pub fields: Vec<FieldInfo>,
    pub methods: Vec<MethodInfo>,
    pub attributes: HashMap<String, Vec<u8>>,
}

impl ClassFile {
    pub fn get_utf8(&self, index: u16) -> &str {
        match self.constant_pool.get(index as usize) {
            Some(Some(CpInfo::Utf8(s))) => s.as_str(),
            _ => "",
        }
    }

    pub fn get_class_name(&self, class_index: u16) -> &str {
        match self.constant_pool.get(class_index as usize) {
            Some(Some(CpInfo::Class(name_index))) => self.get_utf8(*name_index),
            _ => "",
        }
    }

    pub fn source_file(&self) -> Option<String> {
        let data = self.attributes.get("SourceFile")?;
        if data.len() >= 2 {
            let idx = u16::from_be_bytes([data[0], data[1]]);
            let s = self.get_utf8(idx);
            if !s.is_empty() {
                return Some(s.to_string());
            }
        }
        None
    }
}
