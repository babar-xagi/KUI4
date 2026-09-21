# ♿ Accessibility Architecture

## Core Philosophy
In UI4, accessibility is treated as foundational architecture rather than an afterthought. Every UI node exposes semantic metadata:
- Roles (Button, Header, Checkbox, Text)
- Content descriptions
- Actions (Click, Scroll, Dismiss)
- State (Selected, Disabled, Focused)

## Bridge to Android OS
UI4 maps semantic tree nodes directly to `AccessibilityNodeInfo` objects inside the root platform View (`UI4RootView`), ensuring complete parity with screen readers like TalkBack without requiring native Android View widgets.
