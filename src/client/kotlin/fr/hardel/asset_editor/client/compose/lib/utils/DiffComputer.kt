package fr.hardel.asset_editor.client.compose.lib.utils

enum class DiffLineType { UNCHANGED, ADDED, REMOVED }

data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val lineNumber: Int?
)

object DiffComputer {

    fun computeUnifiedDiff(original: String, modified: String): List<DiffLine> {
        val originalLines = original.split("\n")
        val modifiedLines = modified.split("\n")
        val (changedA, changedB) = HistogramDiff.diff(originalLines, modifiedLines)

        val result = ArrayList<DiffLine>(originalLines.size + modifiedLines.size)
        var i = 0
        var j = 0
        var modifiedLineNum = 1

        while (i < originalLines.size || j < modifiedLines.size) {
            val removed = i < originalLines.size && changedA[i]
            val added = j < modifiedLines.size && changedB[j]
            when {
                removed -> result += DiffLine(DiffLineType.REMOVED, originalLines[i++], null)
                added -> result += DiffLine(DiffLineType.ADDED, modifiedLines[j++], modifiedLineNum++)
                else -> {
                    result += DiffLine(DiffLineType.UNCHANGED, modifiedLines[j], modifiedLineNum++)
                    i++; j++
                }
            }
        }

        return result
    }

    fun computeFullDiff(text: String, type: DiffLineType): List<DiffLine> {
        val isAdded = type == DiffLineType.ADDED
        return text.split("\n").mapIndexed { index, content ->
            DiffLine(type, content, if (isAdded) index + 1 else null)
        }
    }
}
