package fr.hardel.asset_editor.client.resource

import java.io.IOException
import java.io.InputStream
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

object StudioResourceLoader {
    @Throws(IOException::class)
    fun open(location: Identifier): InputStream =
        Minecraft.getInstance().resourceManager.open(location)
}
