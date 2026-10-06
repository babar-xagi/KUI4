# 🎨 UI4 Components and Styling

This guide describes the implemented UI4 JVM API. Full interactive Android hosting is still in development. The generated Android Activity currently displays extracted literal text and supported literal styling.

Start with the [learning path](learning-path.md) if these concepts are new.

## 🧱 Containers

| Function | JVM behavior |
| --- | --- |
| `screen { ... }` | Set the application's root screen; one active root child |
| `column { ... }` | Arrange children vertically |
| `row { ... }` | Arrange children horizontally |
| `box { ... }` | Position children within a box |
| `center { ... }` | A box with centered alignment and a fill-size modifier |
| `stack { ... }` | Overlay children in insertion order |
| `scroll { ... }` | Create a scroll container |

Use integer gaps for the beginner layout overloads:

```kotlin
import ui4.*

fun main() = app {
    screen {
        column(gap = 16, alignment = Alignment.CenterHorizontally) {
            text("Actions")
            row(gap = 8, alignment = Alignment.CenterVertically) {
                button("Cancel") { println("Cancelled") }
                button("Continue") { println("Continuing") }
            }
        }
    }
}
```

`gap`, padding, and sizes use the framework's numeric coordinate values. Android density conversion is not yet integrated into the complete UI4 host.

## ✍️ Text and buttons

`text(value)` creates a text node. Presets are `TextStyle.Headline`, `Title`, `Body`, and `Caption`.

```kotlin
import ui4.*

fun main() = app {
    screen {
        column(gap = 12) {
            text("Welcome", style = TextStyle.Headline)
            text(
                "A custom label",
                style = TextStyle(fontSize = 20f, fontWeight = FontWeight.Bold),
                color = Color.Blue
            )
            button("Continue", backgroundColor = Color.Blue, textColor = Color.White) {
                println("Continue clicked")
            }
        }
    }
}
```

Buttons return a `ButtonNode` and accept `enabled` and `onClick`. The JVM framework supports their callbacks; the generated Android text Activity does not provide live UI4 buttons.

## 🖌️ Modifiers and colors

Modifiers chain size, padding, background, opacity, rounding, clipping, and other node properties. Modifier extension functions currently need imports from `ui4.core`.

```kotlin
import ui4.*
import ui4.core.padding
import ui4.core.rounded
import ui4.core.size

fun main() = app {
    screen {
        box(
            modifier = Modifier.size(240f, 120f).padding(16).rounded(12),
            backgroundColor = Color.hex("#EDE9FE")
        ) {
            text("A simple card")
        }
    }
}
```

Use `Color.White`, `Color.Black`, `Color.Blue`, and other presets, or `Color.hex("#RRGGBB")` / `Color.hex("#AARRGGBB")`.

The Android extractor supports literal `backgroundColor = Color.Name`, `Color.hex("...")`, and `Color.parse("...")` expressions. It chooses contrast text color and supported literal alignment for its one TextView. It does not reproduce arbitrary modifier chains, per-node typography, or multiple independently styled widgets.

## ⚡ State and input

`state(value)` and `mutableStateOf(value)` create observable state.

- `text(state)` subscribes the text node to the state's value.
- `text("Value: ${state.value}")` evaluates a string once; it does not establish a subscription.
- `textField(state)` initializes its text from a mutable string state and forwards edits into that state. External state changes are not automatically rebound to the field.

For formatted reactive text, bind explicitly:

```kotlin
import ui4.*
import ui4.state.bindState

fun main() = app {
    val count = state(0)

    screen {
        column(gap = 12) {
            text("").bindState(count) { node, value ->
                node.text = "Count: $value"
            }
            button("Add one") { count.value++ }
        }
    }
}
```

For an editable field:

```kotlin
import ui4.*

fun main() = app {
    val name = state("")

    screen {
        column(gap = 12) {
            textField(name, placeholder = "Your name")
            text(name)
        }
    }
}
```

These examples construct JVM framework nodes. Rendering, keyboard integration, and touch interaction through the full Android UI4 host are not yet available.

## 🧩 Compose reusable UI

Define a container-scope extension to reuse a group of components:

```kotlin
import ui4.*

fun UI4ContainerScope.greetingCard(name: String) {
    box(padding = 16, backgroundColor = Color.White) {
        column(gap = 8) {
            text("Welcome", style = TextStyle.Title)
            text(name)
        }
    }
}

fun main() = app {
    screen {
        greetingCard("Developer")
    }
}
```

The API does not currently include `card`, `divider`, `spacer`, or `image` DSL functions. Use existing containers for composition; treat additional widgets as future work.

[Cookbook](cookbook-and-examples.md) · [JVM runtime architecture](../developer/ui4-engine.md)