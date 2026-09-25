use std::collections::HashMap;

pub mod constants {
    pub const RES_XML_TYPE: u16 = 0x0003;
    pub const RES_STRING_POOL_TYPE: u16 = 0x0001;
    pub const RES_XML_RESOURCE_MAP_TYPE: u16 = 0x0180;
    pub const RES_XML_START_NAMESPACE_TYPE: u16 = 0x0100;
    pub const RES_XML_END_NAMESPACE_TYPE: u16 = 0x0101;
    pub const RES_XML_START_ELEMENT_TYPE: u16 = 0x0102;
    pub const RES_XML_END_ELEMENT_TYPE: u16 = 0x0103;

    // TypedValue types
    pub const TYPE_NULL: u8 = 0x00;
    pub const TYPE_REFERENCE: u8 = 0x01;
    pub const TYPE_STRING: u8 = 0x03;
    pub const TYPE_INT_DEC: u8 = 0x10;
    pub const TYPE_INT_BOOLEAN: u8 = 0x12;

    // Standard Android Attribute Resource IDs
    pub const ATTR_LABEL: u32 = 0x01010001;
    pub const ATTR_NAME: u32 = 0x01010003;
    pub const ATTR_EXPORTED: u32 = 0x01010010;
    pub const ATTR_MIN_SDK_VERSION: u32 = 0x0101020c;
    pub const ATTR_VERSION_CODE: u32 = 0x0101021b;
    pub const ATTR_VERSION_NAME: u32 = 0x0101021c;
    pub const ATTR_TARGET_SDK_VERSION: u32 = 0x01010270;
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AxmlAttribute {
    pub uri_index: i32,
    pub name_index: i32,
    pub value_string_index: i32,
    pub val_type: u8,
    pub data: i32,
}

#[derive(Default, Debug, Clone)]
pub struct AxmlBuffer {
    data: Vec<u8>,
}

impl AxmlBuffer {
    pub fn new() -> Self {
        Self { data: Vec::new() }
    }

    pub fn len(&self) -> usize {
        self.data.len()
    }

    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    pub fn as_bytes(&self) -> &[u8] {
        &self.data
    }

    pub fn into_bytes(self) -> Vec<u8> {
        self.data
    }

    pub fn write_u8(&mut self, val: u8) {
        self.data.push(val);
    }

    pub fn write_u16(&mut self, val: u16) {
        self.data.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_i32(&mut self, val: i32) {
        self.data.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_u32(&mut self, val: u32) {
        self.data.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_bytes(&mut self, bytes: &[u8]) {
        self.data.extend_from_slice(bytes);
    }

    pub fn align4(&mut self) {
        let remainder = self.data.len() % 4;
        if remainder != 0 {
            for _ in 0..(4 - remainder) {
                self.data.push(0);
            }
        }
    }
}

pub struct AxmlWriter {
    strings: Vec<String>,
    string_map: HashMap<String, usize>,
    resource_ids: HashMap<usize, u32>,
}

impl Default for AxmlWriter {
    fn default() -> Self {
        Self::new()
    }
}

impl AxmlWriter {
    pub fn new() -> Self {
        Self {
            strings: Vec::new(),
            string_map: HashMap::new(),
            resource_ids: HashMap::new(),
        }
    }

    pub fn get_string_index(&mut self, s: &str) -> i32 {
        if let Some(&idx) = self.string_map.get(s) {
            idx as i32
        } else {
            let idx = self.strings.len();
            self.strings.push(s.to_string());
            self.string_map.insert(s.to_string(), idx);
            idx as i32
        }
    }

    pub fn set_resource_id(&mut self, string_index: i32, res_id: u32) {
        if string_index >= 0 {
            self.resource_ids.insert(string_index as usize, res_id);
        }
    }

    pub fn build<F>(&mut self, generator: F) -> Vec<u8>
    where
        F: FnOnce(&mut AxmlWriter, &mut AxmlBuffer),
    {
        let mut body_buf = AxmlBuffer::new();
        generator(self, &mut body_buf);
        let body_bytes = body_buf.into_bytes();

        // 1. Build String Pool Chunk (0x0001)
        let mut string_data = AxmlBuffer::new();
        let mut string_offsets = Vec::with_capacity(self.strings.len());

        for s in &self.strings {
            string_offsets.push(string_data.len() as u32);
            let utf16_chars: Vec<u16> = s.encode_utf16().collect();
            string_data.write_u16(utf16_chars.len() as u16);
            for code in utf16_chars {
                string_data.write_u16(code);
            }
            string_data.write_u16(0); // Null terminator
        }
        string_data.align4();

        let string_pool_header_size: u16 = 28;
        let string_pool_total_size: u32 = (string_pool_header_size as u32)
            + ((self.strings.len() as u32) * 4)
            + (string_data.len() as u32);

        let mut string_pool = AxmlBuffer::new();
        string_pool.write_u16(constants::RES_STRING_POOL_TYPE);
        string_pool.write_u16(string_pool_header_size);
        string_pool.write_u32(string_pool_total_size);
        string_pool.write_u32(self.strings.len() as u32);
        string_pool.write_u32(0); // style_count
        string_pool.write_u32(0); // flags (0 = UTF-16)
        string_pool.write_u32(
            (string_pool_header_size as u32) + ((self.strings.len() as u32) * 4),
        );
        string_pool.write_u32(0); // styles_start

        for off in string_offsets {
            string_pool.write_u32(off);
        }
        string_pool.write_bytes(string_data.as_bytes());
        let string_pool_bytes = string_pool.into_bytes();

        // 2. Build Resource Map Chunk (0x0180)
        let res_map_bytes = if !self.resource_ids.is_empty() {
            let max_idx = self.resource_ids.keys().copied().max().unwrap_or(0);
            let count = max_idx + 1;
            let mut res_map = AxmlBuffer::new();
            let res_map_header_size: u16 = 8;
            let res_map_total_size: u32 = (res_map_header_size as u32) + ((count as u32) * 4);

            res_map.write_u16(constants::RES_XML_RESOURCE_MAP_TYPE);
            res_map.write_u16(res_map_header_size);
            res_map.write_u32(res_map_total_size);
            for i in 0..count {
                let id = self.resource_ids.get(&i).copied().unwrap_or(0);
                res_map.write_u32(id);
            }
            res_map.into_bytes()
        } else {
            Vec::new()
        };

        // 3. Assemble Final AXML Chunk (0x0003)
        let total_file_size: u32 = 8
            + (string_pool_bytes.len() as u32)
            + (res_map_bytes.len() as u32)
            + (body_bytes.len() as u32);

        let mut final_buf = AxmlBuffer::new();
        final_buf.write_u16(constants::RES_XML_TYPE);
        final_buf.write_u16(8); // header_size
        final_buf.write_u32(total_file_size);
        final_buf.write_bytes(&string_pool_bytes);
        if !res_map_bytes.is_empty() {
            final_buf.write_bytes(&res_map_bytes);
        }
        final_buf.write_bytes(&body_bytes);

        final_buf.into_bytes()
    }
}

pub struct ManifestGenerator;

impl ManifestGenerator {
    pub fn generate_binary_manifest(
        package_name: &str,
        version_code: i32,
        version_name: &str,
        min_sdk: i32,
        target_sdk: i32,
        app_label: &str,
    ) -> Vec<u8> {
        let mut writer = AxmlWriter::new();

        writer.build(|w, out| {
            let ns_uri = "http://schemas.android.com/apk/res/android";
            let ns_prefix = "android";

            let ns_uri_idx = w.get_string_index(ns_uri);
            let ns_prefix_idx = w.get_string_index(ns_prefix);
            let empty_ns_idx = -1;

            // Pre-register tag strings
            let manifest_idx = w.get_string_index("manifest");
            let uses_sdk_idx = w.get_string_index("uses-sdk");
            let application_idx = w.get_string_index("application");
            let activity_idx = w.get_string_index("activity");
            let intent_filter_idx = w.get_string_index("intent-filter");
            let action_idx = w.get_string_index("action");
            let category_idx = w.get_string_index("category");

            // Pre-register attribute strings
            let package_idx = w.get_string_index("package");
            let version_code_idx = w.get_string_index("versionCode");
            let version_name_idx = w.get_string_index("versionName");
            let min_sdk_idx = w.get_string_index("minSdkVersion");
            let target_sdk_idx = w.get_string_index("targetSdkVersion");
            let label_idx = w.get_string_index("label");
            let name_idx = w.get_string_index("name");
            let exported_idx = w.get_string_index("exported");

            // Register standard Android resource IDs in Resource Map Chunk (0x0180)
            w.set_resource_id(version_code_idx, constants::ATTR_VERSION_CODE);
            w.set_resource_id(version_name_idx, constants::ATTR_VERSION_NAME);
            w.set_resource_id(min_sdk_idx, constants::ATTR_MIN_SDK_VERSION);
            w.set_resource_id(target_sdk_idx, constants::ATTR_TARGET_SDK_VERSION);
            w.set_resource_id(label_idx, constants::ATTR_LABEL);
            w.set_resource_id(name_idx, constants::ATTR_NAME);
            w.set_resource_id(exported_idx, constants::ATTR_EXPORTED);

            // Values
            let pkg_val_idx = w.get_string_index(package_name);
            let ver_name_val_idx = w.get_string_index(version_name);
            let label_val_idx = w.get_string_index(app_label);
            let act_name_val_idx = w.get_string_index(".MainActivity");
            let main_action_val_idx = w.get_string_index("android.intent.action.MAIN");
            let launcher_cat_val_idx = w.get_string_index("android.intent.category.LAUNCHER");
            let vc_val_idx = w.get_string_index(&version_code.to_string());
            let min_sdk_val_idx = w.get_string_index(&min_sdk.to_string());
            let target_sdk_val_idx = w.get_string_index(&target_sdk.to_string());
            let exported_val_idx = w.get_string_index("true");

            let write_start_namespace = |out: &mut AxmlBuffer| {
                out.write_u16(constants::RES_XML_START_NAMESPACE_TYPE);
                out.write_u16(16); // header_size
                out.write_u32(24); // chunk_size
                out.write_u32(0);  // line_number
                out.write_i32(-1); // comment
                out.write_i32(ns_prefix_idx);
                out.write_i32(ns_uri_idx);
            };

            let write_end_namespace = |out: &mut AxmlBuffer| {
                out.write_u16(constants::RES_XML_END_NAMESPACE_TYPE);
                out.write_u16(16);
                out.write_u32(24);
                out.write_u32(0);
                out.write_i32(-1);
                out.write_i32(ns_prefix_idx);
                out.write_i32(ns_uri_idx);
            };

            let write_start_element =
                |out: &mut AxmlBuffer, ns_idx: i32, name_idx: i32, attrs: &[AxmlAttribute]| {
                    let header_size: u16 = 16;
                    let attr_start: u16 = 20;
                    let attr_size: u16 = 20;
                    let total_size: u32 =
                        (header_size as u32) + 20 + ((attrs.len() as u32) * (attr_size as u32));

                    out.write_u16(constants::RES_XML_START_ELEMENT_TYPE);
                    out.write_u16(header_size);
                    out.write_u32(total_size);
                    out.write_u32(0);  // line_number
                    out.write_i32(-1); // comment
                    out.write_i32(ns_idx);
                    out.write_i32(name_idx);
                    out.write_u16(attr_start);
                    out.write_u16(attr_size);
                    out.write_u16(attrs.len() as u16);
                    out.write_u16(0); // id_index
                    out.write_u16(0); // class_index
                    out.write_u16(0); // style_index

                    for attr in attrs {
                        out.write_i32(attr.uri_index);
                        out.write_i32(attr.name_index);
                        out.write_i32(attr.value_string_index);
                        out.write_u16(8); // typed_value.size = 8
                        out.write_u8(0);  // res0 = 0
                        out.write_u8(attr.val_type);
                        out.write_i32(attr.data);
                    }
                };

            let write_end_element = |out: &mut AxmlBuffer, ns_idx: i32, name_idx: i32| {
                out.write_u16(constants::RES_XML_END_ELEMENT_TYPE);
                out.write_u16(16);
                out.write_u32(24);
                out.write_u32(0);
                out.write_i32(-1);
                out.write_i32(ns_idx);
                out.write_i32(name_idx);
            };

            // 1. Start Namespace
            write_start_namespace(out);

            // 2. <manifest package="..." android:versionCode="..." android:versionName="...">
            write_start_element(
                out,
                empty_ns_idx,
                manifest_idx,
                &[
                    AxmlAttribute {
                        uri_index: empty_ns_idx,
                        name_index: package_idx,
                        value_string_index: pkg_val_idx,
                        val_type: constants::TYPE_STRING,
                        data: pkg_val_idx,
                    },
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: version_code_idx,
                        value_string_index: vc_val_idx,
                        val_type: constants::TYPE_INT_DEC,
                        data: version_code,
                    },
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: version_name_idx,
                        value_string_index: ver_name_val_idx,
                        val_type: constants::TYPE_STRING,
                        data: ver_name_val_idx,
                    },
                ],
            );

            // 3. <uses-sdk android:minSdkVersion="..." android:targetSdkVersion="..."/>
            write_start_element(
                out,
                empty_ns_idx,
                uses_sdk_idx,
                &[
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: min_sdk_idx,
                        value_string_index: min_sdk_val_idx,
                        val_type: constants::TYPE_INT_DEC,
                        data: min_sdk,
                    },
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: target_sdk_idx,
                        value_string_index: target_sdk_val_idx,
                        val_type: constants::TYPE_INT_DEC,
                        data: target_sdk,
                    },
                ],
            );
            write_end_element(out, empty_ns_idx, uses_sdk_idx);

            // 4. <application android:label="...">
            write_start_element(
                out,
                empty_ns_idx,
                application_idx,
                &[AxmlAttribute {
                    uri_index: ns_uri_idx,
                    name_index: label_idx,
                    value_string_index: label_val_idx,
                    val_type: constants::TYPE_STRING,
                    data: label_val_idx,
                }],
            );

            // 5. <activity android:name=".MainActivity" android:exported="true">
            write_start_element(
                out,
                empty_ns_idx,
                activity_idx,
                &[
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: name_idx,
                        value_string_index: act_name_val_idx,
                        val_type: constants::TYPE_STRING,
                        data: act_name_val_idx,
                    },
                    AxmlAttribute {
                        uri_index: ns_uri_idx,
                        name_index: exported_idx,
                        value_string_index: exported_val_idx,
                        val_type: constants::TYPE_INT_BOOLEAN,
                        data: -1, // true in Android resource binary XML
                    },
                ],
            );

            // 6. <intent-filter>
            write_start_element(out, empty_ns_idx, intent_filter_idx, &[]);

            // <action android:name="android.intent.action.MAIN"/>
            write_start_element(
                out,
                empty_ns_idx,
                action_idx,
                &[AxmlAttribute {
                    uri_index: ns_uri_idx,
                    name_index: name_idx,
                    value_string_index: main_action_val_idx,
                    val_type: constants::TYPE_STRING,
                    data: main_action_val_idx,
                }],
            );
            write_end_element(out, empty_ns_idx, action_idx);

            // <category android:name="android.intent.category.LAUNCHER"/>
            write_start_element(
                out,
                empty_ns_idx,
                category_idx,
                &[AxmlAttribute {
                    uri_index: ns_uri_idx,
                    name_index: name_idx,
                    value_string_index: launcher_cat_val_idx,
                    val_type: constants::TYPE_STRING,
                    data: launcher_cat_val_idx,
                }],
            );
            write_end_element(out, empty_ns_idx, category_idx);

            // </intent-filter>
            write_end_element(out, empty_ns_idx, intent_filter_idx);

            // </activity>
            write_end_element(out, empty_ns_idx, activity_idx);

            // </application>
            write_end_element(out, empty_ns_idx, application_idx);

            // </manifest>
            write_end_element(out, empty_ns_idx, manifest_idx);

            // End Namespace
            write_end_namespace(out);
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_manifest_generator_header() {
        let manifest = ManifestGenerator::generate_binary_manifest(
            "com.example.test",
            1,
            "1.0.0",
            24,
            36,
            "TestApp",
        );

        assert!(manifest.len() > 100);
        // First 2 bytes must be RES_XML_TYPE = 0x0003
        assert_eq!(manifest[0], 0x03);
        assert_eq!(manifest[1], 0x00);
        // Header size = 8
        assert_eq!(manifest[2], 0x08);
        assert_eq!(manifest[3], 0x00);
        // Total size = manifest.len()
        let total_size = u32::from_le_bytes(manifest[4..8].try_into().unwrap());
        assert_eq!(total_size as usize, manifest.len());

        // String pool chunk header at offset 8
        assert_eq!(manifest[8], 0x01); // RES_STRING_POOL_TYPE
        assert_eq!(manifest[9], 0x00);
    }
}
