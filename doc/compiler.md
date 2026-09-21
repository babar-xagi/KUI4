# ⚙️ Compiler Pipeline

## Philosophy
KUI delegates Kotlin language semantics directly to the official Kotlin compiler (`kotlinc`), while owning the build graph, caching, classpath resolution, and DEX pipeline.

```text
src/*.kt + UI4 platform classes
               ↓
          kotlinc
               ↓
       JVM Class Files (.class)
               ↓
         KUI DEX Pipeline
               ↓
          classes.dex
```

## Compilation Phases
1. **Source Discovery**: Scans `src/` for `.kt` files.
2. **Classpath Assembly**: Includes UI4 runtime and platform stubs.
3. **Compiler Invocation**: Calls `kotlinc` with exact reproducible flags.
4. **Bytecode Analysis**: Inspects generated class files for entry points and DEX layout optimization.
