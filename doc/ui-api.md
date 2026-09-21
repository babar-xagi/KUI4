# 🎨 UI4 Declarative API Specification

## Target Source Example

```kotlin
import ui4.*

fun main() = app {
    screen {
        column(
            gap = 16,
            alignment = Alignment.Center
        ) {
            text("Welcome 👋", style = TextStyle.Headline)

            button("Continue") {
                navigate(Profile)
            }
        }
    }
}
```

## Core Primitives
- `screen { ... }`: The root visual container bound to the display window.
- `column(gap, alignment) { ... }`: Vertical linear layout.
- `row(gap, alignment) { ... }`: Horizontal linear layout.
- `box(alignment) { ... }`: Layered stack layout.
- `text(value, style)`: Typography rendering primitive.
- `button(label, onClick)`: Interactive action component.
- `spacer(size)`: Layout dimension filler.

## State Management
```kotlin
val count = state(0)

button("Increase") {
    count.value++
}
```
Mutations to `count.value` mark observing nodes as dirty, triggering minimal relayout and redraw without re-evaluating the entire tree.
