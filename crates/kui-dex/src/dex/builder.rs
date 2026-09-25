use std::collections::{BTreeSet, HashMap};

use super::constants::*;
use super::model::*;
use super::writer::{finalize_dex_header, DexWriter};
use crate::leb128::write_uleb128;
use crate::mutf8::{encode_mutf8, utf16_length};

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
struct ProtoKey {
    return_type: String,
    parameters: Vec<String>,
    shorty: String,
}

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
struct FieldIdKey {
    class_desc: String,
    name: String,
    type_desc: String,
}

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
struct MethodIdKey {
    class_desc: String,
    name: String,
    proto: ProtoKey,
}

/// Pure Rust Dalvik Executable (DEX) file builder and serializer.
pub struct DexFileBuilder {
    classes: Vec<DexClass>,
}

impl DexFileBuilder {
    pub fn new() -> Self {
        Self {
            classes: Vec::new(),
        }
    }

    pub fn add_class(&mut self, dex_class: DexClass) {
        self.classes.push(dex_class);
    }

    pub fn build(self) -> Result<Vec<u8>, String> {
        // 1. Collect all unique strings
        let mut string_set = BTreeSet::new();
        // Mandatory types and descriptors
        string_set.insert("V".to_string());
        string_set.insert("Ljava/lang/Object;".to_string());

        for c in &self.classes {
            string_set.insert(c.class_descriptor.clone());
            string_set.insert(c.superclass_descriptor.clone());
            if let Some(ref sf) = c.source_file {
                string_set.insert(sf.clone());
            }
            for iface in &c.interface_descriptors {
                string_set.insert(iface.clone());
            }

            for f in c.static_fields.iter().chain(c.instance_fields.iter()) {
                string_set.insert(f.name.clone());
                string_set.insert(f.type_descriptor.clone());
            }

            for m in c.direct_methods.iter().chain(c.virtual_methods.iter()) {
                string_set.insert(m.name.clone());
                string_set.insert(m.return_type.clone());
                for p in &m.parameter_types {
                    string_set.insert(p.clone());
                }
                string_set.insert(compute_shorty(&m.return_type, &m.parameter_types));

                for fixup in &m.instruction_fixups {
                    match fixup {
                        DexInstructionFixup::MethodRef {
                            class_descriptor,
                            name,
                            return_type,
                            parameter_types,
                            ..
                        } => {
                            string_set.insert(class_descriptor.clone());
                            string_set.insert(name.clone());
                            string_set.insert(return_type.clone());
                            for p in parameter_types {
                                string_set.insert(p.clone());
                            }
                            string_set.insert(compute_shorty(return_type, parameter_types));
                        }
                        DexInstructionFixup::TypeRef {
                            type_descriptor, ..
                        } => {
                            string_set.insert(type_descriptor.clone());
                        }
                        DexInstructionFixup::StringRef { string, .. } => {
                            string_set.insert(string.clone());
                        }
                    }
                }
            }
        }

        // Sort strings according to MUTF-8 binary comparison per DEX spec
        let mut string_list: Vec<String> = string_set.into_iter().collect();
        string_list.sort_by(|a, b| {
            let a_mutf8 = encode_mutf8(a);
            let b_mutf8 = encode_mutf8(b);
            a_mutf8.cmp(&b_mutf8)
        });

        let mut string_map: HashMap<String, u32> = HashMap::with_capacity(string_list.len());
        for (idx, s) in string_list.iter().enumerate() {
            string_map.insert(s.clone(), idx as u32);
        }

        // 2. Collect unique types
        let mut type_set = BTreeSet::new();
        type_set.insert("V".to_string());
        type_set.insert("Ljava/lang/Object;".to_string());

        for c in &self.classes {
            type_set.insert(c.class_descriptor.clone());
            type_set.insert(c.superclass_descriptor.clone());
            for iface in &c.interface_descriptors {
                type_set.insert(iface.clone());
            }

            for f in c.static_fields.iter().chain(c.instance_fields.iter()) {
                type_set.insert(f.type_descriptor.clone());
                type_set.insert(f.class_descriptor.clone());
            }

            for m in c.direct_methods.iter().chain(c.virtual_methods.iter()) {
                type_set.insert(m.return_type.clone());
                type_set.insert(m.class_descriptor.clone());
                for p in &m.parameter_types {
                    type_set.insert(p.clone());
                }

                for fixup in &m.instruction_fixups {
                    match fixup {
                        DexInstructionFixup::MethodRef {
                            class_descriptor,
                            return_type,
                            parameter_types,
                            ..
                        } => {
                            type_set.insert(class_descriptor.clone());
                            type_set.insert(return_type.clone());
                            for p in parameter_types {
                                type_set.insert(p.clone());
                            }
                        }
                        DexInstructionFixup::TypeRef {
                            type_descriptor, ..
                        } => {
                            type_set.insert(type_descriptor.clone());
                        }
                        _ => {}
                    }
                }
            }
        }

        // Sort types by their descriptor string_idx ascending
        let mut type_list: Vec<String> = type_set.into_iter().collect();
        type_list.sort_by_key(|t| *string_map.get(t).unwrap_or(&0));

        let mut type_map: HashMap<String, u32> = HashMap::with_capacity(type_list.len());
        for (idx, t) in type_list.iter().enumerate() {
            type_map.insert(t.clone(), idx as u32);
        }

        // 3. Collect unique prototypes
        let mut proto_set = BTreeSet::new();
        for c in &self.classes {
            for m in c.direct_methods.iter().chain(c.virtual_methods.iter()) {
                proto_set.insert(ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                });

                for fixup in &m.instruction_fixups {
                    if let DexInstructionFixup::MethodRef {
                        return_type,
                        parameter_types,
                        ..
                    } = fixup
                    {
                        proto_set.insert(ProtoKey {
                            shorty: compute_shorty(return_type, parameter_types),
                            return_type: return_type.clone(),
                            parameters: parameter_types.clone(),
                        });
                    }
                }
            }
        }

        // Sort protos: return_type_idx ascending, then lexicographically by param type indices
        let mut proto_list: Vec<ProtoKey> = proto_set.into_iter().collect();
        proto_list.sort_by(|a, b| {
            let ret_a = type_map.get(&a.return_type).unwrap_or(&0);
            let ret_b = type_map.get(&b.return_type).unwrap_or(&0);
            if ret_a != ret_b {
                return ret_a.cmp(ret_b);
            }
            let params_a: Vec<u32> = a.parameters.iter().map(|p| *type_map.get(p).unwrap_or(&0)).collect();
            let params_b: Vec<u32> = b.parameters.iter().map(|p| *type_map.get(p).unwrap_or(&0)).collect();
            params_a.cmp(&params_b)
        });

        let mut proto_map: HashMap<ProtoKey, u32> = HashMap::with_capacity(proto_list.len());
        for (idx, p) in proto_list.iter().enumerate() {
            proto_map.insert(p.clone(), idx as u32);
        }

        // 4. Collect unique fields
        let mut field_set = BTreeSet::new();
        for c in &self.classes {
            for f in c.static_fields.iter().chain(c.instance_fields.iter()) {
                field_set.insert(FieldIdKey {
                    class_desc: f.class_descriptor.clone(),
                    name: f.name.clone(),
                    type_desc: f.type_descriptor.clone(),
                });
            }
        }

        // Sort fields: class_idx, name_idx, type_idx
        let mut field_list: Vec<FieldIdKey> = field_set.into_iter().collect();
        field_list.sort_by(|a, b| {
            let c_a = type_map.get(&a.class_desc).unwrap_or(&0);
            let c_b = type_map.get(&b.class_desc).unwrap_or(&0);
            if c_a != c_b {
                return c_a.cmp(c_b);
            }
            let n_a = string_map.get(&a.name).unwrap_or(&0);
            let n_b = string_map.get(&b.name).unwrap_or(&0);
            if n_a != n_b {
                return n_a.cmp(n_b);
            }
            let t_a = type_map.get(&a.type_desc).unwrap_or(&0);
            let t_b = type_map.get(&b.type_desc).unwrap_or(&0);
            t_a.cmp(t_b)
        });

        let mut field_map: HashMap<FieldIdKey, u32> = HashMap::with_capacity(field_list.len());
        for (idx, f) in field_list.iter().enumerate() {
            field_map.insert(f.clone(), idx as u32);
        }

        // 5. Collect unique method IDs
        let mut method_set = BTreeSet::new();
        for c in &self.classes {
            for m in c.direct_methods.iter().chain(c.virtual_methods.iter()) {
                let proto = ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                };
                method_set.insert(MethodIdKey {
                    class_desc: m.class_descriptor.clone(),
                    name: m.name.clone(),
                    proto,
                });

                for fixup in &m.instruction_fixups {
                    if let DexInstructionFixup::MethodRef {
                        class_descriptor,
                        name,
                        return_type,
                        parameter_types,
                        ..
                    } = fixup
                    {
                        let f_proto = ProtoKey {
                            shorty: compute_shorty(return_type, parameter_types),
                            return_type: return_type.clone(),
                            parameters: parameter_types.clone(),
                        };
                        method_set.insert(MethodIdKey {
                            class_desc: class_descriptor.clone(),
                            name: name.clone(),
                            proto: f_proto,
                        });
                    }
                }
            }
        }

        // Sort methods: class_idx, name_idx, proto_idx
        let mut method_list: Vec<MethodIdKey> = method_set.into_iter().collect();
        method_list.sort_by(|a, b| {
            let c_a = type_map.get(&a.class_desc).unwrap_or(&0);
            let c_b = type_map.get(&b.class_desc).unwrap_or(&0);
            if c_a != c_b {
                return c_a.cmp(c_b);
            }
            let n_a = string_map.get(&a.name).unwrap_or(&0);
            let n_b = string_map.get(&b.name).unwrap_or(&0);
            if n_a != n_b {
                return n_a.cmp(n_b);
            }
            let p_a = proto_map.get(&a.proto).unwrap_or(&0);
            let p_b = proto_map.get(&b.proto).unwrap_or(&0);
            p_a.cmp(p_b)
        });

        let mut method_map: HashMap<MethodIdKey, u32> = HashMap::with_capacity(method_list.len());
        for (idx, m) in method_list.iter().enumerate() {
            method_map.insert(m.clone(), idx as u32);
        }

        // 6. Calculate Header & Section Offsets
        let header_size = HEADER_SIZE;
        let string_ids_size = string_list.len() as u32;
        let string_ids_off = header_size;

        let type_ids_size = type_list.len() as u32;
        let type_ids_off = string_ids_off + (string_ids_size * 4);

        let proto_ids_size = proto_list.len() as u32;
        let proto_ids_off = type_ids_off + (type_ids_size * 4);

        let field_ids_size = field_list.len() as u32;
        let field_ids_off = proto_ids_off + (proto_ids_size * 12);

        let method_ids_size = method_list.len() as u32;
        let method_ids_off = field_ids_off + (field_ids_size * 8);

        let class_defs_size = self.classes.len() as u32;
        let class_defs_off = method_ids_off + (method_ids_size * 8);

        let data_start = class_defs_off + (class_defs_size * 32);

        // 7. Write Data Section (Strings data, Type lists, Code items, Class data)
        let mut data_out = DexWriter::new();

        // 7a. String data items
        let mut string_data_offsets = Vec::with_capacity(string_list.len());
        for s in &string_list {
            data_out.align(1);
            let off = data_start + data_out.len() as u32;
            string_data_offsets.push(off);

            let mut uleb = Vec::new();
            write_uleb128(&mut uleb, utf16_length(s));
            data_out.write_bytes(&uleb);

            let mutf8 = encode_mutf8(s);
            data_out.write_bytes(&mutf8);
            data_out.write_u8(0); // Null terminator
        }

        // 7b. Proto parameter type_lists
        let mut proto_params_offsets = Vec::with_capacity(proto_list.len());
        for pr in &proto_list {
            if pr.parameters.is_empty() {
                proto_params_offsets.push(0u32);
            } else {
                data_out.align(4);
                let off = data_start + data_out.len() as u32;
                proto_params_offsets.push(off);

                data_out.write_u32(pr.parameters.len() as u32);
                for p in &pr.parameters {
                    let t_idx = *type_map.get(p).unwrap_or(&0) as u16;
                    data_out.write_u16(t_idx);
                }
            }
        }

        // 7c. Class interfaces type_lists
        let mut class_interfaces_offsets = Vec::with_capacity(self.classes.len());
        for c in &self.classes {
            if c.interface_descriptors.is_empty() {
                class_interfaces_offsets.push(0u32);
            } else {
                data_out.align(4);
                let off = data_start + data_out.len() as u32;
                class_interfaces_offsets.push(off);

                data_out.write_u32(c.interface_descriptors.len() as u32);
                for iface in &c.interface_descriptors {
                    let t_idx = *type_map.get(iface).unwrap_or(&0) as u16;
                    data_out.write_u16(t_idx);
                }
            }
        }

        // 7d. Code items (methods bytecode)
        let mut method_code_offsets: HashMap<(*const DexClass, usize, bool), u32> = HashMap::new();

        for (_c_idx, c) in self.classes.iter().enumerate() {
            let class_ptr = c as *const DexClass;

            for (m_idx, m) in c.direct_methods.iter().enumerate() {
                data_out.align(4);
                let code_off = data_start + data_out.len() as u32;
                method_code_offsets.insert((class_ptr, m_idx, true), code_off);

                // Patch instructions with symbolic fixups
                let mut insns = m.instructions.clone();
                for fixup in &m.instruction_fixups {
                    match fixup {
                        DexInstructionFixup::MethodRef {
                            offset_in_instructions,
                            class_descriptor,
                            name,
                            return_type,
                            parameter_types,
                        } => {
                            let proto = ProtoKey {
                                shorty: compute_shorty(return_type, parameter_types),
                                return_type: return_type.clone(),
                                parameters: parameter_types.clone(),
                            };
                            let key = MethodIdKey {
                                class_desc: class_descriptor.clone(),
                                name: name.clone(),
                                proto,
                            };
                            let idx = *method_map.get(&key).ok_or_else(|| {
                                format!("Method ID not found for fixup: {:?}", key)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                        DexInstructionFixup::TypeRef {
                            offset_in_instructions,
                            type_descriptor,
                        } => {
                            let idx = *type_map.get(type_descriptor).ok_or_else(|| {
                                format!("Type ID not found for fixup: {}", type_descriptor)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                        DexInstructionFixup::StringRef {
                            offset_in_instructions,
                            string,
                        } => {
                            let idx = *string_map.get(string).ok_or_else(|| {
                                format!("String ID not found for fixup: {}", string)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                    }
                }

                data_out.write_u16(m.registers_size);
                data_out.write_u16(m.ins_size);
                data_out.write_u16(m.outs_size);
                data_out.write_u16(0); // tries_size
                data_out.write_u32(0); // debug_info_off
                data_out.write_u32(insns.len() as u32);
                for insn in insns {
                    data_out.write_u16(insn);
                }
            }

            for (m_idx, m) in c.virtual_methods.iter().enumerate() {
                data_out.align(4);
                let code_off = data_start + data_out.len() as u32;
                method_code_offsets.insert((class_ptr, m_idx, false), code_off);

                // Patch instructions with symbolic fixups
                let mut insns = m.instructions.clone();
                for fixup in &m.instruction_fixups {
                    match fixup {
                        DexInstructionFixup::MethodRef {
                            offset_in_instructions,
                            class_descriptor,
                            name,
                            return_type,
                            parameter_types,
                        } => {
                            let proto = ProtoKey {
                                shorty: compute_shorty(return_type, parameter_types),
                                return_type: return_type.clone(),
                                parameters: parameter_types.clone(),
                            };
                            let key = MethodIdKey {
                                class_desc: class_descriptor.clone(),
                                name: name.clone(),
                                proto,
                            };
                            let idx = *method_map.get(&key).ok_or_else(|| {
                                format!("Method ID not found for fixup: {:?}", key)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                        DexInstructionFixup::TypeRef {
                            offset_in_instructions,
                            type_descriptor,
                        } => {
                            let idx = *type_map.get(type_descriptor).ok_or_else(|| {
                                format!("Type ID not found for fixup: {}", type_descriptor)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                        DexInstructionFixup::StringRef {
                            offset_in_instructions,
                            string,
                        } => {
                            let idx = *string_map.get(string).ok_or_else(|| {
                                format!("String ID not found for fixup: {}", string)
                            })? as u16;
                            if *offset_in_instructions < insns.len() {
                                insns[*offset_in_instructions] = idx;
                            }
                        }
                    }
                }

                data_out.write_u16(m.registers_size);
                data_out.write_u16(m.ins_size);
                data_out.write_u16(m.outs_size);
                data_out.write_u16(0); // tries_size
                data_out.write_u32(0); // debug_info_off
                data_out.write_u32(insns.len() as u32);
                for insn in insns {
                    data_out.write_u16(insn);
                }
            }
        }

        // 7e. Class Data Items
        let mut class_data_offsets = Vec::with_capacity(self.classes.len());
        for c in &self.classes {
            if c.static_fields.is_empty()
                && c.instance_fields.is_empty()
                && c.direct_methods.is_empty()
                && c.virtual_methods.is_empty()
            {
                class_data_offsets.push(0u32);
                continue;
            }

            data_out.align(1);
            let class_data_off = data_start + data_out.len() as u32;
            class_data_offsets.push(class_data_off);

            let mut class_data_buf = Vec::new();
            write_uleb128(&mut class_data_buf, c.static_fields.len() as u32);
            write_uleb128(&mut class_data_buf, c.instance_fields.len() as u32);
            write_uleb128(&mut class_data_buf, c.direct_methods.len() as u32);
            write_uleb128(&mut class_data_buf, c.virtual_methods.len() as u32);

            // Static fields (sorted by field_idx ascending)
            let mut sorted_static_fields: Vec<&DexField> = c.static_fields.iter().collect();
            sorted_static_fields.sort_by_key(|f| {
                let key = FieldIdKey {
                    class_desc: f.class_descriptor.clone(),
                    name: f.name.clone(),
                    type_desc: f.type_descriptor.clone(),
                };
                *field_map.get(&key).unwrap_or(&0)
            });

            let mut last_field_idx = 0u32;
            for f in sorted_static_fields {
                let key = FieldIdKey {
                    class_desc: f.class_descriptor.clone(),
                    name: f.name.clone(),
                    type_desc: f.type_descriptor.clone(),
                };
                let f_idx = *field_map.get(&key).unwrap_or(&0);
                let diff = f_idx - last_field_idx;
                last_field_idx = f_idx;

                write_uleb128(&mut class_data_buf, diff);
                write_uleb128(&mut class_data_buf, f.access_flags);
            }

            // Instance fields (sorted by field_idx ascending)
            let mut sorted_instance_fields: Vec<&DexField> = c.instance_fields.iter().collect();
            sorted_instance_fields.sort_by_key(|f| {
                let key = FieldIdKey {
                    class_desc: f.class_descriptor.clone(),
                    name: f.name.clone(),
                    type_desc: f.type_descriptor.clone(),
                };
                *field_map.get(&key).unwrap_or(&0)
            });

            last_field_idx = 0u32;
            for f in sorted_instance_fields {
                let key = FieldIdKey {
                    class_desc: f.class_descriptor.clone(),
                    name: f.name.clone(),
                    type_desc: f.type_descriptor.clone(),
                };
                let f_idx = *field_map.get(&key).unwrap_or(&0);
                let diff = f_idx - last_field_idx;
                last_field_idx = f_idx;

                write_uleb128(&mut class_data_buf, diff);
                write_uleb128(&mut class_data_buf, f.access_flags);
            }

            // Direct methods (sorted by method_idx ascending)
            let class_ptr = c as *const DexClass;
            let mut sorted_direct_indices: Vec<usize> = (0..c.direct_methods.len()).collect();
            sorted_direct_indices.sort_by_key(|&m_idx| {
                let m = &c.direct_methods[m_idx];
                let proto = ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                };
                let key = MethodIdKey {
                    class_desc: m.class_descriptor.clone(),
                    name: m.name.clone(),
                    proto,
                };
                *method_map.get(&key).unwrap_or(&0)
            });

            let mut last_method_idx = 0u32;
            for m_idx in sorted_direct_indices {
                let m = &c.direct_methods[m_idx];
                let proto = ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                };
                let key = MethodIdKey {
                    class_desc: m.class_descriptor.clone(),
                    name: m.name.clone(),
                    proto,
                };
                let m_idx_val = *method_map.get(&key).unwrap_or(&0);
                let diff = m_idx_val - last_method_idx;
                last_method_idx = m_idx_val;

                let code_off = *method_code_offsets
                    .get(&(class_ptr, m_idx, true))
                    .unwrap_or(&0);

                write_uleb128(&mut class_data_buf, diff);
                write_uleb128(&mut class_data_buf, m.access_flags);
                write_uleb128(&mut class_data_buf, code_off);
            }

            // Virtual methods (sorted by method_idx ascending)
            let mut sorted_virtual_indices: Vec<usize> = (0..c.virtual_methods.len()).collect();
            sorted_virtual_indices.sort_by_key(|&m_idx| {
                let m = &c.virtual_methods[m_idx];
                let proto = ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                };
                let key = MethodIdKey {
                    class_desc: m.class_descriptor.clone(),
                    name: m.name.clone(),
                    proto,
                };
                *method_map.get(&key).unwrap_or(&0)
            });

            last_method_idx = 0u32;
            for m_idx in sorted_virtual_indices {
                let m = &c.virtual_methods[m_idx];
                let proto = ProtoKey {
                    shorty: compute_shorty(&m.return_type, &m.parameter_types),
                    return_type: m.return_type.clone(),
                    parameters: m.parameter_types.clone(),
                };
                let key = MethodIdKey {
                    class_desc: m.class_descriptor.clone(),
                    name: m.name.clone(),
                    proto,
                };
                let m_idx_val = *method_map.get(&key).unwrap_or(&0);
                let diff = m_idx_val - last_method_idx;
                last_method_idx = m_idx_val;

                let code_off = *method_code_offsets
                    .get(&(class_ptr, m_idx, false))
                    .unwrap_or(&0);

                write_uleb128(&mut class_data_buf, diff);
                write_uleb128(&mut class_data_buf, m.access_flags);
                write_uleb128(&mut class_data_buf, code_off);
            }

            data_out.write_bytes(&class_data_buf);
        }

        // 7f. Build and append map_list (0x1000)
        data_out.align(4);
        let map_off = data_start + data_out.len() as u32;

        #[derive(Debug, Clone)]
        struct MapItem {
            item_type: u16,
            size: u32,
            offset: u32,
        }

        let mut map_items = Vec::new();
        map_items.push(MapItem {
            item_type: TYPE_HEADER_ITEM,
            size: 1,
            offset: 0,
        });

        if string_ids_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_STRING_ID_ITEM,
                size: string_ids_size,
                offset: string_ids_off,
            });
        }
        if type_ids_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_TYPE_ID_ITEM,
                size: type_ids_size,
                offset: type_ids_off,
            });
        }
        if proto_ids_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_PROTO_ID_ITEM,
                size: proto_ids_size,
                offset: proto_ids_off,
            });
        }
        if field_ids_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_FIELD_ID_ITEM,
                size: field_ids_size,
                offset: field_ids_off,
            });
        }
        if method_ids_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_METHOD_ID_ITEM,
                size: method_ids_size,
                offset: method_ids_off,
            });
        }
        if class_defs_size > 0 {
            map_items.push(MapItem {
                item_type: TYPE_CLASS_DEF_ITEM,
                size: class_defs_size,
                offset: class_defs_off,
            });
        }

        // Data section items
        if !string_list.is_empty() {
            map_items.push(MapItem {
                item_type: TYPE_STRING_DATA_ITEM,
                size: string_list.len() as u32,
                offset: string_data_offsets[0],
            });
        }

        let mut all_type_lists: Vec<u32> = proto_params_offsets
            .iter()
            .copied()
            .chain(class_interfaces_offsets.iter().copied())
            .filter(|&off| off != 0)
            .collect();
        all_type_lists.sort_unstable();
        all_type_lists.dedup();

        if !all_type_lists.is_empty() {
            map_items.push(MapItem {
                item_type: TYPE_TYPE_LIST,
                size: all_type_lists.len() as u32,
                offset: all_type_lists[0],
            });
        }

        if !method_code_offsets.is_empty() {
            let mut code_offsets: Vec<u32> = method_code_offsets.values().copied().collect();
            code_offsets.sort_unstable();
            map_items.push(MapItem {
                item_type: TYPE_CODE_ITEM,
                size: code_offsets.len() as u32,
                offset: code_offsets[0],
            });
        }

        let non_zero_class_data: Vec<u32> = class_data_offsets
            .iter()
            .copied()
            .filter(|&off| off != 0)
            .collect();
        if !non_zero_class_data.is_empty() {
            map_items.push(MapItem {
                item_type: TYPE_CLASS_DATA_ITEM,
                size: non_zero_class_data.len() as u32,
                offset: non_zero_class_data[0],
            });
        }

        map_items.push(MapItem {
            item_type: TYPE_MAP_LIST,
            size: 1,
            offset: map_off,
        });

        map_items.sort_by_key(|it| it.offset);

        data_out.write_u32(map_items.len() as u32);
        for item in map_items {
            data_out.write_u16(item.item_type);
            data_out.write_u16(0); // unused
            data_out.write_u32(item.size);
            data_out.write_u32(item.offset);
        }

        let data_bytes = data_out.into_bytes();
        let total_file_size = data_start + data_bytes.len() as u32;

        // 8. Assemble Complete Binary Output
        let mut out = DexWriter::with_capacity(total_file_size as usize);

        // Header (112 bytes)
        out.write_bytes(DEX_FILE_MAGIC); // 8 bytes
        out.write_u32(0); // Adler-32 placeholder (bytes 8..11)
        out.write_bytes(&[0u8; 20]); // SHA-1 signature placeholder (bytes 12..31)
        out.write_u32(total_file_size);
        out.write_u32(HEADER_SIZE);
        out.write_u32(ENDIAN_CONSTANT);
        out.write_u32(0); // link_size
        out.write_u32(0); // link_off
        out.write_u32(map_off);
        out.write_u32(string_ids_size);
        out.write_u32(if string_ids_size == 0 { 0 } else { string_ids_off });
        out.write_u32(type_ids_size);
        out.write_u32(if type_ids_size == 0 { 0 } else { type_ids_off });
        out.write_u32(proto_ids_size);
        out.write_u32(if proto_ids_size == 0 { 0 } else { proto_ids_off });
        out.write_u32(field_ids_size);
        out.write_u32(if field_ids_size == 0 { 0 } else { field_ids_off });
        out.write_u32(method_ids_size);
        out.write_u32(if method_ids_size == 0 { 0 } else { method_ids_off });
        out.write_u32(class_defs_size);
        out.write_u32(if class_defs_size == 0 { 0 } else { class_defs_off });
        out.write_u32(data_bytes.len() as u32);
        out.write_u32(if data_bytes.is_empty() { 0 } else { data_start });

        assert_eq!(out.len(), 112);

        // Write String IDs
        for off in string_data_offsets {
            out.write_u32(off);
        }

        // Write Type IDs
        for t in &type_list {
            out.write_u32(*string_map.get(t).unwrap_or(&0));
        }

        // Write Proto IDs (12 bytes each)
        for (i, pr) in proto_list.iter().enumerate() {
            out.write_u32(*string_map.get(&pr.shorty).unwrap_or(&0));
            out.write_u32(*type_map.get(&pr.return_type).unwrap_or(&0));
            out.write_u32(proto_params_offsets[i]);
        }

        // Write Field IDs (8 bytes each)
        for f in &field_list {
            out.write_u16(*type_map.get(&f.class_desc).unwrap_or(&0) as u16);
            out.write_u16(*type_map.get(&f.type_desc).unwrap_or(&0) as u16);
            out.write_u32(*string_map.get(&f.name).unwrap_or(&0));
        }

        // Write Method IDs (8 bytes each)
        for m in &method_list {
            out.write_u16(*type_map.get(&m.class_desc).unwrap_or(&0) as u16);
            out.write_u16(*proto_map.get(&m.proto).unwrap_or(&0) as u16);
            out.write_u32(*string_map.get(&m.name).unwrap_or(&0));
        }

        // Write Class Defs (32 bytes each)
        for (i, c) in self.classes.iter().enumerate() {
            out.write_u32(*type_map.get(&c.class_descriptor).unwrap_or(&0));
            out.write_u32(c.access_flags);
            out.write_u32(*type_map.get(&c.superclass_descriptor).unwrap_or(&0));
            out.write_u32(class_interfaces_offsets[i]);
            let sf_idx = match &c.source_file {
                Some(sf) => *string_map.get(sf).unwrap_or(&NO_INDEX),
                None => NO_INDEX,
            };
            out.write_u32(sf_idx);
            out.write_u32(0); // annotations_off
            out.write_u32(class_data_offsets[i]);
            out.write_u32(0); // static_values_off
        }

        // Write Data Section
        out.write_bytes(&data_bytes);

        let mut final_bytes = out.into_bytes();
        assert_eq!(final_bytes.len(), total_file_size as usize);

        // 9. Compute and Patch SHA-1 and Adler-32 Checksums
        finalize_dex_header(&mut final_bytes);

        Ok(final_bytes)
    }
}
