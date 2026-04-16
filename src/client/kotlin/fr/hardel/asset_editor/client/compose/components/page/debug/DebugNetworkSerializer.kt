package fr.hardel.asset_editor.client.compose.components.page.debug

import fr.hardel.asset_editor.client.memory.session.debug.NetworkTraceMemory
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun copyNetworkEntriesToClipboard(entries: List<NetworkTraceMemory.TraceEntry>) {
    val json = entries.joinToString(",\n  ", "[\n  ", "\n]") { serializeNetworkEntry(it) }
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(json), null)
}

fun serializeNetworkEntry(entry: NetworkTraceMemory.TraceEntry): String {
    val payloadJson = serializeNetworkPayload(entry.payload())
    return buildString {
        append("{\"id\":").append(entry.id())
        append(",\"timestamp\":").append(entry.timestamp())
        append(",\"direction\":\"").append(entry.direction()).append('"')
        append(",\"payloadId\":\"").append(entry.payloadId()).append('"')
        if (payloadJson != null) {
            append(",\"payload\":").append(payloadJson)
        }
        append('}')
    }
}

private fun serializeNetworkPayload(payload: Any?): String? {
    if (payload == null || !payload.javaClass.isRecord) return null
    val components = payload.javaClass.recordComponents ?: return null
    if (components.isEmpty()) return null

    val fields = components.mapNotNull { component ->
        val value = component.accessor.invoke(payload)
        if (value is Collection<*> || (value != null && value.javaClass.isArray)) return@mapNotNull null
        "\"${component.name}\":${serializeNetworkValue(value)}"
    }.joinToString(",")
    return "{$fields}"
}

private fun serializeNetworkValue(value: Any?): String = when {
    value == null -> "null"
    value is String -> "\"${value.replace("\"", "\\\"")}\""
    value is Number || value is Boolean -> value.toString()
    value.javaClass.isRecord -> serializeNetworkPayload(value) ?: "\"${value}\""
    value.javaClass.isEnum -> "\"${value}\""
    else -> "\"${value}\""
}
