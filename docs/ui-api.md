# 🎨 UI4 API Overview

UI4 constructs a UI tree using Kotlin receiver lambdas. Begin with the [learning path](user/learning-path.md) and use the [component guide](user/ui-components-and-styling.md) for examples.

## Implemented building blocks

| Category | API |
| --- | --- |
| Entry | `app { screen { ... } }` |
| Layout | `column`, `row`, `box`, `center`, `stack`, `scroll` |
| Content | `text`, `button`, `textField` |
| State | `state`, `mutableStateOf`, `text(state)`, explicit node bindings |
| Styling | `TextStyle`, `Color`, `Modifier` extensions |

Import public DSL types with `ui4.*`. Modifier extensions currently require their `ui4.core` imports; explicit state binding extensions live in `ui4.state`.

## State contract

`text(state)` subscribes a node to state. Arbitrary string interpolation or state reads inside a builder do not automatically establish subscriptions or recompose the tree.

A mutable state value can be changed by a button callback, then observed through explicit bindings. Larger dynamic tree updates need explicit tree/state coordination.

## Platform boundary

These APIs are implemented in the JVM framework. The current Android packager generates a text Activity from literal source content rather than executing the entire UI4 application tree.

`card`, `divider`, `spacer`, and `image` are not current DSL functions. Proposed API additions are listed separately in the [developer experience proposal](developer/developer-experience.md).