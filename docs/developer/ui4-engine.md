# UI4 Runtime Engine Architecture

UI4 is the declarative user interface engine powering KUI applications. Inspired by modern declarative paradigms (Flutter, Jetpack Compose, SwiftUI), UI4 provides a pure Kotlin, hardware-accelerated rendering and layout pipeline with zero runtime dependencies on Android View/ViewGroup hierarchies.

---

## 📐 The 2-Pass Layout System (`measure` & `layout`)

UI4 implements a strictly bounded, 2-pass layout protocol that guarantees linear time complexity $O(N)$ with zero layout recursion thrashing.

```
                  ┌────────────────────────────────────────┐
                  │              Parent Node               │
                  └───────┬────────────────────────▲───────┘
                          │                        │
       Pass 1: Constraints│                        │Pass 1: Desired Size
       (minW, maxW,       │                        │(width, height)
        minH, maxH)       ▼                        │
                  ┌────────────────────────────────┴───────┐
                  │               Child Node               │
                  └───────┬────────────────────────▲───────┘
                          │                        │
       Pass 2: Placement  │                        │Pass 2: Laid Out
       (x, y,             │                        │(bounds, transform)
        finalW, finalH)   ▼                        │
                  ┌────────────────────────────────┴───────┐
                  │             Grandchild Node            │
                  └────────────────────────────────────────┘
```

### Pass 1: Measure (`measure(constraints: Constraints): Size`)
- Parent nodes pass `Constraints(minWidth, maxWidth, minHeight, maxHeight)` down to their children.
- Children measure their contents (e.g. text glyph metrics, child counts, padding) and return their desired `Size(width, height)` satisfying the incoming constraints.
- **Caching:** If a node's incoming constraints have not changed and the node is not marked dirty, the previously computed measurement is returned in $O(1)$ time.

### Pass 2: Layout (`layout(bounds: Rect)`)
- After children report their desired sizes, the parent applies its layout algorithm (e.g., vertical stacking in `Column`, horizontal distribution in `Row`, centering in `Center`).
- Parent computes the final offset `(x, y)` and assigns the `Rect(x, y, width, height)` bounds to each child.

---

## ⚡ Reactive State & Dirty Tracking (`State.kt` & `DirtyTracking.kt`)

UI4 uses fine-grained reactivity to ensure minimal redraws and layout passes.

### How Reactive State Works:
1. State is created using `mutableStateOf(initialValue)`:
   ```kotlin
   val count = mutableStateOf(0)
   ```
2. When a node's builder reads `count.value`, the node is registered as an observer of that state.
3. When `count.value = count.value + 1` is written:
   - The state notifies its observer nodes.
   - Nodes mark themselves as `isDirty = true`.
   - The dirty flag propagates up to `UiRoot`.
   - The layout pass is scheduled for the next frame.
4. **Selective Invalidation:** Clean subtrees with `isDirty == false` are skipped during both measurement and layout passes.

---

## 🎨 Rendering Pipeline & Canvas Abstraction (`RenderPipeline.kt`)

UI4 decouples layout from platform-specific graphics backends through the `UiCanvas` interface.

```
                  ┌───────────────────────┐
                  │    UiRoot (Clean)     │
                  └──────────┬────────────┘
                             │
                             ▼
                  ┌───────────────────────┐
                  │    RenderPipeline     │
                  └──────────┬────────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
┌───────────────────────┐         ┌───────────────────────┐
│   RecordingCanvas     │         │ Platform Graphics     │
│   (Headless Tester &  │         │ (Hardware Skia /      │
│    Display List)      │         │  Android Canvas)      │
└───────────────────────┘         └───────────────────────┘
```

### `UiCanvas` Interface Contract:
- `drawRect(rect: Rect, paint: Paint)`
- `drawText(text: String, origin: Point, style: TextStyle)`
- `drawImage(image: Any, rect: Rect)`
- `clipRect(rect: Rect)`
- `save()` / `restore()`
- `translate(dx: Float, dy: Float)`

### `RecordingCanvas`:
For headless verification, unit testing, and CI pipelines, `RecordingCanvas` records every draw command into an in-memory display list. Tests can inspect drawn shapes, colors, bounds, and text strings without spinning up a display server.

---

## 🎯 Input & Focus System (`InputManager.kt` & `FocusManager.kt`)

UI4 features a unified input routing system handling touch, mouse, and keyboard input.

### Hit Testing & Pointer Dispatch:
1. When a `PointerEvent` (`Down`, `Move`, `Up`, `Cancel`) arrives at `UiHost`:
2. `InputManager` performs hit testing by traversing the tree in reverse paint order ($Z$-order).
3. The deepest interactive node containing the event point receives the event.
4. If the event is consumed (`consumed = true`), propagation stops.

### Focus Management:
- `FocusManager` maintains a reference to the active `FocusNode`.
- Handles tab key traversal (`Tab` / `Shift+Tab`) across focusable elements (buttons, text fields).
- Handles focus gain, focus loss, and virtual keyboard dispatch.

---

## 🎬 Animation Subsystem (`Animation.kt`)

UI4 includes frame-based physics and value interpolation:
- **Tweens:** Linear, EaseIn, EaseOut, EaseInOut, and Cubic bezier curves.
- **Springs:** Mass, stiffness, and damping ratio for fluid physics.
- **Lerp Utilities:** Linear interpolation for `Float`, `Int`, `Color`, `Point`, and `Rect`.
- **Clock:** Driven by 60Hz/90Hz/120Hz platform display refresh ticks.

---

## 🌐 Accessibility & Semantics (`Semantics.kt`)

Every UI4 node can expose semantic properties:
- **Roles:** `Button`, `Image`, `TextField`, `Header`, `Checkbox`, `Switch`.
- **Labels:** Spoken text for screen readers (TalkBack / accessibility tools).
- **Actions:** Click, scroll, dismiss, set text.
- Accessibility tools traverse the semantic tree independently of visual layout decorations.
