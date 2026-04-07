package fr.hardel.asset_editor.client.compose.lib.utils

import net.minecraft.resources.Identifier

object IconUtils {

    fun isSvgIcon(icon: Identifier?): Boolean {
        if (icon == null) return false
        val value = icon.toString()
        return value.startsWith("asset_editor:icons/") || value.endsWith(".svg")
    }
}
