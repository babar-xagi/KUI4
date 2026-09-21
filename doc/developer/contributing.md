# KUI Contributor & Development Guide

Thank you for contributing to KUI! This guide covers everything you need to set up your environment, build the platform, run tests, and contribute code.

---

## 💻 Prerequisites

To develop on the KUI platform codebase itself, you only need:
1. **JDK 21 or higher:** (OpenJDK, Temurin, or Oracle JDK). Ensure `JAVA_HOME` is set and `java` is on your `PATH`.
2. **Kotlin Compiler (`kotlinc`) 2.0+:** Standalone Kotlin compiler on your `PATH`.
3. **Android Debug Bridge (`adb`) (Optional):** Needed only for testing APK installation and execution on physical hardware or emulators.

*Note: No Gradle, no Android Studio, and no Android SDK build-tools are required to compile or test KUI!*

---

## 🚀 Building the Platform CLI

The KUI CLI is self-bootstrapping. The root directory contains `kui.ps1` (PowerShell) and `kui.bat` (Windows Command Prompt).

When you run `kui` for the first time or after modifying any `.kt` file under `platform/`:
```powershell
.\kui.bat doctor
```
`kui.ps1` automatically detects changed source files in `platform/kui` or `platform/ui4`, re-compiles `.kui/build/kui.jar` with `kotlinc`, and executes the command!

To force a full clean rebuild of the platform:
```powershell
Remove-Item .kui\build\kui.jar -Force
.\kui.bat doctor
```

---

## 🧪 Running the Milestone Test Suites

All test batteries in KUI are standalone Kotlin scripts located in `scripts/`:

| Milestone | Script | Test Battery Focus |
| :--- | :--- | :--- |
| **Milestone A** | `scripts/test_milestone_a.bat` | Repository bootstrap, directory structure, config parser |
| **Milestone B** | `scripts/test_milestone_b.bat` | Compiler driver, AST, classfile verification |
| **Milestone C** | `scripts/test_milestone_c.bat` | Incremental caching, SHA-256 hashing, build benchmarks |
| **Milestone D** | `scripts/test_milestone_d.bat` | UI4 core layout, node trees, constraints, dirty tracking |
| **Milestone E** | `scripts/test_milestone_e.bat` | Reactive state bindings, gestures, text layout, animation |
| **Milestone F** | `scripts/test_milestone_f.bat` | Virtual host surface, focus tree, input dispatch, semantics |
| **Milestone G** | `scripts/test_milestone_g.bat` | Pure DEX compiler, AXML writer, APK v2 signer, ADB launcher (78 tests) |

### Executing Milestone G Tests:
```powershell
.\scripts\test_milestone_g.bat
```
Expected output:
```
===========================================================
Milestone G Test Results: 78 PASSED, 0 FAILED
===========================================================
[KUI4] Milestone G Verification: ALL PASS
```

---

## 📱 Testing on a Physical Android Phone

1. Enable **Developer Options** and **USB Debugging** on your phone.
2. Connect your phone via USB.
3. Verify connection:
   ```powershell
   adb devices -l
   ```
4. Navigate to any example project:
   ```powershell
   cd examples\myaapp
   ..\..\kui.bat run
   ```
5. Monitor logcat in a separate terminal:
   ```powershell
   adb logcat -s ActivityTaskManager AndroidRuntime com.example.myaapp
   ```

---

## 📐 Coding Conventions & Guidelines

1. **Zero External Build Tooling:** Never introduce Gradle dependencies or Gradle wrapper files. All build logic belongs in `platform/kui/`.
2. **Pure Kotlin / Standard Library Only:** Rely on standard Java/Kotlin library packages (`java.io`, `java.nio`, `java.security`, `java.util.zip`). Avoid third-party heavy JAR dependencies.
3. **Immutability First:** Data classes and immutable collections (`List`, `Map`, `Set`) preferred unless performing tight binary serialization loops.
4. **Comprehensive Regression Tests:** Every new phase or bug fix must include corresponding assertions in `tests/ToolchainAndApkTest.kt` or `tests/UI4*.kt`.
5. **Clear Commit Messages:** Follow standard conventional commits format:
   - `feat(dex): ...`
   - `fix(axml): ...`
   - `docs(user): ...`
   - `test(ui4): ...`
