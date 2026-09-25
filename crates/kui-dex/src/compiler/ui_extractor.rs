use std::collections::BTreeSet;
use std::fs;
use std::path::{Path, PathBuf};

use crate::classfile::{ClassFile, CpInfo};

pub struct UiExtractor;

impl UiExtractor {
    /// Extracts display text defined in the user's project:
    /// 1. Scans Kotlin source files under `src/` for `text("...")` or `button("...")` declarations.
    /// 2. Inspects ConstantPool strings from compiled JVM `.class` files.
    /// 3. Falls back to a clean default application greeting.
    pub fn extract_ui_text(
        project_root: Option<&Path>,
        classes_dir: Option<&Path>,
        class_files: &[ClassFile],
        fallback_name: &str,
    ) -> String {
        // 1. Scan Kotlin sources under src/
        if let Some(text) = Self::scan_sources(project_root, classes_dir) {
            if !text.trim().is_empty() {
                return text;
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

        if !cp_strings.is_empty() {
            return cp_strings.join("\n\n");
        }

        // 3. Fallback
        format!(
            "Hello from {}!\n\nRunning on KUI Pure Kotlin Platform 🚀",
            fallback_name
        )
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

    fn scan_sources(project_root: Option<&Path>, classes_dir: Option<&Path>) -> Option<String> {
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

        let mut extracted: Vec<String> = Vec::new();
        let mut seen = BTreeSet::new();

        for file in kt_files {
            if let Ok(content) = fs::read_to_string(&file) {
                Self::extract_from_source(&content, &mut extracted, &mut seen);
            }
        }

        if !extracted.is_empty() {
            Some(extracted.join("\n\n"))
        } else {
            None
        }
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
        // Match: text("...") or button("...") or text(text = "...")
        // Simplified manual token search for robustness without heavy regex dependencies
        let keywords = ["text(", "button(", "text (", "button ("];
        for kw in &keywords {
            let mut search_idx = 0;
            while let Some(pos) = content[search_idx..].find(kw) {
                let start = search_idx + pos + kw.len();
                if let Some(quote_start_rel) = content[start..].find('"') {
                    let q_start = start + quote_start_rel + 1;
                    // Find closing quote
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
}
