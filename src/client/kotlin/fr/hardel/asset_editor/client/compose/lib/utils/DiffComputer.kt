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
        val table = computeLCSTable(originalLines, modifiedLines)
        val result = ArrayList<DiffLine>()

        var i = 0
        var j = 0
        var modifiedLineNum = 1

        while (i < originalLines.size || j < modifiedLines.size) {
            when {
                i >= originalLines.size -> {
                    result += DiffLine(DiffLineType.ADDED, modifiedLines[j], modifiedLineNum++)
                    j++
                }
                j >= modifiedLines.size -> {
                    result += DiffLine(DiffLineType.REMOVED, originalLines[i], null)
                    i++
                }
                originalLines[i] == modifiedLines[j] -> {
                    result += DiffLine(DiffLineType.UNCHANGED, modifiedLines[j], modifiedLineNum++)
                    i++
                    j++
                }
                table[i + 1][j] >= table[i][j + 1] -> {
                    result += DiffLine(DiffLineType.REMOVED, originalLines[i], null)
                    i++
                }
                else -> {
                    result += DiffLine(DiffLineType.ADDED, modifiedLines[j], modifiedLineNum++)
                    j++
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

    private fun computeLCSTable(original: List<String>, modified: List<String>): Array<IntArray> {
        val m = original.size
        val n = modified.size
        val table = Array(m + 1) { IntArray(n + 1) }

        for (i in m - 1 downTo 0) {
            for (j in n - 1 downTo 0) {
                table[i][j] = if (original[i] == modified[j]) {
                    1 + table[i + 1][j + 1]
                } else {
                    maxOf(table[i + 1][j], table[i][j + 1])
                }
            }
        }

        return table
    }
}
