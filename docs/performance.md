# ⚡ Performance and Benchmarks

KUI provides JVM benchmarks:

```powershell
kui bench
kui bench compiler
```

Reports are written under `.kui/build/reports/`. The foundation benchmark measures toolchain operations; the compiler benchmark measures cold and warm compilation.

UI4 tests include layout and recorded-render measurements. These are headless JVM measurements, not Android GPU frame-time results.

Cached builds can skip application compilation. Timing depends on hardware, JVM startup, compiler discovery, source size, and cache state. The project does not guarantee sub-second builds, zero-allocation layout, or a particular Android startup time.

Report the command, environment, workload, and cold/warm conditions when discussing performance.

[Testing](testing.md) · [Contributor guide](developer/contributing.md)