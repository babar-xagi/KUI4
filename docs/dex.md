# 📦 DEX Overview

KUI writes Android Dalvik Executable bytes directly in Kotlin.

`ClassFileReader` parses JVM classes. `ClassToDexCompiler` constructs DEX class and method models. `DexFileBuilder` writes identifier tables, class definitions, code/data items, MUTF-8 strings, ULEB128 values, the map, SHA-1 signature, and Adler-32 checksum.

The instruction translator is incomplete. General fields, branches, exception behavior, Kotlin runtime dependencies, and arbitrary method semantics are not preserved end to end. A structurally valid DEX does not prove application behavior.

The generated MainActivity calls Android framework methods to display literal UI text and supported styling in a TextView.

[Current DEX implementation](developer/kui-dex-internals.md) · [Testing](testing.md)