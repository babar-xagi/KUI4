# 🧭 Learn UI4 in Five Concepts

UI4 is declarative: you describe the UI tree you want to construct using nested Kotlin functions.

You need basic Kotlin functions, variables, and lambdas. The beginner examples use the public `ui4.*` API.

## 1. An application contains a screen

```kotlin
import ui4.*

fun main() = app {
    screen {
        text("Hello, UI4!")
    }
}
```

Read it from the outside inward: create an application, create a screen, add text. A screen uses centered alignment by default, so a simple greeting does not need another centering container.

## 2. Containers arrange content

```kotlin
import ui4.*

fun main() = app {
    screen {
        column(gap = 12) {
            text("Welcome")
            text("Learn one concept at a time.")
        }
    }
}
```

A `column` arranges children vertically. A `row` arranges them horizontally. A `box` positions children within a shared area. Nest these functions to describe a larger UI tree.

These are JVM UI4 layout behaviors. The current Android text Activity does not reproduce this layout tree.

## 3. Named arguments make styling readable

```kotlin
import ui4.*

fun main() = app {
    screen(backgroundColor = Color.hex("#121212")) {
        text("A clear starting point", style = TextStyle.Headline)
    }
}
```

Learn presets such as `TextStyle.Headline` before custom typography. Use the documented `backgroundColor` form consistently; some aliases exist in the framework but are not equivalent in the experimental Android extractor.

## 4. Actions change state

This example demonstrates the JVM framework:

```kotlin
import ui4.*

fun main() = app {
    val count = state(0)

    screen {
        column(gap = 12) {
            text(count)
            button("Add one") {
                count.value++
            }
        }
    }
}
```

`state(0)` creates an observable value. `text(count)` binds a text node to it. The button callback changes the value.

Use `text(count)` for this binding. `text("Count: ${count.value}")` creates a string at construction time and does not automatically subscribe or recompose. See the [state examples](ui-components-and-styling.md#-state-and-input) for formatted bindings.

The generated Android Activity does not yet execute this counter's interaction.

## 5. Tests make behavior concrete

```kotlin
package tests

import ui4.*
import ui4.layout.ButtonNode
import ui4.layout.TextNode

fun main() {
    val count = state(0)
    lateinit var label: TextNode
    lateinit var addButton: ButtonNode

    app {
        screen {
            column(gap = 12) {
                label = text(count)
                addButton = button("Add one") { count.value++ }
            }
        }
    }

    addButton.click()
    check(count.value == 1)
    check(label.text == "1")
    println("Counter binding passed")
}
```

Put this in a project's `tests/CounterTest.kt` and run `kui test`. It checks the callback and text binding without requiring a phone.

## 📚 Continue learning

| You want to… | Read… |
| --- | --- |
| Control layout or typography | [Components and styling](ui-components-and-styling.md) |
| Find complete starter examples | [Cookbook](cookbook-and-examples.md) |
| Understand configuration | [Project guide](project-guide.md) |
| Add automated checks | [Testing](../testing.md) |
| See planned API simplifications | [Developer experience proposal](../developer/developer-experience.md) |