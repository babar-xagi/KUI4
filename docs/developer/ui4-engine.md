# 🎛️ UI4 Runtime Architecture

UI4 is a Kotlin JVM framework for constructing and testing a UI tree. The current generated Android Activity does not host the complete engine.

## Tree and layout

`app` creates a `UiRoot`. `screen` sets its single root child. Container functions add nodes such as `ColumnNode`, `RowNode`, `BoxNode`, and `StackNode`.

Nodes implement:

- `measure(constraints): Size` to calculate a constrained size.
- `layout(origin, finalSize)` to place themselves and their children.

`UiRoot.performLayout(width, height)` starts the layout with fixed viewport constraints. Text nodes currently estimate metrics from font size and character count; they do not use native Android glyph shaping.

Modifiers describe padding, size, fill, background, weight, alpha, rounding, and clipping. Do not infer zero allocations or universal complexity guarantees from the two-pass design.

## ⚡ State and invalidation

`State` notifies subscribers when values change. Reading a value during tree construction does not implicitly register a subscriber.

`text(state)` calls `bindText`. `bindState` supports explicit updates to other node properties. A binding updates its node and marks its ancestor path dirty.

General automatic recomposition is not implemented. Dynamic lists or conditional subtrees require explicit updates rather than assuming every builder is reevaluated on a state change.

## 🎨 Rendering

`RenderPipeline` emits commands through `UiCanvas`: rectangles, rounded rectangles, text, clipping, opacity, translation, and save/restore.

`RecordingCanvas` stores commands in memory. `VirtualHost` coordinates layout, recorded rendering, and input for headless JVM checks.

Neither class opens a preview window. `AndroidHostBridge.onPlatformDraw` requests a host render but does not map those drawing commands to the supplied native Canvas. A complete Android graphics adapter remains necessary.

## 👆 Input and additional models

The JVM framework includes hit testing, focus, pointer and key dispatch, gesture recognizers, scrolling, editable text models, animations, navigation, and semantics.

These models are covered by JVM tests. Native keyboard/IME, display-frame scheduling, TalkBack integration, and full on-device interaction require additional platform work.

## 📱 Generated Android output

The APK compiler separately synthesizes a MainActivity with a native TextView. It extracts literal source text and supported background/alignment metadata. It does not start `VirtualHost`, invoke the application's complete UI4 main, or ship a finished renderer.

[Architecture](architecture.md) · [UI4 examples](../user/cookbook-and-examples.md) · [Testing](../testing.md)