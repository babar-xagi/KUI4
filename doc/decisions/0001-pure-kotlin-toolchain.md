# ADR 0001: Pure Kotlin Toolchain Foundation

## Status
Accepted

## Context
Traditional Android application development relies on a complex stack of tools: Gradle, Android Gradle Plugin (AGP), AAPT2, D8/R8, Maven repositories, and extensive daemon processes. This leads to slow startup times, high memory consumption, fragile build scripts, and complex setup requirements.

## Decision
KUI4 will standardize entirely on:
- The official Kotlin language and compiler (`kotlinc`)
- The host JDK/JVM (Java 21)
- UI4/KUI codebase written 100% in Kotlin

All build execution, project generation, dependency coordination, bytecode inspection, DEX emission, packaging, and device commands will be implemented natively in Kotlin.

## Consequences
- **Positive**: Zero external build dependencies, sub-second execution, transparent error reporting, complete control over the toolchain.
- **Trade-off**: Requires implementing clean Kotlin-native encoders for DEX, binary XML, and APK signing.
