package fr.hardel.asset_editor.mixin.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {

    @Accessor("texturesByName")
    Map<Identifier, TextureAtlasSprite> asset_editor$getTexturesByName();

    @Accessor("width")
    int asset_editor$getWidth();

    @Accessor("height")
    int asset_editor$getHeight();
}
