package fr.hardel.asset_editor.network.structure;

import java.util.Set;

/** Single source of truth for which {@code StructureType}s the editor can preview client-side. */
public final class StructureTypeSupport {

    private static final Set<String> PROCEDURAL_TYPES = Set.of(
        "minecraft:stronghold",
        "minecraft:mineshaft",
        "minecraft:fortress",
        "minecraft:ocean_monument",
        "minecraft:woodland_mansion",
        "minecraft:buried_treasure",
        "minecraft:desert_pyramid",
        "minecraft:swamp_hut",
        "minecraft:jungle_temple"
    );

    public static boolean isPreviewable(String type) {
        return type != null && !type.isBlank() && !PROCEDURAL_TYPES.contains(type);
    }

    private StructureTypeSupport() {}
}
