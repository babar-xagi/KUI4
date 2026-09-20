package kui.build

import java.util.Locale

/**
 * Monotonic duration measurement helper (Phase 011).
 */
class Stopwatch private constructor(private val startNanos: Long) {
    companion object {
        fun start(): Stopwatch = Stopwatch(System.nanoTime())

        inline fun <T> measure(block: () -> T): Pair<T, Long> {
            val sw = start()
            val result = block()
            return result to sw.elapsedMillis()
        }
    }

    fun elapsedNanos(): Long = System.nanoTime() - startNanos

    fun elapsedMillis(): Long = elapsedNanos() / 1_000_000L

    fun format(): String {
        val ms = elapsedMillis()
        return when {
            ms < 1000 -> "${ms}ms"
            else -> String.format(Locale.US, "%.2fs", ms / 1000.0)
        }
    }
}
