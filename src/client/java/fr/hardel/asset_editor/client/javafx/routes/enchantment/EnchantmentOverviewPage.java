package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.Row;
import fr.hardel.asset_editor.client.javafx.lib.InfiniteScrollPane;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentMutations;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs;
import fr.hardel.asset_editor.client.javafx.lib.data.SlotConfigs.SlotConfig;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EnchantmentOverviewPage extends VBox implements Page {

    private final StudioContext context;
    private final StackPane content = new StackPane();
    private final InfiniteScrollPane<ElementEntry<Enchantment>> scrollPane;
    private final Runnable storeListener = this::refresh;

    public EnchantmentOverviewPage(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-overview-page");

        TextField search = new TextField();
        search.getStyleClass().add("enchantment-overview-search");
        search.setPromptText(I18n.get("enchantment:overview.search"));
        search.textProperty().bindBidirectional(context.uiState().searchProperty());

        VBox toolbar = new VBox(search);
        toolbar.getStyleClass().add("enchantment-overview-toolbar");
        toolbar.setPadding(new Insets(16, 32, 16, 32));

        scrollPane = new InfiniteScrollPane<>(16, this::buildRow);
        scrollPane.getStyleClass().add("enchantment-overview-scroll");
        scrollPane.content().getStyleClass().add("enchantment-overview-list");
        scrollPane.content().setPadding(new Insets(24, 32, 24, 32));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, content);

        context.uiState().searchProperty().addListener((obs, o, v) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, o, v) -> refresh());
        context.uiState().sidebarViewProperty().addListener((obs, o, v) -> refresh());
    }

    @Override
    public void onActivate() {
        context.elementStore().subscribeRegistry(Registries.ENCHANTMENT, storeListener);
        refresh();
    }

    @Override
    public void onDeactivate() {
        context.elementStore().unsubscribeRegistry(Registries.ENCHANTMENT, storeListener);
    }

    private Row buildRow(ElementEntry<Enchantment> entry) {
        Row row = new Row();
        row.getStyleClass().add("enchantment-row");
        row.setOnClick(() -> open(entry.id()));
        row.setIcon(buildIcon(entry));

        Label name = new Label(entry.data().description().getString());
        name.getStyleClass().add("enchantment-row-name");

        Label identifier = new Label(entry.id().toString());
        identifier.getStyleClass().add("enchantment-row-identifier");

        Label bullet = new Label("\u2022");
        bullet.getStyleClass().add("enchantment-row-separator");

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + entry.data().getMaxLevel());
        level.getStyleClass().add("enchantment-row-level");

        HBox subInfo = new HBox(8, identifier, bullet, level);
        subInfo.setAlignment(Pos.CENTER_LEFT);

        row.setContent(name, subInfo);

        var sw = row.toggle();
        sw.setValue(!EnchantmentMutations.isSoftDeleted(entry));
        sw.valueProperty().addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v)) return;
            var current = context.elementStore().get(Registries.ENCHANTMENT, entry.id());
            boolean enabled = current == null || !EnchantmentMutations.isSoftDeleted(current);
            if (Boolean.valueOf(v).equals(enabled)) return;
            var result = context.gateway().applyCustom(Registries.ENCHANTMENT, entry.id(), EnchantmentMutations.toggleDisabled());
            if (!result.isApplied()) sw.setValue(enabled);
        });

        return row;
    }

    private static StackPane buildIcon(ElementEntry<Enchantment> entry) {
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("enchantment-row-icon");
        iconWrap.setMinSize(32, 32);
        iconWrap.setPrefSize(32, 32);
        iconWrap.setMaxSize(32, 32);
        var texture = EnchantmentMutations.previewTexture(entry.data());
        if (texture != null) {
            iconWrap.getChildren().add(new ResourceImageIcon(texture, 32));
        } else {
            Label fallback = new Label("?");
            fallback.getStyleClass().add("enchantment-row-placeholder");
            iconWrap.getChildren().add(fallback);
        }
        return iconWrap;
    }

    private void refresh() {
        List<ElementEntry<Enchantment>> entries = filteredEntries();
        if (entries.isEmpty()) {
            content.getChildren().setAll(emptyState());
            return;
        }
        scrollPane.setItems(entries);
        content.getChildren().setAll(scrollPane);
    }

    private List<ElementEntry<Enchantment>> filteredEntries() {
        String search = context.uiState().search() == null ? "" : context.uiState().search().trim().toLowerCase(Locale.ROOT);
        String filterPath = context.uiState().filterPath() == null ? "" : context.uiState().filterPath().trim().toLowerCase(Locale.ROOT);
        StudioSidebarView sidebarView = context.uiState().sidebarView();

        return context.allTypedEntries(Registries.ENCHANTMENT).stream()
                .filter(e -> search.isEmpty() || e.id().getPath().contains(search))
                .filter(e -> matchesFilter(e, filterPath, sidebarView))
                .toList();
    }

    private static boolean matchesFilter(ElementEntry<Enchantment> entry, String filterPath, StudioSidebarView sidebarView) {
        if (filterPath.isEmpty()) return true;
        ElementEntry<Enchantment> effectiveEntry = EnchantmentMutations.prepareForFlush(entry);
        String[] parts = filterPath.split("/", 2);
        String category = parts[0];
        String leaf = parts.length == 2 ? parts[1] : "";
        if (!leaf.isEmpty() && !effectiveEntry.id().getPath().equals(leaf)) return false;

        Enchantment enchantment = effectiveEntry.data();
        if (sidebarView == StudioSidebarView.SLOTS) {
            SlotConfig config = SlotConfigs.BY_ID.get(category);
            if (config == null) return false;
            return enchantment.definition().slots().stream()
                    .anyMatch(g -> config.slots().contains(g.getSerializedName()));
        }
        if (sidebarView == StudioSidebarView.ITEMS) {
            String itemTag = "enchantable/" + category;
            boolean supported = enchantment.definition().supportedItems().unwrapKey()
                    .map(tag -> tag.location().getPath().equals(itemTag))
                    .orElse(false);
            if (supported) return true;
            boolean primary = enchantment.definition().primaryItems()
                    .flatMap(hs -> hs.unwrapKey())
                    .map(tag -> tag.location().getPath().equals(itemTag))
                    .orElse(false);
            if (primary) return true;
            return effectiveEntry.tags().stream().anyMatch(t -> t.getPath().equals(itemTag));
        }
        var tagKey = enchantment.exclusiveSet().unwrapKey();
        if (tagKey.isPresent()) {
            String full = tagKey.get().location().toString().toLowerCase(Locale.ROOT);
            String path = tagKey.get().location().getPath().toLowerCase(Locale.ROOT);
            return full.equals(category) || path.equals(category);
        }
        return enchantment.exclusiveSet().stream()
                .map(holder -> holder.unwrapKey().map(k -> k.identifier()).orElse(null))
                .filter(Objects::nonNull)
                .anyMatch(id -> id.toString().toLowerCase(Locale.ROOT).equals(category)
                        || id.getPath().toLowerCase(Locale.ROOT).equals(category));
    }

    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/search.svg");

    private VBox emptyState() {
        var icon = new SvgIcon(SEARCH_ICON, 40, Color.WHITE);
        icon.setOpacity(0.2);
        StackPane circle = new StackPane(icon);
        circle.getStyleClass().add("enchantment-overview-empty-circle");
        circle.setPrefSize(96, 96);
        circle.setMaxSize(96, 96);
        circle.setAlignment(Pos.CENTER);

        Label title = new Label(I18n.get("enchantment:items.no_results.title"));
        title.getStyleClass().add("enchantment-overview-empty-title");
        Label body = new Label(I18n.get("enchantment:items.no_results.description"));
        body.getStyleClass().add("enchantment-overview-empty-body");
        body.setWrapText(true);

        VBox box = new VBox(6, circle, title, body);
        VBox.setMargin(circle, new Insets(0, 0, 18, 0));
        box.getStyleClass().add("enchantment-overview-empty");
        box.setOpacity(0.6);
        return box;
    }

    private void open(Identifier id) {
        context.tabsState().openElement(id.toString(), StudioRoute.ENCHANTMENT_MAIN);
        context.router().navigate(StudioRoute.ENCHANTMENT_MAIN);
    }
}
