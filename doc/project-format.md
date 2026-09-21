# 📦 Project Format Specification

## File Structure

A standard UI4 project generated via `kui new <name>`:

```text
hello/
├── kui.toml
├── src/
│   └── main.kt
├── assets/
│   ├── images/
│   └── fonts/
├── tests/
│   └── AppTest.kt
└── README.md
```

## `kui.toml` Format

```toml
[project]
name = "hello"
version = "0.1.0"
application_id = "com.example.hello"

[android]
min_sdk = 24
target_sdk = 36

[ui]
theme = "system"
```

### Configuration Sections
- `[project]`: Identity, human-readable name, semantic version, and Android application package ID.
- `[android]`: Minimum SDK constraint (default 24) and target compilation SDK (default 36).
- `[ui]`: Global styling options, default theme ("light", "dark", "system").
