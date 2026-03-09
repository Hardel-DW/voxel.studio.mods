package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnchantmentItemsPage extends RegistryPage<Enchantment> {

    private static final List<String> ENCHANTABLE_KEYS = List.of(
            "sword", "trident", "mace", "bow", "crossbow", "range", "fishing", "shield", "weapon",
            "melee", "head_armor", "chest_armor", "leg_armor", "foot_armor", "elytra", "armor",
            "equippable", "axes", "shovels", "hoes", "pickaxes", "durability", "mining_loot");

    private final SectionSelector sectionSelector;
    private String currentSection = "supportedItems";

    private final Map<String, Card> supportedCards = new LinkedHashMap<>();
    private final Map<String, Card> primaryCards = new LinkedHashMap<>();
    private Card primaryNoneCard;
    private ResponsiveGrid supportedGrid;
    private ResponsiveGrid primaryGrid;

    public EnchantmentItemsPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 32, new Insets(32));

        LinkedHashMap<String, String> tabs = new LinkedHashMap<>();
        tabs.put("supportedItems", I18n.get("enchantment:toggle.supported.title"));
        tabs.put("primaryItems", I18n.get("enchantment:toggle.primary.title"));

        sectionSelector = new SectionSelector("enchantment:section.supported.description", tabs, currentSection, this::onSectionChange);
        content().getChildren().setAll(sectionSelector);
    }

    @Override
    protected void buildContent() {
        supportedCards.clear();
        primaryCards.clear();

        supportedGrid = buildGrid(supportedCards, false);
        primaryGrid = buildGrid(primaryCards, true);

        bindView(entry -> entry.data().definition().supportedItems().unwrapKey()
                .map(k -> k.location().getPath()).orElse(""), tag -> updateCards(supportedCards, tag));

        bindView(entry -> entry.data().definition().primaryItems()
                .flatMap(hs -> hs.unwrapKey())
                .map(k -> k.location().getPath()).orElse(""), tag -> {
            updateCards(primaryCards, tag);
            if (primaryNoneCard != null) primaryNoneCard.setActive(tag.isEmpty());
        });

        wireCardActions(supportedCards, key -> {
            var holderSet = EnchantmentMutations.resolveItemTag("enchantable/" + key);
            return holderSet != null ? EnchantmentMutations.supportedItems(holderSet) : null;
        });

        wireCardActions(primaryCards, key -> {
            var holderSet = EnchantmentMutations.resolveItemTag("enchantable/" + key);
            return holderSet != null ? EnchantmentMutations.primaryItems(Optional.of(holderSet)) : null;
        });

        if (primaryNoneCard != null) {
            primaryNoneCard.setOnMouseClicked(e ->
                    applyAction(EnchantmentMutations.primaryItems(Optional.empty())));
        }
    }

    @Override
    protected void onReady() {
        sectionSelector.setContent(currentSection.equals("primaryItems") ? primaryGrid : supportedGrid);
    }

    @Override
    protected void onNoElement() {
        sectionSelector.setContent();
    }

    private ResponsiveGrid buildGrid(Map<String, Card> cardMap, boolean includePrimaryNone) {
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (String key : ENCHANTABLE_KEYS) {
            Identifier img = Identifier.fromNamespaceAndPath("asset_editor", "textures/features/item/" + key + ".png");
            Card card = new Card(img, "enchantment:supported." + key + ".title", false);
            cardMap.put(key, card);
            grid.addItem(card);
        }

        if (includePrimaryNone) {
            Identifier noneImg = Identifier.fromNamespaceAndPath("asset_editor", "textures/tools/cross.png");
            primaryNoneCard = new Card(noneImg, "enchantment:supported.none.title", false);
            grid.addItem(primaryNoneCard);
        }

        return grid;
    }

    private void wireCardActions(Map<String, Card> cardMap,
                                  java.util.function.Function<String, java.util.function.UnaryOperator<Enchantment>> mutationFactory) {
        for (var entry : cardMap.entrySet()) {
            String key = entry.getKey();
            entry.getValue().setOnMouseClicked(e -> {
                var mutation = mutationFactory.apply(key);
                if (mutation != null) applyAction(mutation);
            });
        }
    }

    private void updateCards(Map<String, Card> cardMap, String tag) {
        for (var e : cardMap.entrySet()) {
            e.getValue().setActive(tag.equals("enchantable/" + e.getKey()));
        }
    }

    private void onSectionChange(String section) {
        if (section.equals(currentSection)) return;
        currentSection = section;
        var grid = currentSection.equals("primaryItems") ? primaryGrid : supportedGrid;
        if (grid != null) sectionSelector.setContent(grid);
        else sectionSelector.setContent();
    }

}
