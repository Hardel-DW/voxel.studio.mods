package fr.hardel.asset_editor.client.compose.components.page.changes

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

fun isPreviewableDiffPath(path: String): Boolean =
    path.endsWith(".json") || path.endsWith(".mcfunction") || path.endsWith(".mcmeta")

fun formatDiffContentIfJson(path: String, content: String): String {
    if (!path.endsWith(".json") && !path.endsWith(".mcmeta")) return content
    if (content.isBlank()) return content
    return runCatching {
        val element = JsonParser.parseString(content)
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element)
    }.getOrElse { content }
}
