package ui4.core

import java.util.concurrent.atomic.AtomicLong

/**
 * Strongly typed node identifier (Phase 057).
 */
data class NodeId(val value: Long) : Comparable<NodeId> {
    override fun compareTo(other: NodeId): Int = value.compareTo(other.value)
    override fun toString(): String = "#$value"
}

/**
 * Thread-safe generator for sequential NodeIds.
 */
object NodeIdGenerator {
    private val counter = AtomicLong(1L)

    fun next(): NodeId = NodeId(counter.getAndIncrement())

    fun reset(startValue: Long = 1L) {
        counter.set(startValue)
    }
}
