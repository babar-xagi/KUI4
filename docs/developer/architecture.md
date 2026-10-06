# Kotlin architecture

```text
kui.bat / kui.ps1
  -> Java: kui.cli.MainKt (kui.jar)
  -> kui.toml validation + Kotlin compiler discovery
  -> UI4 API bootstrap + kotlinc application compilation
  -> ClassFileReader + ClassToDexCompiler + DexFileBuilder
  -> ManifestGenerator (binary AXML)
  -> ApkWriter (ZIP with alignment)
  -> ApkV2Signer + ApkV2Verifier (Java cryptography)
  -> app-debug.apk
  -> DeviceManager: ADB install + launch
```

The launcher compiles source distributions once and caches the platform JAR by input hashes. Packaged distributions ship `lib/kui.jar`. Application compilation and UI4 API caches live in the application project's `.kui` directory.

The DEX backend has limited bytecode translation. Its generated MainActivity presents extracted text in an Android TextView. Full application logic and the UI4 render/input loop require further Android integration. JVM UI4 tests exercise the framework independently of this generated Activity.