package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SlotConfigs {

    public record SlotConfig(String id, List<String> slots) {

        public Identifier image() {
            return SlotConfigs.slotImage(id);
        }
    }

    public static final List<SlotConfig> ALL = List.of(
        new SlotConfig("mainhand", List.of("mainhand", "any", "hand", "all")),
        new SlotConfig("offhand",  List.of("offhand", "any", "hand", "all")),
        new SlotConfig("body",     List.of("body", "any", "all")),
        new SlotConfig("saddle",   List.of("saddle", "any", "all")),
        new SlotConfig("head",     List.of("head", "any", "armor", "all")),
        new SlotConfig("chest",    List.of("chest", "any", "armor", "all")),
        new SlotConfig("legs",     List.of("legs", "any", "armor", "all")),
        new SlotConfig("feet",     List.of("feet", "any", "armor", "all"))
    );

    public static final Map<String, SlotConfig> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(SlotConfig::id, Function.identity()));

    private static Identifier slotImage(String name) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/studio/slots/" + name + ".png");
    }

    private SlotConfigs() {}
}
