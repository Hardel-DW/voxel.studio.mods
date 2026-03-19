package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentTreeData;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.client.javafx.lib.StudioText;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import javafx.geometry.Insets;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class EnchantmentItemsPage extends RegistryPage<Enchantment> {

    private final SectionSelector sectionSelector;
    private String currentSection = "supportedItems";

    private final Map<Identifier, Card> supportedCards = new LinkedHashMap<>();
    private final Map<Identifier, Card> primaryCards = new LinkedHashMap<>();
    private Card primaryNoneCard;
    private ResponsiveGrid supportedGrid;
    private ResponsiveGrid primaryGrid;

    public EnchantmentItemsPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "enchantment-subpage-scroll", 32, new Insets(32));

        LinkedHashMap<String, String> tabs = new LinkedHashMap<>();
        tabs.put("supportedItems", I18n.get("enchantment:toggle.supported.title"));
        tabs.put("primaryItems", I18n.get("enchantment:toggle.primary.title"));

        sectionSelector = new SectionSelector(I18n.get("enchantment:section.supported.description"), tabs, currentSection, this::onSectionChange);
        content().getChildren().setAll(sectionSelector);
    }

    @Override
    protected void buildContent() {
        supportedCards.clear();
        primaryCards.clear();
        supportedGrid = buildGrid(supportedCards, false);
        primaryGrid = buildGrid(primaryCards, true);

        bindView(entry -> entry.data().definition().supportedItems().unwrapKey()
            .map(k -> k.location()).orElse(null), tag -> updateCards(supportedCards, tag));

        bindView(entry -> entry.data().definition().primaryItems()
            .flatMap(hs -> hs.unwrapKey())
            .map(k -> k.location()).orElse(null), tag -> {
                updateCards(primaryCards, tag);
                if (primaryNoneCard != null)
                    primaryNoneCard.setActive(tag == null);
            });

        wireCardActions(supportedCards, tagId -> new EditorAction.SetSupportedItems(tagId.toString()));
        wireCardActions(primaryCards, tagId -> new EditorAction.SetPrimaryItems(tagId.toString()));

        if (primaryNoneCard != null) {
            primaryNoneCard.setOnMouseClicked(e -> context().gateway().dispatch(Registries.ENCHANTMENT, currentId(), new EditorAction.SetPrimaryItems("")));
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

    private ResponsiveGrid buildGrid(Map<Identifier, Card> cardMap, boolean includePrimaryNone) {
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        for (EnchantmentTreeData.ItemTagConfig tag : EnchantmentTreeData.ITEM_TAGS) {
            Card card = new Card(tag.icon(), StudioText.resolve("item_tag", tag.tagId()), false);
            cardMap.put(tag.tagId(), card);
            grid.addItem(card);
        }

        if (includePrimaryNone) {
            Identifier noneImg = Identifier.fromNamespaceAndPath("asset_editor", "textures/cross.png");
            primaryNoneCard = new Card(noneImg, I18n.get("enchantment.supported:none"), false);
            grid.addItem(primaryNoneCard);
        }

        return grid;
    }

    private void wireCardActions(Map<Identifier, Card> cardMap,
        Function<Identifier, EditorAction> actionFactory) {
        for (var entry : cardMap.entrySet()) {
            Identifier key = entry.getKey();
            entry.getValue().setOnMouseClicked(e -> context().gateway().dispatch(Registries.ENCHANTMENT, currentId(), actionFactory.apply(key)));
        }
    }

    private void updateCards(Map<Identifier, Card> cardMap, Identifier tag) {
        for (var e : cardMap.entrySet()) {
            e.getValue().setActive(e.getKey().equals(tag));
        }
    }

    private void onSectionChange(String section) {
        if (section.equals(currentSection))
            return;
        currentSection = section;
        var grid = currentSection.equals("primaryItems") ? primaryGrid : supportedGrid;
        if (grid != null)
            sectionSelector.setContent(grid);
        else
            sectionSelector.setContent();
    }

}
