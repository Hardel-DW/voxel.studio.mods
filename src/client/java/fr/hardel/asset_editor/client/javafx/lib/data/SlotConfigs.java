package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SlotConfigs {

    public record SlotConfig(String id, String nameKey, Identifier image, List<String> slots) {}

    public static final List<SlotConfig> ALL = List.of(
        new SlotConfig("mainhand", "enchantment:slots.mainhand.title", slotImage("mainhand"), List.of("mainhand", "any", "hand", "all")),
        new SlotConfig("offhand",  "enchantment:slots.offhand.title",  slotImage("offhand"),  List.of("offhand", "any", "hand", "all")),
        new SlotConfig("body",     "enchantment:slots.body.title",     slotImage("body"),     List.of("body", "any", "all")),
        new SlotConfig("saddle",   "enchantment:slots.saddle.title",   slotImage("saddle"),   List.of("saddle", "any", "all")),
        new SlotConfig("head",     "enchantment:slots.head.title",     slotImage("head"),     List.of("head", "any", "armor", "all")),
        new SlotConfig("chest",    "enchantment:slots.chest.title",    slotImage("chest"),    List.of("chest", "any", "armor", "all")),
        new SlotConfig("legs",     "enchantment:slots.legs.title",     slotImage("legs"),     List.of("legs", "any", "armor", "all")),
        new SlotConfig("feet",     "enchantment:slots.feet.title",     slotImage("feet"),     List.of("feet", "any", "armor", "all"))
    );

    public static final Map<String, SlotConfig> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(SlotConfig::id, Function.identity()));

    private static Identifier slotImage(String name) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/slots/" + name + ".png");
    }

    private SlotConfigs() {}
}
