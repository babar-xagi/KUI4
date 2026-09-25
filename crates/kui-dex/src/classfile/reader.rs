use std::collections::HashMap;
use std::fs;
use std::path::Path;

use super::model::*;

pub struct ClassFileReader;

pub const CLASS_MAGIC: u32 = 0xCAFEBABE;

impl ClassFileReader {
    pub fn read_file<P: AsRef<Path>>(path: P) -> Result<ClassFile, String> {
        let bytes = fs::read(path).map_err(|e| format!("Failed to read class file: {}", e))?;
        Self::read_bytes(&bytes)
    }

    pub fn read_bytes(bytes: &[u8]) -> Result<ClassFile, String> {
        let mut cursor = Cursor::new(bytes);

        // 1. Magic number
        let magic = cursor.read_u32()?;
        if magic != CLASS_MAGIC {
            return Err(format!("Invalid JVM class magic: 0x{:08X}", magic));
        }

        let minor = cursor.read_u16()?;
        let major = cursor.read_u16()?;

        // 2. Constant Pool (1-indexed)
        let cp_count = cursor.read_u16()? as usize;
        let mut cp: Vec<Option<CpInfo>> = Vec::with_capacity(cp_count);
        cp.push(None); // Index 0 is unused in JVM specification

        let mut i = 1;
        while i < cp_count {
            let tag = cursor.read_u8()?;
            match tag {
                1 => {
                    // CONSTANT_Utf8
                    let len = cursor.read_u16()? as usize;
                    let str_bytes = cursor.read_bytes(len)?;
                    let s = String::from_utf8_lossy(str_bytes).to_string();
                    cp.push(Some(CpInfo::Utf8(s)));
                }
                3 => {
                    // CONSTANT_Integer
                    let val = cursor.read_i32()?;
                    cp.push(Some(CpInfo::Integer(val)));
                }
                4 => {
                    // CONSTANT_Float
                    let bits = cursor.read_u32()?;
                    cp.push(Some(CpInfo::Float(f32::from_bits(bits))));
                }
                5 => {
                    // CONSTANT_Long (takes 2 slots)
                    let val = cursor.read_i64()?;
                    cp.push(Some(CpInfo::Long(val)));
                    cp.push(Some(CpInfo::Empty));
                    i += 1;
                }
                6 => {
                    // CONSTANT_Double (takes 2 slots)
                    let bits = cursor.read_u64()?;
                    cp.push(Some(CpInfo::Double(f64::from_bits(bits))));
                    cp.push(Some(CpInfo::Empty));
                    i += 1;
                }
                7 => {
                    // CONSTANT_Class
                    let name_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Class(name_index)));
                }
                8 => {
                    // CONSTANT_String
                    let string_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::StringCp(string_index)));
                }
                9 => {
                    // CONSTANT_Fieldref
                    let class_index = cursor.read_u16()?;
                    let name_and_type_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Fieldref {
                        class_index,
                        name_and_type_index,
                    }));
                }
                10 => {
                    // CONSTANT_Methodref
                    let class_index = cursor.read_u16()?;
                    let name_and_type_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Methodref {
                        class_index,
                        name_and_type_index,
                    }));
                }
                11 => {
                    // CONSTANT_InterfaceMethodref
                    let class_index = cursor.read_u16()?;
                    let name_and_type_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::InterfaceMethodref {
                        class_index,
                        name_and_type_index,
                    }));
                }
                12 => {
                    // CONSTANT_NameAndType
                    let name_index = cursor.read_u16()?;
                    let descriptor_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::NameAndType {
                        name_index,
                        descriptor_index,
                    }));
                }
                15 => {
                    // CONSTANT_MethodHandle
                    let reference_kind = cursor.read_u8()?;
                    let reference_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::MethodHandle {
                        reference_kind,
                        reference_index,
                    }));
                }
                16 => {
                    // CONSTANT_MethodType
                    let descriptor_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::MethodType { descriptor_index }));
                }
                17 => {
                    // CONSTANT_Dynamic
                    let bootstrap_method_attr_index = cursor.read_u16()?;
                    let name_and_type_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Dynamic {
                        bootstrap_method_attr_index,
                        name_and_type_index,
                    }));
                }
                18 => {
                    // CONSTANT_InvokeDynamic
                    let bootstrap_method_attr_index = cursor.read_u16()?;
                    let name_and_type_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::InvokeDynamic {
                        bootstrap_method_attr_index,
                        name_and_type_index,
                    }));
                }
                19 => {
                    // CONSTANT_Module
                    let name_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Module(name_index)));
                }
                20 => {
                    // CONSTANT_Package
                    let name_index = cursor.read_u16()?;
                    cp.push(Some(CpInfo::Package(name_index)));
                }
                other => {
                    return Err(format!("Unknown JVM Constant Pool tag: {} at index {}", other, i));
                }
            }
            i += 1;
        }

        // Helper closures for resolving CP entries
        let get_utf8_str = |idx: u16| -> String {
            match cp.get(idx as usize) {
                Some(Some(CpInfo::Utf8(s))) => s.clone(),
                _ => String::new(),
            }
        };

        let get_class_str = |idx: u16| -> String {
            match cp.get(idx as usize) {
                Some(Some(CpInfo::Class(name_idx))) => get_utf8_str(*name_idx),
                _ => String::new(),
            }
        };

        // 3. Class headers
        let access_flags = cursor.read_u16()?;
        let this_class_idx = cursor.read_u16()?;
        let this_class_name = get_class_str(this_class_idx);

        let super_class_idx = cursor.read_u16()?;
        let super_class_name = if super_class_idx != 0 {
            Some(get_class_str(super_class_idx))
        } else {
            None
        };

        let interfaces_count = cursor.read_u16()? as usize;
        let mut interface_names = Vec::with_capacity(interfaces_count);
        for _ in 0..interfaces_count {
            let iface_idx = cursor.read_u16()?;
            interface_names.push(get_class_str(iface_idx));
        }

        // 4. Fields
        let fields_count = cursor.read_u16()? as usize;
        let mut fields = Vec::with_capacity(fields_count);
        for _ in 0..fields_count {
            let f_flags = cursor.read_u16()?;
            let f_name_idx = cursor.read_u16()?;
            let f_desc_idx = cursor.read_u16()?;
            let f_name = get_utf8_str(f_name_idx);
            let f_desc = get_utf8_str(f_desc_idx);

            let attr_count = cursor.read_u16()? as usize;
            let mut attrs = HashMap::with_capacity(attr_count);
            for _ in 0..attr_count {
                let a_name_idx = cursor.read_u16()?;
                let a_name = get_utf8_str(a_name_idx);
                let a_len = cursor.read_u32()? as usize;
                let a_data = cursor.read_bytes(a_len)?.to_vec();
                attrs.insert(a_name, a_data);
            }
            fields.push(FieldInfo {
                access_flags: f_flags,
                name: f_name,
                descriptor: f_desc,
                attributes: attrs,
            });
        }

        // 5. Methods
        let methods_count = cursor.read_u16()? as usize;
        let mut methods = Vec::with_capacity(methods_count);
        for _ in 0..methods_count {
            let m_flags = cursor.read_u16()?;
            let m_name_idx = cursor.read_u16()?;
            let m_desc_idx = cursor.read_u16()?;
            let m_name = get_utf8_str(m_name_idx);
            let m_desc = get_utf8_str(m_desc_idx);

            let attr_count = cursor.read_u16()? as usize;
            let mut attrs = HashMap::with_capacity(attr_count);
            let mut code_attr: Option<CodeAttribute> = None;

            for _ in 0..attr_count {
                let a_name_idx = cursor.read_u16()?;
                let a_name = get_utf8_str(a_name_idx);
                let a_len = cursor.read_u32()? as usize;
                let a_data = cursor.read_bytes(a_len)?.to_vec();

                if a_name == "Code" {
                    if let Ok(parsed_code) = Self::parse_code_attribute(&a_data) {
                        code_attr = Some(parsed_code);
                    }
                }
                attrs.insert(a_name, a_data);
            }

            methods.push(MethodInfo {
                access_flags: m_flags,
                name: m_name,
                descriptor: m_desc,
                code: code_attr,
                attributes: attrs,
            });
        }

        // 6. Class Attributes
        let class_attr_count = cursor.read_u16()? as usize;
        let mut class_attrs = HashMap::with_capacity(class_attr_count);
        for _ in 0..class_attr_count {
            let a_name_idx = cursor.read_u16()?;
            let a_name = get_utf8_str(a_name_idx);
            let a_len = cursor.read_u32()? as usize;
            let a_data = cursor.read_bytes(a_len)?.to_vec();
            class_attrs.insert(a_name, a_data);
        }

        Ok(ClassFile {
            minor_version: minor,
            major_version: major,
            constant_pool: cp,
            access_flags,
            this_class_name,
            super_class_name,
            interface_names,
            fields,
            methods,
            attributes: class_attrs,
        })
    }

    fn parse_code_attribute(bytes: &[u8]) -> Result<CodeAttribute, String> {
        let mut cursor = Cursor::new(bytes);
        let max_stack = cursor.read_u16()?;
        let max_locals = cursor.read_u16()?;
        let code_len = cursor.read_u32()? as usize;
        let code_bytes = cursor.read_bytes(code_len)?.to_vec();

        let ex_count = cursor.read_u16()? as usize;
        let mut ex_table = Vec::with_capacity(ex_count);
        for _ in 0..ex_count {
            let start_pc = cursor.read_u16()?;
            let end_pc = cursor.read_u16()?;
            let handler_pc = cursor.read_u16()?;
            let catch_type = cursor.read_u16()?;
            ex_table.push(ExceptionEntry {
                start_pc,
                end_pc,
                handler_pc,
                catch_type,
            });
        }

        Ok(CodeAttribute {
            max_stack,
            max_locals,
            code: code_bytes,
            exception_table: ex_table,
        })
    }
}

/// Helper cursor for reading big-endian binary primitives.
struct Cursor<'a> {
    data: &'a [u8],
    pos: usize,
}

impl<'a> Cursor<'a> {
    fn new(data: &'a [u8]) -> Self {
        Self { data, pos: 0 }
    }

    fn read_bytes(&mut self, n: usize) -> Result<&'a [u8], String> {
        if self.pos + n > self.data.len() {
            return Err("Unexpected EOF while reading bytes".to_string());
        }
        let slice = &self.data[self.pos..self.pos + n];
        self.pos += n;
        Ok(slice)
    }

    fn read_u8(&mut self) -> Result<u8, String> {
        let slice = self.read_bytes(1)?;
        Ok(slice[0])
    }

    fn read_u16(&mut self) -> Result<u16, String> {
        let slice = self.read_bytes(2)?;
        Ok(u16::from_be_bytes([slice[0], slice[1]]))
    }

    fn read_u32(&mut self) -> Result<u32, String> {
        let slice = self.read_bytes(4)?;
        Ok(u32::from_be_bytes([slice[0], slice[1], slice[2], slice[3]]))
    }

    fn read_i32(&mut self) -> Result<i32, String> {
        let slice = self.read_bytes(4)?;
        Ok(i32::from_be_bytes([slice[0], slice[1], slice[2], slice[3]]))
    }

    fn read_i64(&mut self) -> Result<i64, String> {
        let slice = self.read_bytes(8)?;
        Ok(i64::from_be_bytes([
            slice[0], slice[1], slice[2], slice[3], slice[4], slice[5], slice[6], slice[7],
        ]))
    }

    fn read_u64(&mut self) -> Result<u64, String> {
        let slice = self.read_bytes(8)?;
        Ok(u64::from_be_bytes([
            slice[0], slice[1], slice[2], slice[3], slice[4], slice[5], slice[6], slice[7],
        ]))
    }
}
