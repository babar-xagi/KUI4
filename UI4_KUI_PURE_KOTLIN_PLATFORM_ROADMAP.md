# 🟣 UI4 + KUI — Pure Kotlin Application Platform

## Complete Project Goal, Architecture, Toolchain, UI Vision, CLI UX, Directory Structure, Testing Strategy, and Tiny-Phase Roadmap

> **North star:** Build a complete Kotlin-first application platform where developers can create modern declarative UI apps with a Compose-like experience, but without Gradle, AGP, Jetpack Compose, Material3, AndroidX UI runtime, or external build orchestration. The developer should need only the official Kotlin compiler/JDK foundation plus UI4/KUI, whose own tooling is written in Kotlin.

---

# 1. 🌍 Vision

**UI4** is the UI/runtime framework.  
**KUI** is the developer toolchain and CLI.

```text
Developer writes Kotlin
        ↓
UI4 declarative UI
        ↓
KUI project/build system
        ↓
Kotlin compiler
        ↓
KUI-owned DEX/resources/APK/signing
        ↓
install + launch
        ↓
native Android application
```

Desired workflow:

```powershell
kui new hello
cd hello
kui run
```

Generated project:

```text
hello/
├── kui.toml
├── src/
│   └── main.kt
├── assets/
└── tests/
```

No normal UI4 project should require:

```text
gradlew
gradle/
settings.gradle.kts
build.gradle.kts
Android Gradle Plugin
AAPT2
D8
apksigner
zipalign
```

---

# 2. 🎯 Developer Experience

Target source:

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

The developer should think about:

```text
screen
layout
state
events
navigation
business logic
```

KUI should hide:

```text
manifest internals
DEX internals
APK ZIP structure
signing blocks
build graph
cache keys
device commands
compiler flags
```

---

# 3. 🧭 Naming

## UI framework
**UI4**

Owns:

```text
declarative API
UI tree
state
layout
rendering
text
input
gestures
animation
navigation
accessibility
themes
platform services
```

## Toolchain / CLI
**KUI**

```powershell
kui new hello
kui run
kui build
kui install
kui launch
kui test
kui doctor
kui clean
kui bench
kui profile
kui info
```

---

# 4. 🔒 Final Toolchain Policy

Allowed foundation:

```text
Kotlin language
official Kotlin compiler
JDK/JVM required to run the Kotlin compiler/tooling
Android OS / official platform APIs
UI4/KUI code written in Kotlin
```

Final normal build path should not require:

```text
Gradle
AGP
AAPT2
D8
R8
apksigner
zipalign
external Maven resolver
third-party UI runtime
Rust
Zig
C++
```

### Important engineering reality

Normal Kotlin/JVM compilation produces JVM class files, while Android executes DEX. Therefore KUI must eventually own either:

```text
Kotlin → JVM bytecode → KUI DEX compiler
```

or later:

```text
Kotlin frontend/IR → KUI DEX backend
```

The roadmap reaches this gradually instead of trying to build everything on day one.

---

# 5. 🧠 Principles

1. 🟣 Kotlin everywhere by default.
2. ✨ Simple outside, complex inside.
3. 🧰 One CLI.
4. 📦 One project model.
5. 🚫 No build-file ceremony.
6. 🎨 Modern minimal UI.
7. ♿ Accessibility is core architecture.
8. ⚡ Small runtime and fast startup.
9. 📊 Measure before optimizing.
10. 🧪 Every phase is testable.
11. 📱 Use Android platform services rather than rebuilding the OS.
12. 🔧 Own build/package logic when it materially improves DX.
13. 🧑‍🎓 Public APIs must remain beginner-readable.
14. 🔍 No hidden performance claims—benchmark them.
15. 🧱 Grow vertically from a working tiny system.

---

# 6. 🏗️ Architecture

```text
┌──────────────────────────────────────────┐
│             Developer App                │
│                 Kotlin                   │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│                   UI4                    │
│ API • State • Layout • Input • Nav      │
│ Accessibility • Theme • Services        │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│              UI4 Runtime                 │
│ UI Tree • Dirty Tracking • Render Tree  │
│ Text • Gestures • Animation             │
└───────────────────┬──────────────────────┘
                    ▼
┌──────────────────────────────────────────┐
│                   KUI                    │
│ Project • Build • Compiler • Cache      │
│ DEX • Resources • APK • Signing         │
│ Device • Test • Profile                 │
└───────────────────┬──────────────────────┘
                    ▼
               Android / ART
```

---

# 7. 📁 Monorepo Structure

```text
ui4-platform/
├── README.md
├── LICENSE
├── kui.toml
├── docs/
│   ├── vision.md
│   ├── architecture.md
│   ├── ui-api.md
│   ├── project-format.md
│   ├── compiler.md
│   ├── dex.md
│   ├── packaging.md
│   ├── accessibility.md
│   ├── performance.md
│   ├── testing.md
│   └── decisions/
├── platform/
│   ├── ui4/
│   │   ├── api/
│   │   ├── core/
│   │   ├── tree/
│   │   ├── state/
│   │   ├── layout/
│   │   ├── render/
│   │   ├── text/
│   │   ├── input/
│   │   ├── gesture/
│   │   ├── animation/
│   │   ├── navigation/
│   │   ├── accessibility/
│   │   ├── theme/
│   │   ├── resources/
│   │   └── platform/android/
│   └── kui/
│       ├── cli/
│       ├── project/
│       ├── config/
│       ├── build/
│       ├── cache/
│       ├── compiler/
│       ├── classfile/
│       ├── dex/
│       ├── axml/
│       ├── resources/
│       ├── apk/
│       ├── signing/
│       ├── device/
│       ├── testing/
│       └── profiling/
├── examples/
├── tests/
├── benchmarks/
└── scripts/
```

---

# 8. 📦 Generated Project

```powershell
kui new hello
```

creates:

```text
hello/
├── kui.toml
├── src/
│   └── main.kt
├── assets/
│   ├── images/
│   └── fonts/
├── tests/
│   └── AppTest.kt
└── README.md
```

Example `kui.toml`:

```toml
[project]
name = "hello"
version = "0.1.0"
application_id = "com.example.hello"

[android]
min_sdk = 24
target_sdk = 36

[ui]
theme = "system"
```

Example `src/main.kt`:

```kotlin
import ui4.*

fun main() = app {
    screen {
        center {
            text("Hello, UI4 👋")
        }
    }
}
```

---

# 9. 🧰 CLI UX

```text
kui new <name>     create project
kui doctor         verify environment
kui info           show project information
kui build          compile/package
kui run            build + install + launch
kui install        install current APK
kui launch         launch installed app
kui test           run tests
kui clean          remove generated build output
kui bench          benchmarks
kui profile        profiling report
kui ui-tree        print runtime UI tree later
```

`kui run` should eventually mean:

```text
load project
↓
compile Kotlin
↓
generate DEX
↓
compile/generate resources
↓
package APK
↓
sign
↓
install
↓
launch
```

---

# 10. 🎨 UI4 Design

UI4 should combine:

```text
Compose-like readability
React-like simplicity
native Android integration
smaller runtime
fewer concepts
less ceremony
predictable behavior
```

Example:

```kotlin
screen {
    column(
        gap = 16,
        padding = 24
    ) {
        text("Sign in", style = TextStyle.Headline)

        textField(
            state = email,
            placeholder = "Email"
        )

        button("Continue") {
            submit()
        }
    }
}
```

Initial component set:

```text
Screen
Box
Row
Column
Stack
Spacer
Text
Image
Icon
Button
IconButton
TextField
Checkbox
Switch
Card
Divider
Scroll
List
Dialog
Sheet
NavigationBar
```

Do not begin with 100 components.

---

# 11. 🎭 Style / Modifier Experiments

Option A:

```kotlin
box(
    modifier = Modifier
        .fillWidth()
        .padding(16)
        .background(Color.Black)
        .rounded(16)
)
```

Option B:

```kotlin
box(
    fillWidth = true,
    padding = 16,
    background = Color.Black,
    radius = 16
)
```

UI4 should prototype both before freezing the public API.

---

# 12. 🧠 State

Target:

```kotlin
val count = state(0)

text("Count: ${count.value}")

button("Increase") {
    count.value++
}
```

Internal model:

```text
state changes
↓
dependent nodes dirty
↓
smallest affected subtree updates
↓
layout only when needed
↓
render
```

---

# 13. 📐 Layout

First primitives:

```text
Box
Row
Column
Stack
Spacer
Scroll
```

Core concepts:

```text
constraints
measurement
size
position
alignment
spacing
padding
fill
weight
min/max
safe area
density
```

---

# 14. 🖌️ Rendering

### Stage 1
Use Android platform drawing APIs from Kotlin:

```text
Canvas
Paint
Path
Bitmap
RenderNode where useful
```

### Stage 2
Optimize invalidation, batching, caches, text, and images.

### Stage 3
Only build a lower-level GPU backend if profiling proves it is necessary.

This keeps UI4 Pure Kotlin while avoiding years of premature native-renderer work.

---

# 15. 🔤 Text / IME / Accessibility

Use platform text capabilities first.

UI4 owns abstractions for:

```text
font
size
weight
line height
wrapping
selection
cursor
RTL
Unicode
emoji
fallback
```

Accessibility:

```kotlin
Semantics(
    role = Role.Button,
    label = "Continue",
    enabled = true,
    clickable = true
)
```

TextField eventually handles:

```text
focus
cursor
selection
composition
IME actions
password mode
multiline
clipboard
keyboard show/hide
```

while using Android's actual IME and accessibility services.

---

# 16. 🌙 Modern Minimal Design System

Spacing:

```text
4 8 12 16 24 32 48 64
```

Radius:

```text
8 12 16 20 full
```

Typography:

```text
caption
body
bodyStrong
subtitle
title
headline
display
```

Theme modes:

```text
Light
Dark
System
HighContrast
ReducedMotion
```

Motion:

```text
fast
subtle
purposeful
interruptible
```

---

# 17. 🧪 Testing

Every phase must have one or more:

```text
unit test
integration test
golden/snapshot test
device test
benchmark
manual visual checklist
```

Definition:

```text
compile
+
correct behavior
+
reproducible verification
```

---

# 18. 📊 Metrics

Record continuously:

```text
APK size
installed size
startup
first frame
RAM
CPU
frame time
input latency
layout time
render time
cold build
warm build
incremental build
cache hit rate
```

Frame budgets:

```text
60 FPS  → 16.67 ms
120 FPS → 8.33 ms
```

These are targets, not guarantees.

---

# 19. 🛠️ Final Build Pipeline

```text
src/main.kt
↓
KUI project loader
↓
official Kotlin compiler
↓
JVM classfiles / compiler IR
↓
KUI DEX compiler
↓
classes.dex
↓
KUI resource/manifest compiler
↓
KUI APK writer
↓
KUI signer
↓
app.apk
↓
KUI device runner
↓
install + launch
```

Build tasks:

```text
compileKotlin
compileResources
dex
package
sign
install
launch
```

Each task stores:

```text
inputs
outputs
hash
cache key
dependencies
timing
```

Cache:

```text
.kui/
├── cache/
│   ├── kotlin/
│   ├── dex/
│   ├── resources/
│   └── apk/
├── build/
└── reports/
```

---

# 20. 🛤️ 180 Tiny Testable Phases

## Milestone A — KUI Foundation 🧱


### Phase 001 — Repository bootstrap
**Goal:** Create monorepo skeleton.

**Test / Done when:** Clean checkout verifies.


### Phase 002 — KUI version
**Goal:** Add version constant.

**Test / Done when:** `kui --version` works.


### Phase 003 — CLI entry
**Goal:** Minimal `kui`.

**Test / Done when:** Running without args shows help.


### Phase 004 — Command parser
**Goal:** Parse subcommands.

**Test / Done when:** Unknown command is friendly.


### Phase 005 — Help command
**Goal:** Structured help.

**Test / Done when:** Snapshot passes.


### Phase 006 — Project path
**Goal:** Detect project root.

**Test / Done when:** Known fixture found.


### Phase 007 — TOML reader v1
**Goal:** Read name/version.

**Test / Done when:** Fixture parses.


### Phase 008 — Config diagnostics
**Goal:** Friendly config errors.

**Test / Done when:** Bad field identified.


### Phase 009 — Info command
**Goal:** Print metadata.

**Test / Done when:** Output matches fixture.


### Phase 010 — Logging
**Goal:** debug/info/warn/error.

**Test / Done when:** Filtering works.


### Phase 011 — Timing
**Goal:** Task duration helper.

**Test / Done when:** Monotonic.


### Phase 012 — File hashing
**Goal:** Content hash.

**Test / Done when:** Stable result.


### Phase 013 — Directory hashing
**Goal:** Stable tree hash.

**Test / Done when:** Ordering independent.


### Phase 014 — Build dirs
**Goal:** Create `.kui/build`.

**Test / Done when:** Safe creation.


### Phase 015 — Cache dirs
**Goal:** Create `.kui/cache`.

**Test / Done when:** Deterministic path.


### Phase 016 — Task abstraction
**Goal:** Inputs/outputs task.

**Test / Done when:** Runs once.


### Phase 017 — Task dependencies
**Goal:** Dependency graph.

**Test / Done when:** Correct order.


### Phase 018 — Task caching
**Goal:** Skip unchanged.

**Test / Done when:** Second run cached.


### Phase 019 — Build report
**Goal:** Write report.

**Test / Done when:** Includes timings.


### Phase 020 — Foundation benchmark
**Goal:** Empty graph baseline.

**Test / Done when:** Stored report.


## Milestone B — Project Generator 📦


### Phase 021 — New command parser
**Goal:** Accept name.

**Test / Done when:** Invalid names rejected.


### Phase 022 — Create root
**Goal:** Create folder.

**Test / Done when:** Folder exists.


### Phase 023 — Generate kui.toml
**Goal:** Write metadata.

**Test / Done when:** Parses back.


### Phase 024 — Generate main.kt
**Goal:** Hello UI source.

**Test / Done when:** Template correct.


### Phase 025 — Assets folders
**Goal:** Create assets.

**Test / Done when:** Folders exist.


### Phase 026 — Tests folder
**Goal:** Create test skeleton.

**Test / Done when:** File exists.


### Phase 027 — README
**Goal:** Generate instructions.

**Test / Done when:** Snapshot.


### Phase 028 — Application ID
**Goal:** Default ID rule.

**Test / Done when:** Known cases pass.


### Phase 029 — Custom app ID
**Goal:** Override config.

**Test / Done when:** Stored correctly.


### Phase 030 — Version model
**Goal:** Name/version/code.

**Test / Done when:** Validation passes.


### Phase 031 — Android config
**Goal:** min/target SDK.

**Test / Done when:** Loads correctly.


### Phase 032 — Template version
**Goal:** Record generator version.

**Test / Done when:** Value present.


### Phase 033 — Minimal template
**Goal:** `--minimal`.

**Test / Done when:** Only essentials.


### Phase 034 — Generator integration
**Goal:** Generate temp project.

**Test / Done when:** All files verified.


### Phase 035 — UX review
**Goal:** Beginner reads project.

**Test / Done when:** Can explain each file.


## Milestone C — Kotlin Compiler Driver 🧠


### Phase 036 — Find compiler
**Goal:** Discover Kotlin compiler.

**Test / Done when:** Doctor reports path.


### Phase 037 — Compiler version
**Goal:** Read version.

**Test / Done when:** Validation works.


### Phase 038 — JDK check
**Goal:** Verify runtime.

**Test / Done when:** Doctor PASS.


### Phase 039 — Process wrapper
**Goal:** Run compiler.

**Test / Done when:** Exit code captured.


### Phase 040 — Compile one file
**Goal:** Compile tiny `.kt`.

**Test / Done when:** Class exists.


### Phase 041 — Diagnostics
**Goal:** Capture compiler errors.

**Test / Done when:** Clean output.


### Phase 042 — Source discovery
**Goal:** Find src/**/*.kt.

**Test / Done when:** Stable list.


### Phase 043 — Multiple files
**Goal:** Cross-file compile.

**Test / Done when:** Works.


### Phase 044 — Classpath model
**Goal:** Deterministic classpath.

**Test / Done when:** Snapshot.


### Phase 045 — UI4 API bootstrap
**Goal:** Compile API module.

**Test / Done when:** Import works.


### Phase 046 — Source hash
**Goal:** Hash sources.

**Test / Done when:** Unchanged identical.


### Phase 047 — Compiler cache key
**Goal:** Include config/version.

**Test / Done when:** Invalidates correctly.


### Phase 048 — Compile task
**Goal:** Build graph compile.

**Test / Done when:** Task PASS.


### Phase 049 — Build v0
**Goal:** `kui build` compile-only.

**Test / Done when:** PASS.


### Phase 050 — Clean compile
**Goal:** Remove/rebuild.

**Test / Done when:** Reproducible.


### Phase 051 — Compile timing
**Goal:** Record ms.

**Test / Done when:** Report includes timing.


### Phase 052 — Failure UX
**Goal:** File/line concise error.

**Test / Done when:** No noise.


### Phase 053 — Verbose mode
**Goal:** Show raw compiler args.

**Test / Done when:** Works.


### Phase 054 — Integration fixture
**Goal:** Compile sample app.

**Test / Done when:** PASS.


### Phase 055 — Compiler baseline
**Goal:** Benchmark Hello compile.

**Test / Done when:** Stored.


## Milestone D — UI4 Core + Layout 🎨


### Phase 056 — UI4 module
**Goal:** Bootstrap core.

**Test / Done when:** Tests run.


### Phase 057 — NodeId
**Goal:** Typed IDs.

**Test / Done when:** Tests.


### Phase 058 — Geometry
**Goal:** Point/Size/Rect/Insets.

**Test / Done when:** Math tests.


### Phase 059 — Color
**Goal:** RGBA/hex.

**Test / Done when:** Conversions.


### Phase 060 — UiNode
**Goal:** Minimal node.

**Test / Done when:** Creation.


### Phase 061 — Children
**Goal:** Tree links.

**Test / Done when:** Invariants.


### Phase 062 — Root
**Goal:** Single root.

**Test / Done when:** Enforced.


### Phase 063 — Traversal
**Goal:** DFS.

**Test / Done when:** Order test.


### Phase 064 — Dirty flag
**Goal:** Mark dirty.

**Test / Done when:** Test.


### Phase 065 — Debug tree
**Goal:** Printable tree.

**Test / Done when:** Snapshot.


### Phase 066 — Constraints
**Goal:** Min/max.

**Test / Done when:** Clamp.


### Phase 067 — Measure
**Goal:** Desired size protocol.

**Test / Done when:** Fixture.


### Phase 068 — Position
**Goal:** Final rect.

**Test / Done when:** Placement.


### Phase 069 — Box
**Goal:** First primitive.

**Test / Done when:** Layout test.


### Phase 070 — Padding
**Goal:** Insets.

**Test / Done when:** Geometry.


### Phase 071 — Alignment
**Goal:** start/center/end.

**Test / Done when:** Tests.


### Phase 072 — Column
**Goal:** Vertical layout.

**Test / Done when:** Correct placement.


### Phase 073 — Row
**Goal:** Horizontal layout.

**Test / Done when:** Correct placement.


### Phase 074 — Spacing
**Goal:** Gap.

**Test / Done when:** Exact.


### Phase 075 — Stack
**Goal:** Overlay.

**Test / Done when:** Z order.


### Phase 076 — Fill width
**Goal:** Fill x.

**Test / Done when:** Constraint.


### Phase 077 — Fill height
**Goal:** Fill y.

**Test / Done when:** Constraint.


### Phase 078 — Weight
**Goal:** Free-space split.

**Test / Done when:** Known cases.


### Phase 079 — Layout stress
**Goal:** 1k nodes.

**Test / Done when:** Timing.


### Phase 080 — DSL builder
**Goal:** Build tree from DSL.

**Test / Done when:** Hello tree.


### Phase 081 — screen {}
**Goal:** Root DSL.

**Test / Done when:** Works.


### Phase 082 — column {}
**Goal:** Column DSL.

**Test / Done when:** Works.


### Phase 083 — row {}
**Goal:** Row DSL.

**Test / Done when:** Works.


### Phase 084 — box {}
**Goal:** Box DSL.

**Test / Done when:** Works.


### Phase 085 — text()
**Goal:** Text node API.

**Test / Done when:** Tree contains node.


### Phase 086 — Modifier prototype
**Goal:** Modifier chain.

**Test / Done when:** Order test.


### Phase 087 — Padding modifier
**Goal:** `.padding(16)`.

**Test / Done when:** Layout test.


### Phase 088 — Background modifier
**Goal:** `.background`.

**Test / Done when:** Metadata.


### Phase 089 — Size modifiers
**Goal:** width/height/fill.

**Test / Done when:** Tests.


### Phase 090 — DSL usability
**Goal:** Hello source review.

**Test / Done when:** Beginner-readable.


## Milestone E — Android Render + State + Input ✋


### Phase 091 — Android host
**Goal:** Minimal Activity/View host.

**Test / Done when:** Launches.


### Phase 092 — Canvas surface
**Goal:** Receive Canvas.

**Test / Done when:** Draw callback.


### Phase 093 — Rectangle render
**Goal:** Draw box.

**Test / Done when:** Screenshot.


### Phase 094 — Text render
**Goal:** Platform text.

**Test / Done when:** Visible Hello.


### Phase 095 — Render tree
**Goal:** Tree → ops.

**Test / Done when:** Snapshot.


### Phase 096 — Invalidation
**Goal:** Draw only dirty.

**Test / Done when:** Idle no redraw.


### Phase 097 — Clip
**Goal:** Clip children.

**Test / Done when:** Golden.


### Phase 098 — Rounded rect
**Goal:** Radius.

**Test / Done when:** Golden.


### Phase 099 — Opacity
**Goal:** Alpha.

**Test / Done when:** Golden.


### Phase 100 — Render timing
**Goal:** Measure draw.

**Test / Done when:** Report.


### Phase 101 — State<T>
**Goal:** Generic state.

**Test / Done when:** Initial value.


### Phase 102 — State get/set
**Goal:** Mutation.

**Test / Done when:** Pass.


### Phase 103 — Change detection
**Goal:** Equal skip.

**Test / Done when:** No dirty.


### Phase 104 — Subscriptions
**Goal:** State dependencies.

**Test / Done when:** Fixture.


### Phase 105 — Partial update
**Goal:** Affected subtree only.

**Test / Done when:** Verified.


### Phase 106 — Counter demo
**Goal:** Button increments.

**Test / Done when:** Device PASS.


### Phase 107 — Hit testing
**Goal:** Pointer → node.

**Test / Done when:** Overlap test.


### Phase 108 — Press
**Goal:** Down/up state.

**Test / Done when:** Pass.


### Phase 109 — Click
**Goal:** Callback once.

**Test / Done when:** Pass.


### Phase 110 — Button
**Goal:** Component.

**Test / Done when:** Device PASS.


### Phase 111 — Disabled
**Goal:** Ignore click.

**Test / Done when:** Pass.


### Phase 112 — Focus
**Goal:** Focusable nodes.

**Test / Done when:** Pass.


### Phase 113 — Key model
**Goal:** Keyboard abstraction.

**Test / Done when:** Mock.


### Phase 114 — Keyboard activation
**Goal:** Enter/Space button.

**Test / Done when:** Pass.


### Phase 115 — Gesture interface
**Goal:** Recognizer contract.

**Test / Done when:** Mock.


### Phase 116 — Tap
**Goal:** Tap recognizer.

**Test / Done when:** Threshold.


### Phase 117 — Long press
**Goal:** Timed.

**Test / Done when:** Pass.


### Phase 118 — Drag
**Goal:** Continuous delta.

**Test / Done when:** Pass.


### Phase 119 — Swipe
**Goal:** Velocity/direction.

**Test / Done when:** Pass.


### Phase 120 — Scroll
**Goal:** Vertical container.

**Test / Done when:** Device.


### Phase 121 — Scroll bounds
**Goal:** Clamp.

**Test / Done when:** Unit.


### Phase 122 — Fling
**Goal:** Physics.

**Test / Done when:** Deterministic.


### Phase 123 — Frame clock
**Goal:** Animation clock.

**Test / Done when:** Monotonic.


### Phase 124 — Tween
**Goal:** Interpolation.

**Test / Done when:** Reference.


### Phase 125 — Frame benchmark
**Goal:** 60/120 report.

**Test / Done when:** Stored.


## Milestone F — Text + Accessibility + Navigation ♿


### Phase 126 — Text styles
**Goal:** Size/weight/color.

**Test / Done when:** Tests.


### Phase 127 — Line wrap
**Goal:** Wrap text.

**Test / Done when:** Screenshot.


### Phase 128 — Alignment text
**Goal:** start/center/end.

**Test / Done when:** Visual.


### Phase 129 — Font selection
**Goal:** System font.

**Test / Done when:** Applied.


### Phase 130 — Layout direction
**Goal:** LTR/RTL.

**Test / Done when:** Fixture.


### Phase 131 — Urdu/Arabic proof
**Goal:** Connected script.

**Test / Done when:** Screenshot.


### Phase 132 — Emoji proof
**Goal:** Emoji render.

**Test / Done when:** Screenshot.


### Phase 133 — Selection model
**Goal:** Text range.

**Test / Done when:** Unit.


### Phase 134 — Focus traversal
**Goal:** Next/previous.

**Test / Done when:** Deterministic.


### Phase 135 — Semantic node
**Goal:** Accessibility metadata.

**Test / Done when:** Tree.


### Phase 136 — Semantic roles
**Goal:** Button/text/image.

**Test / Done when:** Tests.


### Phase 137 — Labels/actions
**Goal:** Semantic actions.

**Test / Done when:** Snapshot.


### Phase 138 — Android accessibility adapter
**Goal:** Platform mapping.

**Test / Done when:** TalkBack sees nodes.


### Phase 139 — Large text
**Goal:** 200% font scale.

**Test / Done when:** Layout survives.


### Phase 140 — High contrast
**Goal:** Theme support.

**Test / Done when:** Visual.


### Phase 141 — Reduced motion
**Goal:** Policy.

**Test / Done when:** Animation changes.


### Phase 142 — TextField shell
**Goal:** Editable field.

**Test / Done when:** Focus.


### Phase 143 — IME show/hide
**Goal:** Platform keyboard.

**Test / Done when:** Visible.


### Phase 144 — Text input
**Goal:** Committed text.

**Test / Done when:** Typing.


### Phase 145 — Cursor
**Goal:** Caret.

**Test / Done when:** Correct.


### Phase 146 — Selection
**Goal:** Select range.

**Test / Done when:** Pass.


### Phase 147 — Navigation stack
**Goal:** Push/pop.

**Test / Done when:** Unit.


### Phase 148 — Back handling
**Goal:** Android back.

**Test / Done when:** Device.


### Phase 149 — Saved state
**Goal:** Basic restore.

**Test / Done when:** Pass.


### Phase 150 — Mini UI milestone
**Goal:** Hello/counter/nav/input.

**Test / Done when:** Real demo PASS.


## Milestone G — Pure Kotlin Android Toolchain 🛠️


### Phase 151 — Classfile header
**Goal:** Parse JVM class file.

**Test / Done when:** Fixture.


### Phase 152 — Constant pool
**Goal:** Parse constants.

**Test / Done when:** Correct.


### Phase 153 — Methods/code
**Goal:** Read bytecode.

**Test / Done when:** Exposed.


### Phase 154 — DEX model
**Goal:** Header/tables/items.

**Test / Done when:** Internal validate.


### Phase 155 — DEX strings
**Goal:** Encode strings.

**Test / Done when:** Reference.


### Phase 156 — DEX type/proto
**Goal:** Encode signatures.

**Test / Done when:** Reference.


### Phase 157 — DEX fields/methods
**Goal:** Encode IDs.

**Test / Done when:** Reference.


### Phase 158 — DEX code item
**Goal:** Emit tiny method.

**Test / Done when:** Decoder verifies.


### Phase 159 — Class → DEX v1
**Goal:** Translate trivial class.

**Test / Done when:** Verifier fixture.


### Phase 160 — Control flow
**Goal:** Branches/labels.

**Test / Done when:** Works.


### Phase 161 — Registers
**Goal:** Simple allocator.

**Test / Done when:** Known case.


### Phase 162 — Invokes
**Goal:** Method calls.

**Test / Done when:** Works.


### Phase 163 — Objects/fields
**Goal:** new/get/set.

**Test / Done when:** Works.


### Phase 164 — Exceptions
**Goal:** try/catch metadata.

**Test / Done when:** Works.


### Phase 165 — Kotlin metadata tolerance
**Goal:** Preserve/ignore safely.

**Test / Done when:** Simple Kotlin converts.


### Phase 166 — Hello → DEX
**Goal:** Compile Kotlin Hello.

**Test / Done when:** DEX produced.


### Phase 167 — AXML writer
**Goal:** Binary XML chunks.

**Test / Done when:** Parser validates.


### Phase 168 — Manifest generator
**Goal:** kui.toml → manifest.

**Test / Done when:** Fields correct.


### Phase 169 — APK ZIP writer
**Goal:** Write archive.

**Test / Done when:** Readable.


### Phase 170 — DEX injection
**Goal:** Add classes.dex.

**Test / Done when:** Present.


### Phase 171 — Assets entries
**Goal:** Package assets.

**Test / Done when:** Present.


### Phase 172 — Alignment
**Goal:** ZIP alignment.

**Test / Done when:** Test.


### Phase 173 — Signing model
**Goal:** APK signing block.

**Test / Done when:** Parser validates.


### Phase 174 — APK v2 signer
**Goal:** JDK crypto signing.

**Test / Done when:** Android verifies.


### Phase 175 — Signature verifier
**Goal:** Verify/tamper.

**Test / Done when:** Tamper fails.


### Phase 176 — Package task
**Goal:** Create APK without external packager.

**Test / Done when:** APK created.


### Phase 177 — Device discovery
**Goal:** List Android devices.

**Test / Done when:** Serial shown.


### Phase 178 — Install
**Goal:** Install APK.

**Test / Done when:** Success.


### Phase 179 — Launch
**Goal:** Start app.

**Test / Done when:** Opens.


### Phase 180 — Final kui run
**Goal:** Compile→DEX→package→sign→install→launch.

**Test / Done when:** Fresh project runs with no Gradle/AGP/AAPT2/D8/apksigner.

---

# 21. 🔭 Post-180 Roadmap

After the first self-owned Android path works, continue with the same tiny-phase method.

## H — Resource Compiler 📚
Build Kotlin implementations for:

```text
strings
colors
dimensions
images
localization
density variants
resource IDs
binary resource table
theme resources
```

## I — Better DEX Backend 🧠

```text
desugaring
lambdas
coroutines validation
multidex
debug info
dead-code elimination
inlining
shrinking
register optimization
```

## J — Incremental Compilation ⚡

```text
source graph
ABI hashes
class cache
DEX cache
resource cache
partial package rebuild
```

## K — Dependency System 📦

```toml
[dependencies]
my-lib = "1.2.0"
```

Long-term:

```text
lockfile
version resolution
checksums
offline mode
local packages
workspaces
publishing
```

## L — Advanced UI4 Components ✨

```text
VirtualList
Grid
Tabs
Dialog
Sheet
Menu
Tooltip
Snackbar
Slider
Progress
Date/Time input
```

## M — Advanced Rendering 🎨

```text
shadows
gradients
paths
transforms
layers
RenderNode optimization
image caching
damage tracking
partial redraw
```

## N — Desktop 🖥️

Use same UI4 API with platform backends:

```text
Windows
Linux
macOS
```

One at a time.

## O — Developer Tools 🛠️

```text
UI inspector
layout overlay
state inspector
event inspector
accessibility inspector
frame profiler
memory profiler
build profiler
snapshot testing
```

## P — Fast Development Loop 🚀

```text
kui dev
file watcher
fast restart
hot reload research
template system
migration system
diagnostic codes
IDE protocol
```

---

# 22. 🧪 Mandatory Android Test Matrix

```text
small phone
large phone
tablet
portrait
landscape
60 Hz
120 Hz where available
light mode
dark mode
high contrast
font scale 100%
font scale 200%
LTR
RTL
English
Urdu
Arabic
emoji
keyboard
touch
background/restore
process recreation
```

---

# 23. ✅ Component Acceptance Checklist

```text
[ ] clean hierarchy
[ ] consistent spacing
[ ] readable typography
[ ] correct touch target
[ ] light mode
[ ] dark mode
[ ] keyboard focus
[ ] accessibility semantics
[ ] disabled state
[ ] pressed state
[ ] loading state where relevant
[ ] reduced motion
[ ] high contrast
[ ] RTL
[ ] large text
[ ] localization-safe layout
[ ] unit test
[ ] device test
```

---

# 24. 📊 Benchmark Report

Every milestone should record:

```text
Kotlin compile ms
DEX ms
resource ms
APK package ms
signing ms
install ms
launch ms
end-to-end ms
APK bytes
installed bytes
idle RAM
first-frame ms
frame P50/P95/P99
```

Reports:

```text
.kui/reports/
```

---

# 25. 📝 Phase Report Template

```markdown
# Phase 081 — screen DSL

## Goal
Add `screen {}` to UI4.

## Learned
- Kotlin DSL receivers
- UI root
- tree creation

## Files changed
...

## Test
kui test

## Result
PASS

## Performance
...

## Problems
...

## Next
Phase 082
```

---

# 26. 🗂️ Git Strategy

```text
main
dev
phase/001-bootstrap
phase/002-version
...
```

After every successful phase:

```powershell
git add .
git commit -m "phase 081: add screen DSL"
git push
```

---

# 27. 🏁 Suggested Versions

```text
0.1 KUI CLI + project generator
0.2 Kotlin compiler pipeline
0.3 UI tree + layout
0.4 Android renderer
0.5 state + input
0.6 scroll + animation
0.7 text + accessibility
0.8 navigation + TextField
0.9 custom DEX/APK/signing
1.0 first complete one-command Android platform
```

Do not rush `1.0`.

---

# 28. ⭐ First Practical Product

## UI4 Mini v0.1

Support:

```text
Screen
Box
Row
Column
Text
Button
padding
spacing
alignment
background
state
tap
scroll
basic animation
navigation
```

Demo:

```kotlin
import ui4.*

val count = state(0)

fun main() = app {
    screen {
        column(
            gap = 16,
            alignment = Alignment.Center
        ) {
            text("UI4 🟣", style = TextStyle.Headline)
            text("Count: ${count.value}")

            button("Increase") {
                count.value++
            }
        }
    }
}
```

---

# 29. 🚀 Final North Star

```powershell
kui new hello
cd hello
kui run
```

Project:

```text
hello/
├── kui.toml
├── src/main.kt
├── assets/
└── tests/
```

`main.kt`:

```kotlin
import ui4.*

fun main() = app {
    screen {
        center {
            text("Hello, World! 👋")
        }
    }
}
```

KUI internally owns:

```text
project loading
Kotlin compilation
UI4 linking
DEX generation
manifest/resources
APK packaging
alignment
signing
device install
launch
testing
profiling
```

The normal UI4 developer should never need:

```text
Gradle
AGP
AAPT2
D8
apksigner
build.gradle.kts
settings.gradle.kts
```

---

# 30. 🧠 Core Rule

> **UI4/KUI must grow from a tiny working Kotlin platform into a large working Kotlin platform—not from a giant design into an unfinished system.**

Every phase:

```text
learn
↓
implement
↓
test
↓
measure
↓
commit
↓
next
```
