# 💡 Making KUI Easier to Learn

**Status: design proposal.** This document recommends changes; it does not introduce new commands or APIs.

KUI already has a declarative DSL. The useful next step is to make its behavior predictable, its learning path short, and its build workflow easy to trust.

## Principles

1. Teach a small set of concepts before exposing advanced internals.
2. Make the same example behave consistently in tests and on a device.
3. Prefer one documented spelling for each common operation.
4. Choose useful defaults while allowing explicit configuration.
5. Keep diagnostics focused on the developer's next action.

## 🧭 Recommended order

| Priority | Change | Benefit | Completion evidence |
| --- | --- | --- | --- |
| 1 | Complete Android hosting and the required DEX/runtime behavior | Learned UI and state behavior works on a phone | A counter renders, receives taps, updates text, and survives lifecycle transitions |
| 2 | Stabilize a small public DSL | Beginners learn fewer overloads and imports | Consistent component parameters, a documented numeric convention, and examples using one public import |
| 3 | Add readable derived-state bindings and define recomposition/lifecycle semantics | Formatted text and dynamic UI update predictably | A formatted counter and conditional/list examples update correctly, with subscriptions disposed safely |
| 4 | Generate useful starter tests and teach errors with fixes | New projects provide evidence rather than placeholder success | Templates contain real checks; missing tools and unsupported features report a precise next step |
| 5 | Add a preview workflow | Developers get feedback before device installation | A real preview renders the same supported components as Android and reports unsupported ones |
| 6 | Improve installation and distributable runtime options | Fewer manual setup steps | A fresh Windows user can create and build a greeting through a documented setup flow |

The learning guides, accurate examples, and support boundaries can be improved immediately. Runtime and API features require separate implementation work.

## 🎨 A small surface to teach

Begin with:

```text
app → screen → row / column / box → text / button → state
```

The current smallest supported UI4 source is already compact:

```kotlin
import ui4.*

fun main() = app {
    screen {
        text("Hello, KUI!")
    }
}
```

Teach `text(state)` as the current binding mechanism. Longer-term, provide an easy way to derive formatted state and document which UI changes rebuild nodes. Do not promise that `text("Count: ${count.value}")` becomes reactive until that behavior is implemented and tested.

A possible future spelling is a computed text lambda:

```kotlin
// Proposed API; not implemented.
text { "Count: ${count.value}" }
```

It would need tracked state reads, reevaluation on change, and subscription cleanup. This is a design direction, not code users can run today.

## 🔧 API consistency

The current API has several color aliases, alignment forms, and numeric overloads. Modifiers also need separate extension imports.

Before adding widgets, choose a canonical public style for color, spacing, alignment, text-field callbacks, and component parameters. Preserve compatibility deliberately rather than accumulating aliases indefinitely.

A reusable component should remain an ordinary Kotlin function. Additional card, divider, image, and spacing components should have clear layout and lifecycle contracts.

## 🚀 Workflow improvements

Keep the familiar goal:

```powershell
kui new hello
cd hello
kui run
```

Improve this workflow by validating the environment early, offering starter templates, reporting the built APK path, and giving honest device results. A run that only built an APK should be visibly distinguishable from a confirmed install and launch.

A bundled JVM runtime could remove the user's separate Java installation step, although the underlying compiler would still need a JVM. Bundling, discovery, packaging size, updates, and supported operating systems would need a deliberate design.

A graphical project creator or installer could help developers who prefer not to use a terminal. An end user receiving a finished APK should never need the development toolchain.

## ✅ What this documentation update delivers

- A beginner path through five concepts.
- A minimal greeting without unnecessary nesting.
- Examples using implemented APIs and explicit state behavior.
- Separate guidance for application developers, platform contributors, and APK users.
- Current capability descriptions alongside future proposals.

The best measure of simplicity is whether a new developer can predict what their code does. Completing the runtime and keeping the API consistent will help more than adding new syntax alone.
