# ⚙️ Compiler Pipeline

KUI invokes the standalone Kotlin compiler for Kotlin language semantics and JVM bytecode.

1. Validate `kui.toml`.
2. Discover Kotlin sources under `src/`.
3. Bootstrap the UI4 API JAR.
4. Compute a compiler cache key.
5. Invoke `kotlinc` with JVM target 21 and the UI4 classpath.
6. Read generated class files and run the Kotlin DEX/packaging pipeline.

Compiler failures stop the build. `--verbose` displays compiler details. Source argument files handle longer command lines and paths containing spaces.

Compilation to JVM bytecode does not establish Android support for arbitrary Kotlin logic. The KUI DEX translator is experimental, and the generated native Activity currently displays extracted text.

Use `build --clean` after changing assets because the current fast cache check does not include asset content.

[Compiler details](developer/toolchain-and-compiler.md) · [DEX internals](developer/kui-dex-internals.md)