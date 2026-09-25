use std::collections::BTreeSet;
use std::fs;
use std::path::{Path, PathBuf};

use crate::classfile::{ClassFile, CpInfo};

#[derive(Debug, Clone)]
pub struct UiMetadata {
    pub display_text: String,
    pub background_color: Option<u32>,
    pub text_color: Option<u32>,
    pub gravity: u32,
}

pub struct UiExtractor;

impl UiExtractor {
    /// Extracts full UI metadata (text, background color, text color, alignment)
    /// from project sources or compiled class files.
    pub fn extract_ui_metadata(
        project_root: Option<&Path>,
        classes_dir: Option<&Path>,
        class_files: &[ClassFile],
        fallback_name: &str,
    ) -> UiMetadata {
        // 1. Scan Kotlin sources under src/
        if let Some(meta) = Self::scan_sources(project_root, classes_dir) {
            if !meta.display_text.trim().is_empty() {
                return meta;
            }
        }

        // 2. Scan class files constant pools
        let mut cp_strings: Vec<String> = Vec::new();
        let mut seen = BTreeSet::new();

        for cf in class_files {
            for cp_entry in &cf.constant_pool {
                if let Some(CpInfo::StringCp(idx)) = cp_entry {
                    let s = cf.get_utf8(*idx);
                    if Self::is_user_string(s) && seen.insert(s.to_string()) {
                        cp_strings.push(s.to_string());
                    }
                }
            }
        }

        let display_text = if !cp_strings.is_empty() {
            cp_strings.join("\n\n")
        } else {
            format!(
                "Hello from {}!\n\nRunning on KUI Pure Kotlin Platform 🚀",
                fallback_name
            )
        };

        UiMetadata {
            display_text,
            background_color: None,
            text_color: None,
            gravity: 17, // Gravity.CENTER
        }
    }

    /// Backwards compatible helper returning only display text.
    pub fn extract_ui_text(
        project_root: Option<&Path>,
        classes_dir: Option<&Path>,
        class_files: &[ClassFile],
        fallback_name: &str,
    ) -> String {
        Self::extract_ui_metadata(project_root, classes_dir, class_files, fallback_name).display_text
    }

    fn is_user_string(s: &str) -> bool {
        let trimmed = s.trim();
        if trimmed.is_empty() {
            return false;
        }
        if trimmed.starts_with("$this$")
            || trimmed.starts_with("kotlin/")
            || trimmed.starts_with("Lkotlin")
            || trimmed.starts_with('$')
            || trimmed == "INSTANCE"
            || trimmed == "<init>"
            || trimmed == "<clinit>"
            || trimmed.ends_with(".kt")
            || trimmed.ends_with(".class")
        {
            return false;
        }
        true
    }

    fn scan_sources(project_root: Option<&Path>, classes_dir: Option<&Path>) -> Option<UiMetadata> {
        let mut candidate_src_dirs: Vec<PathBuf> = Vec::new();

        if let Some(root) = project_root {
            candidate_src_dirs.push(root.join("src"));
        }
        if let Some(cd) = classes_dir {
            if let Some(p1) = cd.parent() {
                if let Some(p2) = p1.parent() {
                    candidate_src_dirs.push(p2.join("src"));
                }
                candidate_src_dirs.push(p1.join("src"));
            }
            candidate_src_dirs.push(cd.join("src"));
        }

        let src_dir = candidate_src_dirs.into_iter().find(|d| d.is_dir())?;
        let mut kt_files: Vec<PathBuf> = Vec::new();
        Self::collect_kt_files(&src_dir, &mut kt_files);

        let mut extracted_texts: Vec<String> = Vec::new();
        let mut seen = BTreeSet::new();
        let mut bg_color: Option<u32> = None;
        let mut gravity: u32 = 17; // Gravity.CENTER

        for file in kt_files {
            if let Ok(content) = fs::read_to_string(&file) {
                Self::extract_from_source(&content, &mut extracted_texts, &mut seen);

                if bg_color.is_none() {
                    bg_color = Self::parse_background_color(&content);
                }

                if let Some(g) = Self::parse_alignment(&content) {
                    gravity = g;
                }
            }
        }

        if extracted_texts.is_empty() {
            return None;
        }

        // Compute text color for contrast if background is explicitly set
        let text_color = bg_color.map(|bg| {
            let r = ((bg >> 16) & 0xFF) as f32;
            let g = ((bg >> 8) & 0xFF) as f32;
            let b = (bg & 0xFF) as f32;
            let luminance = 0.299 * r + 0.587 * g + 0.114 * b;
            if luminance > 128.0 {
                0xFF111827 // Dark Slate / Charcoal for light background
            } else {
                0xFFF9FAFB // Clean Light Gray / White for dark background
            }
        });

        Some(UiMetadata {
            display_text: extracted_texts.join("\n\n"),
            background_color: bg_color,
            text_color,
            gravity,
        })
    }

    fn collect_kt_files(dir: &Path, files: &mut Vec<PathBuf>) {
        if let Ok(entries) = fs::read_dir(dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_dir() {
                    Self::collect_kt_files(&path, files);
                } else if path.extension().map_or(false, |ext| ext == "kt") {
                    files.push(path);
                }
            }
        }
    }

    fn extract_from_source(
        content: &str,
        extracted: &mut Vec<String>,
        seen: &mut BTreeSet<String>,
    ) {
        let keywords = ["text(", "button(", "text (", "button ("];
        for kw in &keywords {
            let mut search_idx = 0;
            while let Some(pos) = content[search_idx..].find(kw) {
                let start = search_idx + pos + kw.len();
                if let Some(quote_start_rel) = content[start..].find('"') {
                    let q_start = start + quote_start_rel + 1;
                    let mut q_end = q_start;
                    let bytes = content.as_bytes();
                    while q_end < bytes.len() {
                        if bytes[q_end] == b'"' && (q_end == 0 || bytes[q_end - 1] != b'\\') {
                            break;
                        }
                        q_end += 1;
                    }
                    if q_end < bytes.len() {
                        let raw = &content[q_start..q_end];
                        let unescaped = raw
                            .replace("\\n", "\n")
                            .replace("\\t", "\t")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                        let trimmed = unescaped.trim().to_string();
                        if !trimmed.is_empty() && seen.insert(trimmed.clone()) {
                            extracted.push(trimmed);
                        }
                    }
                    search_idx = q_end + 1;
                } else {
                    search_idx = start;
                }
            }
        }
    }

    fn parse_background_color(content: &str) -> Option<u32> {
        let lower = content.to_lowercase();
        // Check Color.* constants
        if lower.contains("color.white") || lower.contains("\"white\"") {
            return Some(0xFFFFFFFF);
        }
        if lower.contains("color.black") || lower.contains("\"black\"") {
            return Some(0xFF000000);
        }
        if lower.contains("color.red") || lower.contains("\"red\"") {
            return Some(0xFFFF0000);
        }
        if lower.contains("color.green") || lower.contains("\"green\"") {
            return Some(0xFF00FF00);
        }
        if lower.contains("color.blue") || lower.contains("\"blue\"") {
            return Some(0xFF0000FF);
        }
        if lower.contains("color.yellow") || lower.contains("\"yellow\"") {
            return Some(0xFFFFFF00);
        }
        if lower.contains("color.cyan") || lower.contains("\"cyan\"") {
            return Some(0xFF00FFFF);
        }
        if lower.contains("color.purple") || lower.contains("\"purple\"") {
            return Some(0xFF8B5CF6);
        }
        if lower.contains("color.orange") || lower.contains("\"orange\"") {
            return Some(0xFFF97316);
        }
        if lower.contains("color.teal") || lower.contains("\"teal\"") {
            return Some(0xFF14B8A6);
        }
        if lower.contains("color.pink") || lower.contains("\"pink\"") {
            return Some(0xFFEC4899);
        }
        if lower.contains("color.gray") || lower.contains("\"gray\"") {
            return Some(0xFF888888);
        }

        // Check hex colors like "#121212" or "Color.hex(\"#...\")"
        if let Some(pos) = content.find('#') {
            let start = pos + 1;
            let end = content[start..]
                .find(|c: char| !c.is_ascii_hexdigit())
                .map(|p| start + p)
                .unwrap_or(content.len());
            let hex_str = &content[start..end];
            if hex_str.len() == 6 {
                if let Ok(rgb) = u32::from_str_radix(hex_str, 16) {
                    return Some(0xFF000000 | rgb);
                }
            } else if hex_str.len() == 8 {
                if let Ok(argb) = u32::from_str_radix(hex_str, 16) {
                    return Some(argb);
                }
            }
        }

        None
    }

    fn parse_alignment(content: &str) -> Option<u32> {
        let lower = content.to_lowercase();
        if lower.contains("topstart") || lower.contains("start") {
            Some(51) // Gravity.LEFT (3) | Gravity.TOP (48)
        } else if lower.contains("topcenter") || lower.contains("centerhorizontally") || lower.contains("top") {
            Some(49) // Gravity.CENTER_HORIZONTAL (1) | Gravity.TOP (48)
        } else if lower.contains("bottomcenter") || lower.contains("bottom") {
            Some(81) // Gravity.CENTER_HORIZONTAL (1) | Gravity.BOTTOM (80)
        } else if lower.contains("center") {
            Some(17) // Gravity.CENTER
        } else {
            None
        }
    }
}
