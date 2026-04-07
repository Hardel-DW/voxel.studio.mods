package fr.hardel.asset_editor.client.compose

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.io.IOException
import java.io.InputStream

object StudioResourceLoader {
    @Throws(IOException::class)
    fun open(location: Identifier): InputStream =
        Minecraft.getInstance().resourceManager.open(location)
}