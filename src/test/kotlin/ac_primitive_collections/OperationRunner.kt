// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.fail

/** The seed and operation count for the differential tests. Override with -Dseed= and -Dops=. */
object TestConfig {
    val seedOverride: Long? = System.getProperty("seed")?.toLong()
    val operations: Int = System.getProperty("ops")?.toInt() ?: 10_000
    val seeds: List<Long> = seedOverride?.let(::listOf) ?: listOf(1L, 2L, 3L, 4L)
}

/**
 * Runs an operation list against fresh state and reports the shortest failing list it can find.
 *
 * [check] gets a list of ops and throws if the implementation disagrees with the model.
 * Operations must not depend on earlier randomness, so that any sublist is still a valid test.
 */
object OperationRunner {
    fun <Op> run(label: String, seed: Long, ops: List<Op>, check: (List<Op>) -> Unit) {
        val failure = runCatching { check(ops) }.exceptionOrNull() ?: return
        val shortest = minimize(ops) { candidate -> runCatching { check(candidate) }.isFailure }
        val cause = runCatching { check(shortest) }.exceptionOrNull() ?: failure
        fail(
            "$label failed. Reproduce with -Dseed=$seed. Shortened from ${ops.size} to ${shortest.size} ops:\n" +
                shortest.joinToString("\n") + "\n${cause.stackTraceToString()}",
        )
    }

    /** Zeller and Hildebrandt's ddmin, restricted to removing complements. */
    fun <Op> minimize(ops: List<Op>, fails: (List<Op>) -> Boolean): List<Op> {
        var current = ops
        var chunks = 2
        while (current.size >= 2) {
            val chunkSize = maxOf(1, current.size / chunks)
            var reduced = false
            var start = 0
            while (start < current.size) {
                val candidate = current.subList(0, start) + current.subList(minOf(start + chunkSize, current.size), current.size)
                if (candidate.isNotEmpty() && fails(candidate)) {
                    current = candidate
                    chunks = maxOf(chunks - 1, 2)
                    reduced = true
                    break
                }
                start += chunkSize
            }
            if (!reduced) {
                if (chunkSize == 1) break
                chunks = minOf(chunks * 2, current.size)
            }
        }
        return current
    }
}

/** Operation mixes from docs/PLAN.md, section 7.2. Each weight is relative. */
enum class Distribution(val addWeight: Int, val removeWeight: Int, val otherWeight: Int) {
    BALANCED(3, 2, 4),
    ADD_HEAVY(8, 1, 2),
    REMOVE_HEAVY(2, 6, 2),
    NEAR_EMPTY(1, 3, 1),
}

/** Value domains that exercise duplicates, extremes, and monotonic input. */
enum class ValueDomain { SMALL, FULL, BOUNDARY, MONOTONIC }

fun Random.nextWeighted(distribution: Distribution): Int {
    val total = distribution.addWeight + distribution.removeWeight + distribution.otherWeight
    val roll = nextInt(total)
    return when {
        roll < distribution.addWeight -> 0
        roll < distribution.addWeight + distribution.removeWeight -> 1
        else -> 2
    }
}
