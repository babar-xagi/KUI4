use crc32fast::Hasher as Crc32Hasher;
use flate2::write::DeflateEncoder;
use flate2::Compression;
use std::collections::BTreeMap;
use std::fs;
use std::io::Write;
use std::path::Path;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ApkEntry {
    pub name: String,
    pub data: Vec<u8>,
    pub compress: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ApkEntryInfo {
    pub name: String,
    pub compression_method: u16,
    pub compressed_size: u32,
    pub uncompressed_size: u32,
    pub local_header_offset: u32,
    pub data_offset: u32,
    pub is_aligned_4: bool,
}

pub struct ApkWriter {
    entries: Vec<ApkEntry>,
}

impl Default for ApkWriter {
    fn default() -> Self {
        Self::new()
    }
}

impl ApkWriter {
    pub fn new() -> Self {
        Self {
            entries: Vec::new(),
        }
    }

    pub fn add_entry(&mut self, name: impl Into<String>, data: Vec<u8>, compress: bool) -> &mut Self {
        self.entries.push(ApkEntry {
            name: name.into(),
            data,
            compress,
        });
        self
    }

    pub fn build(&self) -> Vec<u8> {
        let mut out = Vec::new();

        struct WrittenEntry<'a> {
            entry: &'a ApkEntry,
            local_header_offset: u32,
            compression_method: u16,
            crc32: u32,
            compressed_bytes: Vec<u8>,
            extra_field: Vec<u8>,
        }

        let mut written_entries = Vec::with_capacity(self.entries.len());

        for item in &self.entries {
            let name_bytes = item.name.as_bytes();
            let current_offset = out.len() as u32;

            let mut crc_hasher = Crc32Hasher::new();
            crc_hasher.update(&item.data);
            let crc_value = crc_hasher.finalize();

            let (compression_method, compressed_data) = if item.compress {
                let mut encoder = DeflateEncoder::new(Vec::new(), Compression::default());
                encoder.write_all(&item.data).expect("deflate write");
                let compressed = encoder.finish().expect("deflate finish");
                (8u16, compressed)
            } else {
                (0u16, item.data.clone())
            };

            // 4-byte data alignment calculation:
            // Data starts at: current_offset + 30 (header) + name_bytes.len() + extra_field.len()
            // If STORED (compression_method == 0), data_offset MUST be a multiple of 4.
            let base_header_size = 30 + (name_bytes.len() as u32);
            let extra_bytes = if compression_method == 0 {
                let unaligned_offset = current_offset + base_header_size;
                let remainder = (unaligned_offset % 4) as usize;
                let padding_needed = if remainder != 0 { 4 - remainder } else { 0 };
                vec![0u8; padding_needed]
            } else {
                Vec::new()
            };

            // Write Local File Header (30 bytes + name + extra + data)
            out.extend_from_slice(&0x04034b50u32.to_le_bytes()); // Local file header signature
            out.extend_from_slice(&20u16.to_le_bytes());         // Version needed to extract (2.0)
            out.extend_from_slice(&0u16.to_le_bytes());          // General purpose bit flag
            out.extend_from_slice(&compression_method.to_le_bytes()); // Compression method
            out.extend_from_slice(&0u16.to_le_bytes());          // Last mod file time
            out.extend_from_slice(&0x5421u16.to_le_bytes());     // Last mod file date (2022-01-01)
            out.extend_from_slice(&crc_value.to_le_bytes());     // CRC-32
            out.extend_from_slice(&(compressed_data.len() as u32).to_le_bytes()); // Compressed size
            out.extend_from_slice(&(item.data.len() as u32).to_le_bytes());       // Uncompressed size
            out.extend_from_slice(&(name_bytes.len() as u16).to_le_bytes());      // File name length
            out.extend_from_slice(&(extra_bytes.len() as u16).to_le_bytes());     // Extra field length
            out.extend_from_slice(name_bytes);                                    // File name
            out.extend_from_slice(&extra_bytes);                                  // Extra field (alignment padding)
            out.extend_from_slice(&compressed_data);                              // File data

            written_entries.push(WrittenEntry {
                entry: item,
                local_header_offset: current_offset,
                compression_method,
                crc32: crc_value,
                compressed_bytes: compressed_data,
                extra_field: extra_bytes,
            });
        }

        // Central Directory
        let cd_offset = out.len() as u32;
        for w in &written_entries {
            let name_bytes = w.entry.name.as_bytes();
            out.extend_from_slice(&0x02014b50u32.to_le_bytes()); // Central directory signature
            out.extend_from_slice(&20u16.to_le_bytes());         // Version made by
            out.extend_from_slice(&20u16.to_le_bytes());         // Version needed
            out.extend_from_slice(&0u16.to_le_bytes());          // Bit flag
            out.extend_from_slice(&w.compression_method.to_le_bytes()); // Compression method
            out.extend_from_slice(&0u16.to_le_bytes());          // Last mod time
            out.extend_from_slice(&0x5421u16.to_le_bytes());     // Last mod date
            out.extend_from_slice(&w.crc32.to_le_bytes());       // CRC-32
            out.extend_from_slice(&(w.compressed_bytes.len() as u32).to_le_bytes()); // Compressed size
            out.extend_from_slice(&(w.entry.data.len() as u32).to_le_bytes());       // Uncompressed size
            out.extend_from_slice(&(name_bytes.len() as u16).to_le_bytes());          // File name length
            out.extend_from_slice(&(w.extra_field.len() as u16).to_le_bytes());       // Extra field length
            out.extend_from_slice(&0u16.to_le_bytes());          // Comment length
            out.extend_from_slice(&0u16.to_le_bytes());          // Disk number start
            out.extend_from_slice(&0u16.to_le_bytes());          // Internal attributes
            out.extend_from_slice(&0u32.to_le_bytes());          // External attributes
            out.extend_from_slice(&w.local_header_offset.to_le_bytes()); // Local header offset
            out.extend_from_slice(name_bytes);
            out.extend_from_slice(&w.extra_field);
        }
        let cd_size = (out.len() as u32) - cd_offset;

        // End of Central Directory (EOCD)
        out.extend_from_slice(&0x06054b50u32.to_le_bytes()); // EOCD signature
        out.extend_from_slice(&0u16.to_le_bytes());          // Number of this disk
        out.extend_from_slice(&0u16.to_le_bytes());          // Disk where central directory starts
        out.extend_from_slice(&(written_entries.len() as u16).to_le_bytes()); // Records on disk
        out.extend_from_slice(&(written_entries.len() as u16).to_le_bytes()); // Total records
        out.extend_from_slice(&cd_size.to_le_bytes());       // Size of central directory
        out.extend_from_slice(&cd_offset.to_le_bytes());     // Offset of start of central directory
        out.extend_from_slice(&0u16.to_le_bytes());          // Comment length

        out
    }

    pub fn write_to_file(&self, path: &Path) -> std::io::Result<()> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::write(path, self.build())
    }

    /// Convenience helper to build an APK from standard Android components.
    pub fn build_apk(
        manifest_bytes: &[u8],
        dex_bytes: &[u8],
        assets: &BTreeMap<String, Vec<u8>>,
    ) -> Vec<u8> {
        let mut writer = ApkWriter::new();
        // AndroidManifest.xml must be STORED and uncompressed
        writer.add_entry("AndroidManifest.xml", manifest_bytes.to_vec(), false);
        // classes.dex must be STORED and 4-byte aligned for fast memory-mapping
        writer.add_entry("classes.dex", dex_bytes.to_vec(), false);

        for (asset_path, asset_data) in assets {
            let clean_path = asset_path.trim_start_matches('/');
            let full_path = if clean_path.starts_with("assets/") {
                clean_path.to_string()
            } else {
                format!("assets/{}", clean_path)
            };
            writer.add_entry(full_path, asset_data.clone(), false);
        }

        writer.build()
    }

    /// Inspects and lists all entries from an APK byte slice.
    pub fn list_entries(apk_bytes: &[u8]) -> Vec<ApkEntryInfo> {
        let mut result = Vec::new();
        let mut pos = 0;

        while pos + 30 <= apk_bytes.len() {
            let sig = u32::from_le_bytes(apk_bytes[pos..pos + 4].try_into().unwrap());
            if sig != 0x04034b50 {
                break; // Reached Central Directory or other block
            }

            let method = u16::from_le_bytes(apk_bytes[pos + 8..pos + 10].try_into().unwrap());
            let comp_size = u32::from_le_bytes(apk_bytes[pos + 18..pos + 22].try_into().unwrap());
            let uncomp_size = u32::from_le_bytes(apk_bytes[pos + 22..pos + 26].try_into().unwrap());
            let name_len = u16::from_le_bytes(apk_bytes[pos + 26..pos + 28].try_into().unwrap()) as usize;
            let extra_len = u16::from_le_bytes(apk_bytes[pos + 28..pos + 30].try_into().unwrap()) as usize;

            let name_start = pos + 30;
            let name_end = name_start + name_len;
            if name_end > apk_bytes.len() {
                break;
            }
            let name = String::from_utf8_lossy(&apk_bytes[name_start..name_end]).to_string();

            let data_offset = (pos + 30 + name_len + extra_len) as u32;
            let is_aligned = (data_offset % 4) == 0;

            result.push(ApkEntryInfo {
                name,
                compression_method: method,
                compressed_size: comp_size,
                uncompressed_size: uncomp_size,
                local_header_offset: pos as u32,
                data_offset,
                is_aligned_4: is_aligned,
            });

            pos = (data_offset + comp_size) as usize;
        }

        result
    }

    /// Verifies that all uncompressed (STORED) entries are strictly 4-byte memory aligned.
    pub fn verify_alignment(apk_bytes: &[u8]) -> bool {
        let entries = Self::list_entries(apk_bytes);
        if entries.is_empty() {
            return false;
        }
        for e in entries {
            if e.compression_method == 0 && !e.is_aligned_4 {
                return false;
            }
        }
        true
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_apk_writer_alignment() {
        let manifest = b"dummy manifest data";
        let dex = b"dex\n035\0dummy dex bytecode";
        let mut assets = BTreeMap::new();
        assets.insert("sub/test.txt".to_string(), b"hello world".to_vec());

        let apk_bytes = ApkWriter::build_apk(manifest, dex, &assets);
        assert!(apk_bytes.len() > 100);

        let entries = ApkWriter::list_entries(&apk_bytes);
        assert_eq!(entries.len(), 3);

        assert!(ApkWriter::verify_alignment(&apk_bytes));

        for e in &entries {
            if e.compression_method == 0 {
                assert!(e.is_aligned_4, "Entry {} must be 4-byte aligned", e.name);
                assert_eq!(e.data_offset % 4, 0);
            }
        }
    }
}
