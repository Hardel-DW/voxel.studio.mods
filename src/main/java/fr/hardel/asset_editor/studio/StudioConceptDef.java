package fr.hardel.asset_editor.studio;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.List;

public record StudioConceptDef(
    Identifier registry,
    Identifier defaultEditorTab,
    Either<List<Identifier>, Identifier> tabs
) {

    public static final Codec<StudioConceptDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("registry").forGetter(StudioConceptDef::registry),
        Identifier.CODEC.fieldOf("default_editor_tab").forGetter(StudioConceptDef::defaultEditorTab),
        Codec.either(Identifier.CODEC.listOf(), Identifier.CODEC).fieldOf("tabs").forGetter(StudioConceptDef::tabs)
    ).apply(instance, StudioConceptDef::new));

    public ResourceKey<net.minecraft.core.Registry<Object>> registryKey() {
        return ResourceKey.createRegistryKey(registry);
    }

    public List<Identifier> tabIds() {
        return tabs.left().orElse(List.of());
    }

    public Identifier tabsTag() {
        return tabs.right().orElse(null);
    }

    public TagKey<StudioEditorTabDef> tabsTagKey() {
        Identifier tag = tabsTag();
        return tag == null ? null : StudioTagKeys.tab(tag);
    }
}
