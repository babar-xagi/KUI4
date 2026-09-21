# ⚡ Performance & Benchmarking

## Architectural Guarantees
1. **Zero-Allocation Layout Passes**: Measurement and placement use reusable vectors and inline value classes to prevent GC churn during animation frames.
2. **Flat UI Trees**: Containers measure children in a single linear pass, eliminating multi-measure exponential explosion.
3. **Sub-second Build Iteration**: KUI uses content-addressable hashing to skip compilation of unchanged modules.
4. **App Cold Startup**: Avoids heavyweight reflection or multi-step content provider initialization, launching directly into the first UI4 frame.
