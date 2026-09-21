# KUI Project Guide & Structure

This guide explains how to create a new KUI project and details the role of every file and directory generated inside a project.

---

## 📦 Creating a New Project

To create a new KUI project, run `kui new` followed by your desired project name:
```powershell
kui new myawesomeapp
```

The generator will scaffold a complete, clean project structure:
```
[KUI] Creating project 'myawesomeapp'...
[KUI] Created myawesomeapp/kui.toml
[KUI] Created myawesomeapp/src/main.kt
[KUI] Created myawesomeapp/tests/AppTest.kt
[KUI] Created myawesomeapp/assets/fonts/
[KUI] Created myawesomeapp/assets/images/
[KUI] Project 'myawesomeapp' created successfully!
```

Navigate into your new project directory:
```powershell
cd myawesomeapp
```

---

## 🗂️ Project Directory Layout

```
myawesomeapp/
├── kui.toml            # Project configuration and metadata
├── src/
│   └── main.kt         # Application entry point and UI tree
├── tests/
│   └── AppTest.kt      # Unit and UI tests
├── assets/             # Bundled static resources
│   ├── fonts/          # TrueType / OpenType font files (.ttf, .otf)
│   └── images/         # Static images and icons (.png, .svg, .webp)
├── .gitignore          # Version control ignore rules
└── .kui/               # [Generated] Local build cache and compiled artifacts
```

---

## 📄 File Purposes & Configuration

### 1. `kui.toml` (Project Configuration)
`kui.toml` is the single source of truth for your project. No XML manifests or Groovy scripts are needed.

```toml
[project]
name = "myawesomeapp"
version = "0.1.0"
applicationId = "com.example.myawesomeapp"

[android]
minSdk = 26             # Android 8.0 Oreo
targetSdk = 34          # Android 14

[build]
optimization = "speed"
incremental = true
```

| Section | Key | Type | Description |
| :--- | :--- | :--- | :--- |
| `[project]` | `name` | String | Human-readable app name displayed on the device launcher. |
| `[project]` | `version` | String | Semantic version string (e.g. `"1.0.0"`). |
| `[project]` | `applicationId` | String | Unique Android package identifier (e.g. `"com.company.app"`). |
| `[android]` | `minSdk` | Integer | Minimum Android API level supported (default: `26`). |
| `[android]` | `targetSdk` | Integer | Target Android API level for runtime behaviors (default: `34`). |
| `[build]` | `incremental` | Boolean | Enables high-speed SHA-256 caching for instant builds. |

---

### 2. `src/main.kt` (Application Entry Point)
`src/main.kt` contains the `main()` function defining your application's declarative user interface:

```kotlin
import ui4.*

fun main() = app {
    screen {
        center {
            column(gap = 16) {
                text("Hello World 👋", style = TextStyle.Headline)
                text("Built with Pure Kotlin KUI! 🚀", style = TextStyle.Body)
            }
        }
    }
}
```

- `app { ... }`: The root application container. Sets up the display surface and theme context.
- `screen { ... }`: Defines a top-level screen or route within the viewport.
- `center { ... }`: A layout node that centers its child both horizontally and vertically.
- `column(gap = 16) { ... }`: A linear layout stacking elements vertically with 16dp spacing.
- `text(...)`: Displays formatted, styled text.

---

### 3. `tests/AppTest.kt` (Application Tests)
KUI includes a test harness allowing you to test UI logic without an emulator:

```kotlin
import ui4.platform.android.VirtualHost
import ui4.tree.UiRoot

fun testAppScreen() {
    val host = VirtualHost(viewportWidth = 1080f, viewportHeight = 1920f)
    // Verify layout and behavior
    assert(host.viewportWidth == 1080f)
}
```
Run tests anytime using `kui test`.

---

### 4. `assets/` (Static Assets)
Any files placed inside `assets/` are automatically packaged into the final APK under the `assets/` archive directory:
- **`assets/fonts/`**: Store custom fonts. Accessible by name in UI4 text styles.
- **`assets/images/`**: Store application icons, logos, and PNG/JPEG graphics.

---

### 5. `.kui/` (Build Artifacts & Cache)
Generated automatically by the KUI build engine. Ignored by git:
- `.kui/build/classes/`: JVM `.class` bytecode output from `kotlinc`.
- `.kui/build/dex/`: Dalvik Executable (`classes.dex`).
- `.kui/cache/`: Hashes used by the incremental build system.
- `build/outputs/apk/debug/app-debug.apk`: The final signed, 4-byte aligned Android package.
