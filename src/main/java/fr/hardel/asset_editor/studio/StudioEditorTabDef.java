package fr.hardel.asset_editor.studio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
public record StudioEditorTabDef() {

    public static final Codec<StudioEditorTabDef> CODEC = MapCodec.unit(StudioEditorTabDef::new).codec();
}
