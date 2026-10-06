# 👋 Greeting Example

This project contains two literal greeting strings and a UI4 column. The JVM DSL builds a layout tree; the current Android backend presents extracted strings together in one native TextView.

From this directory:

```powershell
..\..\kui.bat build
..\..\kui.bat run
```

Output: `build/outputs/apk/debug/app-debug.apk`. Kotlin and JDK 21+ are required for building; ADB and an authorized device are required for deployment.

[Project guide](../../docs/user/project-guide.md) · [Example cookbook](../../docs/user/cookbook-and-examples.md)