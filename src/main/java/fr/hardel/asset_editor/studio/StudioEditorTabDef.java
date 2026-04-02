package fr.hardel.asset_editor.studio;

import com.mojang.serialization.Codec;
public record StudioEditorTabDef() {

    public static final Codec<StudioEditorTabDef> CODEC = Codec.unit(new StudioEditorTabDef());
}
