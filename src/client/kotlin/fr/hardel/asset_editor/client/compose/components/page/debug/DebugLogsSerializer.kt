package fr.hardel.asset_editor.client.compose.components.page.debug

import fr.hardel.asset_editor.client.memory.session.debug.DebugLogMemory
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun copyDebugLogsToClipboard(entries: List<DebugLogMemory.Entry>) {
    val serialized = buildString {
        append("[\n")
        entries.forEachIndexed { index, entry ->
            append("  {\"timestamp\":${entry.timestamp()},\"level\":\"${entry.level()}\",\"category\":\"${entry.category()}\",\"message\":")
            append("\"${entry.message().replace("\"", "\\\"")}\"")
            if (entry.data().isNotEmpty()) {
                append(",\"data\":{")
                entry.data().entries.joinToString(",") {
                    "\"${it.key}\":\"${it.value.replace("\"", "\\\"")}\""
                }.also(::append)
                append("}")
            }
            append("}")
            if (index < entries.lastIndex) append(",")
            append("\n")
        }
        append("]")
    }
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(serialized), null)
}
