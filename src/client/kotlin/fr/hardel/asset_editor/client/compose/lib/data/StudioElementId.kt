package fr.hardel.asset_editor.client.compose.lib.data

import net.minecraft.resources.Identifier

data class StudioElementId(val identifier: Identifier, val registry: String?) {

    fun namespace(): String = identifier.namespace

    fun resourcePath(): String = identifier.path

    companion object {
        @JvmStatic
        fun parse(raw: String?): StudioElementId? {
            if (raw.isNullOrBlank()) {
                return null
            }

            val parts = raw.split("|", limit = 2)
            val idPart = parts[0]
            val registryPart = if (parts.size == 2) parts[1] else null
            val identifier = Identifier.tryParse(idPart) ?: return null
            return StudioElementId(identifier, registryPart?.takeUnless { it.isBlank() })
        }
    }
}
