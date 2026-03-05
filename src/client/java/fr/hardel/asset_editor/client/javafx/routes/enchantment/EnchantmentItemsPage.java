package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.SectionSelector;
import fr.hardel.asset_editor.client.javafx.components.ui.Card;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.List;

public final class EnchantmentItemsPage extends VBox {

    private static final List<String> ENCHANTABLE_KEYS = List.of(
            "sword", "trident", "mace", "bow", "crossbow", "range", "fishing", "shield", "weapon",
            "melee", "head_armor", "chest_armor", "leg_armor", "foot_armor", "elytra", "armor",
            "equippable", "axes", "shovels", "hoes", "pickaxes", "durability", "mining_loot");

    private final StudioContext context;
    private final VBox content = new VBox(32);
    private final SectionSelector selector;
    private String currentSection = "supportedItems";
    private String currentElement = "";
    private ResponsiveGrid supportedGrid;
    private ResponsiveGrid primaryGrid;

    public EnchantmentItemsPage(StudioContext context) {
        this.context = context;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("enchantment-subpage-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        content.setPadding(new Insets(32));

        LinkedHashMap<String, String> tabs = new LinkedHashMap<>();
        tabs.put("supportedItems", I18n.get("enchantment:toggle.supported.title"));
        tabs.put("primaryItems",   I18n.get("enchantment:toggle.primary.title"));

        selector = new SectionSelector(
            "enchantment:section.supported.description",
            tabs,
            currentSection,
            this::onSectionChange
        );
        content.getChildren().setAll(selector);

        context.tabsState().currentElementIdProperty().addListener((obs, o, v) -> refreshForSelectedElement());
        refreshForSelectedElement();
    }

    private void onSectionChange(String section) {
        if (section.equals(currentSection)) return;
        currentSection = section;
        var grid = currentSection.equals("primaryItems") ? primaryGrid : supportedGrid;
        if (grid == null) {
            selector.setContent();
        } else {
            selector.setContent(grid);
        }
    }

    private void refreshForSelectedElement() {
        Holder.Reference<Enchantment> holder = context.findElement(net.minecraft.core.registries.Registries.ENCHANTMENT);
        if (holder == null) {
            currentElement = "";
            supportedGrid = null;
            primaryGrid = null;
            selector.setContent();
            return;
        }
        String key = holder.key().identifier().toString();
        if (!key.equals(currentElement)) {
            currentElement = key;
            supportedGrid = buildGrid(holder, false);
            primaryGrid = buildGrid(holder, true);
        }
        selector.setContent(currentSection.equals("primaryItems") ? primaryGrid : supportedGrid);
    }

    private ResponsiveGrid buildGrid(Holder.Reference<Enchantment> holder, boolean includePrimaryNone) {
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
            .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        String supportedTag = holder.value().definition().supportedItems().unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("");

        for (String key : ENCHANTABLE_KEYS) {
            boolean active = supportedTag.equals("enchantable/" + key);
            Identifier img = Identifier.fromNamespaceAndPath("asset_editor", "textures/features/item/" + key + ".png");
            grid.addItem(new Card(img, "enchantment:supported." + key + ".title", active));
        }

        if (includePrimaryNone) {
            Identifier noneImg = Identifier.fromNamespaceAndPath("asset_editor", "textures/tools/cross.png");
            boolean noneActive = supportedTag.isEmpty();
            grid.addItem(new Card(noneImg, "enchantment:supported.none.title", noneActive));
        }

        return grid;
    }

}
