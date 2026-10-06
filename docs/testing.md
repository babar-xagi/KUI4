# 🧪 Testing Guide

KUI runs standalone Kotlin JVM test programs. Tests need no Android SDK or phone unless you are explicitly performing device verification.

## Project tests

Create `tests/GreetingTest.kt`:

```kotlin
package com.example.myapp

fun main() {
    val greeting = "Hello, KUI!"
    check(greeting.startsWith("Hello"))
    check(greeting.isNotBlank())
    println("Greeting checks passed")
}
```

Run `kui test` inside the project. KUI compiles `src/` and `tests/`, then runs test entry points in separate JVM processes with assertions enabled. The application main is not executed as a test.

Use top-level `main` functions. `@file:JvmName` is supported. JUnit annotations are not discovered.

A test fails when compilation fails, a check throws, or its process exits nonzero. A generated project's starter test only prints a placeholder message; replace it with real checks.

## UI4 behavior

See the [counter](user/cookbook-and-examples.md#-check-a-counter-on-the-jvm) and [recorded-render](user/cookbook-and-examples.md#-record-a-jvm-render) examples.

Direct callback and tree checks validate the JVM framework. They do not verify Android touch, keyboard, screen-reader, or rendering integration.

## Repository verification

At the repository root:

```powershell
.\kui.bat test
```

For suite logs:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test_all.ps1
```

Logs are saved in `.kui/verification`. Select a suite with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test_all.ps1 -Suite CompilerDriverTest
```

The script compiles the platform once and uses Kotlin friend-module access for internal framework checks. Existing milestone scripts select their corresponding suites.

## Coverage and its limits

The last full run on 2026-10-05 passed 18 programs and 575 checks covering configuration, project generation, CLI behavior, compiler integration, UI4 layout/state/input, DEX structure, APK alignment, v2 signing, and tamper detection.

Negative tests deliberately produce errors such as invalid Kotlin or failed test subprocesses. The overall suite result determines whether those expected failures were handled correctly.

For release 0.04, a generated greeting APK also passed install, launch, and visible-text checks on a TECNO BG7. This verifies the native text Activity, not the complete interactive UI4 Android runtime.
