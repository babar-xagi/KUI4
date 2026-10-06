# ♿ Accessibility Status

UI4 includes JVM semantics models for roles, descriptions, state, and actions. Framework tests can inspect and traverse this semantic tree independently of the visual layout.

A finished Android AccessibilityNodeInfo/TalkBack adapter for the complete UI4 tree is not implemented. The current generated Activity uses a native TextView; it does not expose a live UI4 semantic hierarchy.

The next platform work should validate focus order, labels, actions, and state changes on Android with assistive technology. JVM semantics checks alone are not evidence of complete screen-reader support.

[Runtime architecture](developer/ui4-engine.md) · [Testing](testing.md)