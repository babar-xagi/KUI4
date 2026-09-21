# KUI User Guide & Documentation

Welcome to the **KUI (Kotlin UI) User Guide**! KUI is a next-generation, pure Kotlin multiplatform application development framework that lets you build high-performance mobile applications without the complexity of Gradle, Android Gradle Plugin (AGP), or massive SDK dependencies.

---

## ⚡ Why KUI?

| Traditional Android Development | The KUI Experience |
| :--- | :--- |
| Requires 15GB+ Android Studio & SDKs | Requires only JDK 21 and `kotlinc` |
| 1,000+ lines of Gradle build scripts | Simple, clean `kui.toml` configuration |
| 30s – 2min cold build times | Instant sub-second to few-second builds |
| Fragile dependency resolution errors | Zero third-party build plugin breakage |
| Heavy Android View/Compose runtime | Lightweight, high-performance UI4 engine |
| 50MB+ minimum APK size | Ultra-lean APKs under a few megabytes |

---

## 📖 User Documentation Table of Contents

Follow the guides below to get started and master KUI application development:

1. **[Getting Started & Installation](getting-started.md)**  
   System prerequisites, installation steps, environment setup, and running `kui doctor`.

2. **[Project Creation & Anatomy](project-guide.md)**  
   Scaffolding projects with `kui new`, directory layout explanation, and understanding `kui.toml`, `src/main.kt`, and assets.

3. **[CLI Command Reference](cli-reference.md)**  
   Comprehensive guide to every CLI command: `kui new`, `kui build`, `kui run`, `kui test`, `kui clean`, `kui devices`, and `kui doctor`.

4. **[UI4 Components & Styling Reference](ui-components-and-styling.md)**  
   Complete guide to declarative UI building: layouts (`column`, `row`, `box`, `center`, `stack`), widgets (`text`, `button`, `textField`), reactive state (`mutableStateOf`), gestures, and animations.

5. **[Cookbook & Examples](cookbook-and-examples.md)**  
   Real-world code examples ready to run: Counter App, Todo List, Form Validation, Custom Card Dashboard, and deploying to physical Android phones.

---

## 🚀 60-Second Quick Start

```powershell
# 1. Verify your environment
kui doctor

# 2. Create a new project
kui new myfirstapp
cd myfirstapp

# 3. Build and launch on your connected phone or emulator
kui run
```
