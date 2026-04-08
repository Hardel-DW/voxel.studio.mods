package fr.hardel.asset_editor.client.compose.routes.debug

/**
 * Tiny accumulator used by the debug page to record stage timings.
 * Not exposed outside the debug routes.
 */
internal class BenchTimings {
    private val entries = LinkedHashMap<String, Long>()

    inline fun <T> measure(stage: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        entries[stage] = (System.nanoTime() - start) / 1_000_000
        return result
    }

    fun set(stage: String, millis: Long) {
        entries[stage] = millis
    }

    fun snapshot(): List<BenchEntry> = entries.map { (stage, ms) -> BenchEntry(stage, ms) }
}

internal data class BenchEntry(val stage: String, val millis: Long)
