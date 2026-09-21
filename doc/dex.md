# 📦 DEX Architecture

## Overview
Android executes Dalvik Executable (DEX) bytecode rather than standard JVM classfiles. KUI implements a clean Kotlin-native DEX generation architecture to eliminate external dependencies on Google's D8/R8 tools.

## DEX File Structure
```text
┌───────────────────────────────┐
│          DEX Header           │ (Magic, Checksum, SHA-1, Section Offsets)
├───────────────────────────────┤
│        String Identifiers     │
├───────────────────────────────┤
│         Type Identifiers      │
├───────────────────────────────┤
│         Proto Identifiers     │
├───────────────────────────────┤
│         Field Identifiers     │
├───────────────────────────────┤
│        Method Identifiers     │
├───────────────────────────────┤
│        Class Definitions      │
├───────────────────────────────┤
│           Data Area           │ (Bytecode instructions, debug info, pools)
└───────────────────────────────┘
```

## Progressive Delivery
- **Phase 1-150**: Hybrid bootstrap verification using Android platform D8 tool when available for validation baselines.
- **Phase 151-180**: Self-contained pure Kotlin KUI DEX encoder writing byte-exact `classes.dex` directly.
