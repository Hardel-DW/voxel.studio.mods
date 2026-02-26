package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ToolSectionSelector;
import fr.hardel.asset_editor.client.javafx.components.ui.ToolSlot;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.mock.StudioMockEnchantment;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantmentItemsPage extends VBox {

    private static final Map<String, String> ENCHANTABLE_ENTRIES = buildEntries();

    private final StudioContext context;
    private final VBox content = new VBox(32);
    private final ToolSectionSelector selector;
    private String currentSection = "supportedItems";
    private String currentElement = "";
    private TilePane supportedGrid;
    private TilePane primaryGrid;

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

        selector = new ToolSectionSelector(
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
        if (section.equals(currentSection)) {
            return;
        }
        currentSection = section;
        selector.setContent(currentSection.equals("primaryItems") ? primaryGrid : supportedGrid);
    }

    private void refreshForSelectedElement() {
        StudioMockEnchantment e = selectedEnchantment();
        if (!e.uniqueKey().equals(currentElement)) {
            currentElement = e.uniqueKey();
            supportedGrid = buildGrid(e, false);
            primaryGrid = buildGrid(e, true);
        }
        selector.setContent(currentSection.equals("primaryItems") ? primaryGrid : supportedGrid);
    }

    private TilePane buildGrid(StudioMockEnchantment e, boolean includePrimaryNone) {
        TilePane grid = new TilePane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPrefTileWidth(256);
        grid.setMaxWidth(Double.MAX_VALUE);

        for (Map.Entry<String, String> entry : ENCHANTABLE_ENTRIES.entrySet()) {
            String key  = entry.getKey();
            String tag  = entry.getValue();
            boolean active = e.items().contains(tag);
            Identifier img = Identifier.fromNamespaceAndPath("asset_editor", "textures/features/item/" + key + ".png");
            grid.getChildren().add(new ToolSlot(img, "enchantment:supported." + key + ".title", active));
        }

        if (includePrimaryNone) {
            Identifier noneImg = Identifier.fromNamespaceAndPath("asset_editor", "textures/tools/cross.png");
            boolean noneActive = e.items().isEmpty();
            grid.getChildren().add(new ToolSlot(noneImg, "enchantment:supported.none.title", noneActive));
        }

        return grid;
    }

    private static Map<String, String> buildEntries() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        List.of("sword", "trident", "mace", "bow", "crossbow", "range", "fishing", "shield", "weapon",
                "melee", "head_armor", "chest_armor", "leg_armor", "foot_armor", "elytra", "armor",
                "equippable", "axes", "shovels", "hoes", "pickaxes", "durability", "mining_loot")
            .forEach(k -> map.put(k, "#minecraft:enchantable/" + k));
        return map;
    }

    private StudioMockEnchantment selectedEnchantment() {
        String id = context.tabsState().currentElementId();
        if (!id.isBlank()) {
            for (StudioMockEnchantment e : context.repository().enchantments()) {
                if (e.uniqueKey().equals(id)) return e;
            }
        }
        return context.repository().enchantments().getFirst();
    }
}
