package fr.hardel.asset_editor.client.compose.components.ui.editor

import androidx.compose.ui.text.AnnotatedString

/**
 * Caches the highlighted [AnnotatedString] for each [EditableLineBuffer.Line]
 * keyed by its stable [EditableLineBuffer.Line.id]. Because line ids only
 * change when the line itself is edited, the cache automatically stays valid
 * across insertions, deletions and reorderings of *other* lines.
 *
 * The cache grows monotonically as the user edits. A naive cap is enforced
 * via trimIfTooLarge which clears the whole map once it exceeds the limit;
 * an LRU is overkill for the JSON sizes the editor targets.
 */
class LineHighlightCache(
    private val highlighter: JsonLineHighlighter,
    private val maxEntries: Int = 8192
) {
    private val cache = HashMap<Long, AnnotatedString>()

    fun get(line: EditableLineBuffer.Line): AnnotatedString {
        cache[line.id]?.let { return it }
        val built = highlighter.highlight(line.content)
        if (cache.size >= maxEntries) cache.clear()
        cache[line.id] = built
        return built
    }

    fun clear() {
        cache.clear()
    }
}
