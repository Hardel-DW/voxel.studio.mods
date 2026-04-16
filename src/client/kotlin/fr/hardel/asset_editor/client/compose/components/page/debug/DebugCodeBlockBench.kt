package fr.hardel.asset_editor.client.compose.components.page.debug

class BenchTimings {
    private val entries = LinkedHashMap<String, Long>()

    fun <T> measure(stage: String, block: () -> T): T {
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

data class BenchEntry(val stage: String, val millis: Long)
