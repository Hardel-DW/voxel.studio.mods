package fr.hardel.asset_editor.data.concept;

import fr.hardel.asset_editor.data.StudioRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

public final class StudioTagKeys {

    public static TagKey<StudioEditorTabDef> tab(Identifier id) {
        return TagKey.create(StudioRegistries.STUDIO_TAB, id);
    }

    private StudioTagKeys() {}
}
