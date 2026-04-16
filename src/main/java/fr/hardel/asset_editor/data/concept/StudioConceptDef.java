package fr.hardel.asset_editor.data.concept;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StudioConceptDef(
    List<Identifier> registries,
    Identifier defaultEditorTab,
    Either<List<Identifier>, Identifier> tabs
) {

    public StudioConceptDef {
        registries = List.copyOf(registries);
        if (registries.isEmpty())
            throw new IllegalArgumentException("StudioConceptDef must reference at least one registry");
    }

    public Identifier registry() {
        return registries.getFirst();
    }

    public boolean hasRegistry(Identifier registryId) {
        return registries.contains(registryId);
    }

    private static final Codec<List<Identifier>> REGISTRY_CODEC = Codec
        .either(Identifier.CODEC.listOf(), Identifier.CODEC)
        .xmap(
            either -> either.map(list -> list, List::of),
            list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
        );

    public static final Codec<StudioConceptDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        REGISTRY_CODEC.fieldOf("registry").forGetter(StudioConceptDef::registries),
        Identifier.CODEC.fieldOf("default_editor_tab").forGetter(StudioConceptDef::defaultEditorTab),
        Codec.either(Identifier.CODEC.listOf(), Identifier.CODEC).fieldOf("tabs").forGetter(StudioConceptDef::tabs)
    ).apply(instance, StudioConceptDef::new));
}
