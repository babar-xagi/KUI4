# KUI Code Cookbook & Real-World Examples

This cookbook contains complete, production-ready code examples demonstrating how to build interactive applications with KUI and UI4.

---

## 🍳 Recipe 1: Hello Babar Greeting App

The exact application verified on physical hardware (`TECNO_BG7`):

```kotlin
// src/main.kt
import ui4.*

fun main() = app {
    screen {
        center {
            column(gap = 16, alignment = Alignment.CenterHorizontally) {
                text("Hello Babar 👋", style = TextStyle.Headline)
                text(
                    "Running on KUI4 Pure Kotlin Platform! 🚀",
                    style = TextStyle.Body.copy(color = Color(0xFF475569.toInt()))
                )
            }
        }
    }
}
```

---

## 🍳 Recipe 2: Interactive Counter with Dynamic Styling

A reactive state counter where text color dynamically transitions when reaching thresholds:

```kotlin
// src/main.kt
import ui4.*

fun main() = app {
    val counter = mutableStateOf(0)

    screen {
        center {
            box(
                padding = 24f,
                backgroundColor = Color(0xFFF8FAFC.toInt())
            ) {
                column(gap = 20, alignment = Alignment.CenterHorizontally) {
                    text("Interactive Counter", style = TextStyle.Headline)

                    val statusColor = when {
                        counter.value > 0 -> Color(0xFF16A34A.toInt()) // Green
                        counter.value < 0 -> Color(0xFFDC2626.toInt()) // Red
                        else -> Color(0xFF64748B.toInt())              // Gray
                    }

                    text(
                        "${counter.value}",
                        style = TextStyle(
                            fontSize = 48f,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )

                    row(gap = 12) {
                        button("Decrement (-)", backgroundColor = Color(0xFFEF4444.toInt())) {
                            counter.value--
                        }
                        button("Reset (0)", backgroundColor = Color(0xFF6B7280.toInt())) {
                            counter.value = 0
                        }
                        button("Increment (+)", backgroundColor = Color(0xFF22C55E.toInt())) {
                            counter.value++
                        }
                    }
                }
            }
        }
    }
}
```

---

## 🍳 Recipe 3: Task / Todo Manager with Dynamic List

A complete Task list allowing users to type tasks, add them to a list, and toggle completion:

```kotlin
// src/main.kt
import ui4.*

data class TodoItem(val id: Int, val title: String, val isCompleted: Boolean)

fun main() = app {
    val inputTask = mutableStateOf("")
    val todoList = mutableStateOf(
        listOf(
            TodoItem(1, "Build pure Kotlin DEX compiler", true),
            TodoItem(2, "Implement APK Signature Scheme v2", true),
            TodoItem(3, "Launch on physical mobile phone", true),
            TodoItem(4, "Write developer & user documentation", false)
        )
    )

    screen {
        box(padding = 20f) {
            column(gap = 16) {
                text("KUI Task Tracker", style = TextStyle.Headline)

                row(gap = 8) {
                    textField(
                        value = inputTask.value,
                        placeholder = "New task description...",
                        onValueChange = { inputTask.value = it }
                    )
                    button("Add") {
                        if (inputTask.value.isNotBlank()) {
                            val newItem = TodoItem(
                                id = (todoList.value.maxOfOrNull { it.id } ?: 0) + 1,
                                title = inputTask.value.trim(),
                                isCompleted = false
                            )
                            todoList.value = todoList.value + newItem
                            inputTask.value = ""
                        }
                    }
                }

                divider()

                column(gap = 8) {
                    for (item in todoList.value) {
                        row(gap = 12, alignment = Alignment.CenterVertically) {
                            val checkSymbol = if (item.isCompleted) "[X]" else "[ ]"
                            button(checkSymbol) {
                                todoList.value = todoList.value.map {
                                    if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                                }
                            }
                            text(
                                item.title,
                                style = if (item.isCompleted) {
                                    TextStyle.Body.copy(color = Color.Gray)
                                } else {
                                    TextStyle.Body
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## 🍳 Recipe 4: User Profile Card Dashboard

A modern dashboard card layout with metrics and avatars:

```kotlin
// src/main.kt
import ui4.*

fun main() = app {
    screen {
        center {
            box(
                width = 360f,
                padding = 24f,
                backgroundColor = Color.White
            ) {
                column(gap = 16) {
                    row(gap = 16, alignment = Alignment.CenterVertically) {
                        // Avatar placeholder
                        box(width = 64f, height = 64f, backgroundColor = Color(0xFF6366F1.toInt())) {
                            center {
                                text("B", style = TextStyle(fontSize = 28f, color = Color.White))
                            }
                        }
                        column(gap = 4) {
                            text("Babar", style = TextStyle.Title)
                            text("Lead Mobile Architect", style = TextStyle.Caption)
                        }
                    }

                    divider()

                    row(gap = 20) {
                        column(gap = 4) {
                            text("Projects", style = TextStyle.Caption)
                            text("12", style = TextStyle.Headline)
                        }
                        column(gap = 4) {
                            text("Build Speed", style = TextStyle.Caption)
                            text("0.4s", style = TextStyle.Headline)
                        }
                        column(gap = 4) {
                            text("KUI Status", style = TextStyle.Caption)
                            text("Live 🚀", style = TextStyle.Headline)
                        }
                    }

                    button(
                        text = "View Analytics",
                        backgroundColor = Color(0xFF4F46E5.toInt()),
                        textColor = Color.White
                    ) {
                        println("Opening analytics dashboard...")
                    }
                }
            }
        }
    }
}
```

---

## 📱 Running Any Example on Your Mobile Phone

1. Put the recipe code into `src/main.kt` of your project.
2. Connect your phone via USB cable and ensure USB debugging is enabled.
3. In your project directory, execute:
   ```powershell
   kui run
   ```
4. The KUI platform toolchain will compile, build Dalvik bytecode (`classes.dex`), package into an aligned APK, sign with v2 signature, install via ADB, and immediately display the screen on your phone!
