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
- **Positive**: One implementation language, direct control over the APK pipeline, and no Gradle or Android SDK build-tools requirement.
- **Trade-off**: Requires implementing clean Kotlin-native encoders for DEX, binary XML, and APK signing.
- **Requirements**: The build host still needs Kotlin and JDK 21+. ADB is optional for building and required for automatic deployment; WiX is a maintainer dependency for MSI creation.
- **Current scope**: The encoders and APK v2 signing are implemented. General instruction translation and full interactive Android hosting remain incomplete. Sub-second execution is a goal, not a guaranteed property of this decision.
