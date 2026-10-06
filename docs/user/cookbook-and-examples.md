# 🍳 UI4 Examples

Each example below is complete Kotlin code. The Android example targets the generated text Activity. The other examples demonstrate the JVM framework and do not claim complete Android interaction.

## 👋 A greeting for the current Android backend

Save as `src/main.kt`, then run `kui build`.

```kotlin
import ui4.*

fun main() = app {
    screen(backgroundColor = Color.hex("#121212")) {
        text("Hello from KUI!")
    }
}
```

KUI extracts the literal greeting and supported background styling into the generated native Activity.

## 🧱 A reusable JVM component

```kotlin
import ui4.*

fun UI4ContainerScope.profileCard(name: String, role: String) {
    box(padding = 20, backgroundColor = Color.White) {
        column(gap = 8) {
            text(name, style = TextStyle.Title)
            text(role, style = TextStyle.Body)
        }
    }
}

fun main() = app {
    screen {
        profileCard("Alex", "Kotlin developer")
    }
}
```

The component is an ordinary Kotlin extension function. It introduces no additional template language.

## 🔢 Check a counter on the JVM

Save as `tests/CounterTest.kt` and run `kui test`.

```kotlin
package tests

import ui4.*
import ui4.layout.ButtonNode
import ui4.layout.TextNode

fun main() {
    val count = state(0)
    lateinit var label: TextNode
    lateinit var increment: ButtonNode

    app {
        screen {
            column(gap = 12) {
                label = text(count)
                increment = button("Increment") { count.value++ }
            }
        }
    }

    increment.click()
    increment.click()
    check(count.value == 2)
    check(label.text == "2")
    println("Counter example passed")
}
```

This validates observable state and callbacks without a device. It does not test Android touch delivery.

## 🖼️ Record a JVM render

Save as `tests/RenderTest.kt` and run `kui test`.

```kotlin
package tests

import ui4.*
import ui4.platform.android.VirtualHost
import ui4.render.RenderOp

fun main() {
    val root = app {
        screen {
            text("Preview check")
        }
    }

    val host = VirtualHost(viewportWidth = 360f, viewportHeight = 640f)
    host.attach(root)
    host.render(force = true)
    check(host.canvas.ops.any { it is RenderOp.DrawText && it.text == "Preview check" })
    host.detach()
    println("Render example passed")
}
```

`VirtualHost` records drawing commands in memory. It is not a desktop preview window or a native Android renderer.

## 📁 Repository examples

- [hello](../../examples/hello/README.md): starter greeting.
- [myaapp](../../examples/myaapp/README.md): two literal greeting strings.

Continue with [testing](../testing.md) or [component reference](ui-components-and-styling.md).