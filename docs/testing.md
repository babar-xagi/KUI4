# 🧪 Testing Strategy

## Philosophy
Every phase in the 180-phase roadmap must be independently testable without network connectivity or third-party test runners.

## Testing Tiers
1. **Unit Tests (`tests/`)**: Fast, pure Kotlin JVM tests validating parsers, algorithms, tree mutations, and data structures.
2. **Snapshot Tests**: Validates exact CLI outputs, help messages, and generated file templates.
3. **Integration Tests**: Tests end-to-end project building and byte-level archive validation.
4. **Android Platform Tests**: Validates rendering on Android hardware/emulators via ADB.
