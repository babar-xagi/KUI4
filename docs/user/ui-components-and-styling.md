# UI4 Components, Layouts & Styling Guide

UI4 is the declarative user interface toolkit included with KUI. This guide provides code examples and API references for every layout, widget, styling attribute, reactive state pattern, and animation tool available in UI4.

---

## 🧱 Layout Containers

### 1. `column` (Vertical Layout)
Stacks children vertically.
```kotlin
column(gap = 16, alignment = Alignment.CenterHorizontally) {
    text("Item 1")
    text("Item 2")
    text("Item 3")
}
```
- `gap`: Space in dp between consecutive children.
- `alignment`: Cross-axis alignment (`Start`, `CenterHorizontally`, `End`).

---

### 2. `row` (Horizontal Layout)
Arranges children side-by-side horizontally.
```kotlin
row(gap = 12, alignment = Alignment.CenterVertically) {
    button("Cancel") { /* on click */ }
    button("Submit") { /* on click */ }
}
```
- `gap`: Space in dp between consecutive children.
- `alignment`: Cross-axis alignment (`Top`, `CenterVertically`, `Bottom`).

---

### 3. `box` (Decorated Container)
A container offering background color, borders, padding, and size constraints.
```kotlin
box(
    width = 300f,
    height = 200f,
    padding = 16f,
    backgroundColor = Color(0xFFF1F5F9.toInt())
) {
    text("Card Content Inside Box")
}
```

---

### 4. `center` (Centering Layout)
Centers a child both horizontally and vertically within available parent space.
```kotlin
center {
    text("Dead Center", style = TextStyle.Headline)
}
```

---

### 5. `stack` (Z-Index Overlays)
Overlays children on top of each other. The first child is at the bottom, and the last child is on top.
```kotlin
stack {
    box(width = 200f, height = 200f, backgroundColor = Color.Blue)
    center {
        text("Overlaid Text", style = TextStyle(color = Color.White))
    }
}
```

---

### 6. `spacer` (Flexible Spacing)
Inserts flexible or fixed spacing along a layout axis.
```kotlin
row {
    text("Left Aligned")
    spacer() // Pushes the next element to the far right
    text("Right Aligned")
}
```

---

## 🎛️ Interactive Widgets

### 1. `text` (Typography)
Renders styled text.
```kotlin
text("Welcome to KUI", style = TextStyle.Headline)
text("Secondary subtitle", style = TextStyle.Title)
text("Standard body text", style = TextStyle.Body)
text("Tiny caption", style = TextStyle.Caption)
```

Custom `TextStyle`:
```kotlin
text(
    "Custom Styled Text",
    style = TextStyle(
        fontSize = 22f,
        color = Color(0xFF2563EB.toInt()), // Royal Blue
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5f
    )
)
```

---

### 2. `button` (Interactive Buttons)
Interactive button responding to user clicks/taps.
```kotlin
button(
    text = "Click Me",
    backgroundColor = Color(0xFF3B82F6.toInt()),
    textColor = Color.White
) {
    println("Button was tapped!")
}
```

---

### 3. `textField` (Text Input)
Accepts user text input with cursor positioning and keyboard events.
```kotlin
val username = mutableStateOf("")

textField(
    value = username.value,
    placeholder = "Enter your username...",
    onValueChange = { newValue ->
        username.value = newValue
    }
)
```

---

### 4. `card` & `divider`
```kotlin
card(elevation = 4f, padding = 16f) {
    column(gap = 8) {
        text("Profile", style = TextStyle.Title)
        divider(thickness = 1f, color = Color.Gray)
        text("Babar - Mobile Engineer", style = TextStyle.Body)
    }
}
```

---

## ⚡ Reactive State Management (`mutableStateOf`)

UI4 uses fine-grained reactivity. UI elements automatically subscribe to any state read during their composition. When the state changes, only the affected subtrees re-render!

### Counter Example:
```kotlin
fun counterApp() = app {
    val count = mutableStateOf(0)

    screen {
        center {
            column(gap = 20, alignment = Alignment.CenterHorizontally) {
                text("Current Count: ${count.value}", style = TextStyle.Headline)
                
                row(gap = 12) {
                    button("Decrement (-)") {
                        count.value--
                    }
                    button("Increment (+)") {
                        count.value++
                    }
                }
            }
        }
    }
}
```

---

## 👆 Gestures & Event Handling

Attach gesture modifiers directly to any UI node:

```kotlin
box(
    padding = 24f,
    backgroundColor = Color.Yellow
) {
    text("Tap, Double Tap, or Long Press Me!")
}.onClick {
    println("Single tap detected!")
}.onDoubleTap {
    println("Double tap detected!")
}.onLongPress {
    println("Long press detected!")
}
```

---

## 🎨 Colors & Theming

UI4 provides standard colors and 32-bit ARGB color construction:
```kotlin
Color.Red
Color.Green
Color.Blue
Color.White
Color.Black
Color.Transparent

// Custom Hex ARGB:
val brandPurple = Color(0xFF7C3AED.toInt())
val slate = Color.fromRgb(30, 41, 59)
val translucent = Color.fromArgb(alpha = 128, red = 0, green = 0, blue = 0)
```

---

## 🎬 Animations & Interpolation

Animate numeric values fluidly over time:
```kotlin
val opacity = mutableStateOf(0.0f)

// Animate from 0.0 to 1.0 over 500ms using EaseInOut
animateFloat(
    from = 0.0f,
    to = 1.0f,
    durationMs = 500,
    easing = Easing.EaseInOut
) { animatedValue ->
    opacity.value = animatedValue
}
```
Supported easings: `Linear`, `EaseIn`, `EaseOut`, `EaseInOut`, `Cubic`.
