package fr.hardel.asset_editor.client.compose.components.ui.scene

private const val MAX_ENTRIES = 8

object Scene3DStateMemory {

    private val cache = object : java.util.LinkedHashMap<String, Scene3DState>(MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Scene3DState>): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun obtain(key: String, defaultCamera: () -> Scene3DCamera): Scene3DState {
        val existing = cache[key]
        if (existing != null) return existing
        val created = Scene3DState(defaultCamera())
        cache[key] = created
        return created
    }

    @Synchronized
    fun invalidate(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
