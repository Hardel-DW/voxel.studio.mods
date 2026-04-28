package fr.hardel.asset_editor.client.compose

import java.util.Locale
import net.minecraft.resources.Identifier

object StudioText {

    fun humanize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
    }

    fun humanize(id: Identifier?): String {
        if (id == null) return ""
        return humanize(id.path.substringAfterLast('/'))
    }

    fun pathParents(id: Identifier, separator: String = " / "): String {
        val parts = id.path.split('/')
        if (parts.size <= 1) return ""
        return parts.dropLast(1).joinToString(separator) { humanize(it) }
    }
}
