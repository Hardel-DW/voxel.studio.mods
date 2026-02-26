package fr.hardel.asset_editor.client.javafx.lib.data;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SlotConfigs {

    public static final List<SlotConfig> ALL = List.of(
        new SlotConfig("mainhand", "enchantment:slots.mainhand.title", SlotConfig.featureImage("mainhand"), List.of("mainhand", "any", "hand")),
        new SlotConfig("offhand",  "enchantment:slots.offhand.title",  SlotConfig.featureImage("offhand"),  List.of("offhand", "any", "hand")),
        new SlotConfig("body",     "enchantment:slots.body.title",     SlotConfig.featureImage("body"),     List.of("body", "any")),
        new SlotConfig("saddle",   "enchantment:slots.saddle.title",   SlotConfig.featureImage("saddle"),   List.of("saddle", "any")),
        new SlotConfig("head",     "enchantment:slots.head.title",     SlotConfig.featureImage("head"),     List.of("head", "any", "armor")),
        new SlotConfig("chest",    "enchantment:slots.chest.title",    SlotConfig.featureImage("chest"),    List.of("chest", "any", "armor")),
        new SlotConfig("legs",     "enchantment:slots.legs.title",     SlotConfig.featureImage("legs"),     List.of("legs", "any", "armor")),
        new SlotConfig("feet",     "enchantment:slots.feet.title",     SlotConfig.featureImage("feet"),     List.of("feet", "any", "armor"))
    );
    public static final Map<String, SlotConfig> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(SlotConfig::id, Function.identity()));

    private SlotConfigs() {}
}
