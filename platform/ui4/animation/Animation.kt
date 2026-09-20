package ui4.animation

import kotlin.math.exp

/**
 * Inertial scroll fling physics simulation with exponential friction deceleration (Phase 122).
 */
class FlingPhysics(
    val friction: Float = 4.2f
) {
    /**
     * Total displacement produced by an initial velocity until rest.
     */
    fun totalDistance(initialVelocity: Float): Float =
        if (friction <= 0f) 0f else initialVelocity / friction

    /**
     * Calculates velocity at time t seconds.
     */
    fun velocityAt(initialVelocity: Float, timeSeconds: Float): Float =
        initialVelocity * exp(-friction * timeSeconds)

    /**
     * Calculates accumulated distance at time t seconds.
     */
    fun distanceAt(initialVelocity: Float, timeSeconds: Float): Float =
        if (friction <= 0f) 0f else (initialVelocity / friction) * (1f - exp(-friction * timeSeconds))
}

/**
 * Monotonic frame clock for animation and physics ticking (Phase 123).
 */
object FrameClock {

    fun nowNanos(): Long = System.nanoTime()
    fun nowMillis(): Long = System.nanoTime() / 1_000_000L

    /**
     * Executes an animation frame callback passing current frame time in nanoseconds.
     */
    fun withFrameNanos(block: (Long) -> Unit) {
        block(nowNanos())
    }
}

/**
 * Animation easing curve transformation contract (Phase 124).
 */
fun interface Easing {
    fun transform(fraction: Float): Float

    companion object {
        val Linear: Easing = Easing { it }
        val EaseIn: Easing = Easing { it * it }
        val EaseOut: Easing = Easing { it * (2f - it) }
        val EaseInOut: Easing = Easing {
            if (it < 0.5f) 2f * it * it else -1f + (4f - 2f * it) * it
        }

        fun cubicBezier(p1: Float, p2: Float): Easing = Easing { t ->
            val u = 1f - t
            // Simplified 1D cubic easing approximation: 3*u^2*t*p1 + 3*u*t^2*p2 + t^3
            (3f * u * u * t * p1) + (3f * u * t * t * p2) + (t * t * t)
        }
    }
}

/**
 * Deterministic value interpolation animation (Phase 124).
 */
class TweenAnimation(
    val from: Float,
    val to: Float,
    val durationMs: Long,
    val easing: Easing = Easing.Linear
) {
    init {
        require(durationMs >= 0L) { "durationMs must be non-negative: $durationMs" }
    }

    /**
     * Computes the animated value at a given elapsed time.
     */
    fun valueAt(elapsedMs: Long): Float {
        if (durationMs == 0L || elapsedMs >= durationMs) return to
        if (elapsedMs <= 0L) return from

        val progress = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val eased = easing.transform(progress)
        return from + (to - from) * eased
    }

    fun isFinished(elapsedMs: Long): Boolean =
        elapsedMs >= durationMs
}

/**
 * Frame timing and FPS benchmark report generator (Phase 125).
 */
object FrameBenchmark {

    data class Report(
        val frames: Int,
        val avgFrameMs: Double,
        val p95FrameMs: Double,
        val maxFrameMs: Double,
        val meets60Fps: Boolean,
        val meets120Fps: Boolean
    )

    /**
     * Measures frame clock tick and animation stepping latency across a specified number of frames.
     */
    fun run(frameCount: Int = 120): Report {
        val tween = TweenAnimation(0f, 1000f, 1000L, Easing.EaseInOut)
        val latenciesNanos = LongArray(frameCount)

        for (i in 0 until frameCount) {
            val start = System.nanoTime()
            val simulatedElapsed = (i * 16L) // 60 FPS tick step
            val v = tween.valueAt(simulatedElapsed)
            val end = System.nanoTime()
            latenciesNanos[i] = maxOf(1L, end - start)
        }

        latenciesNanos.sort()
        val avgMs = (latenciesNanos.average()) / 1_000_000.0
        val p95Index = ((frameCount * 0.95).toInt()).coerceIn(0, frameCount - 1)
        val p95Ms = latenciesNanos[p95Index] / 1_000_000.0
        val maxMs = latenciesNanos.last() / 1_000_000.0

        return Report(
            frames = frameCount,
            avgFrameMs = avgMs,
            p95FrameMs = p95Ms,
            maxFrameMs = maxMs,
            meets60Fps = p95Ms < 16.67,
            meets120Fps = p95Ms < 8.33
        )
    }
}
