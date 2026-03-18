package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.ui.ItemSprite;
import fr.hardel.asset_editor.client.javafx.components.ui.Row;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.InfiniteScrollPane;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.EnchantmentViewMatchers;
import fr.hardel.asset_editor.client.state.WorkspaceUiState;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.network.workspace.EditorAction;
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EnchantmentOverviewPage extends VBox implements Page {

    private final StudioContext context;
    private final StackPane content = new StackPane();
    private final InfiniteScrollPane<ElementEntry<Enchantment>> scrollPane;
    private final FxSelectionBindings fieldBindings = new FxSelectionBindings();
    private final FxSelectionBindings pageBindings = new FxSelectionBindings();
    private final Runnable storeListener = this::refresh;

    public EnchantmentOverviewPage(StudioContext context) {
        this.context = context;
        getStyleClass().add("editor-overview-page");

        TextField search = new TextField();
        search.getStyleClass().add("editor-overview-search");
        search.setPromptText(I18n.get("enchantment:overview.search"));
        fieldBindings.bind(search.textProperty(), context.uiState().select(WorkspaceUiState.Snapshot::search));
        search.textProperty().addListener((obs, oldValue, newValue) -> context.uiState().setSearch(newValue));

        VBox toolbar = new VBox(search);
        toolbar.getStyleClass().add("editor-overview-toolbar");
        toolbar.setPadding(new Insets(16, 32, 16, 32));

        scrollPane = new InfiniteScrollPane<>(16, this::buildRow);
        scrollPane.getStyleClass().add("editor-overview-scroll");
        scrollPane.content().setPadding(new Insets(24, 32, 24, 32));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, content);

    }

    @Override
    public void onActivate() {
        pageBindings.observe(context.uiState().select(WorkspaceUiState.Snapshot::search), value -> refresh());
        pageBindings.observe(context.selectFilterPath(), value -> refresh());
        pageBindings.observe(context.uiState().select(WorkspaceUiState.Snapshot::sidebarView), value -> refresh());
        context.elementStore().subscribeRegistry(Registries.ENCHANTMENT, storeListener);
        refresh();
    }

    @Override
    public void onDeactivate() {
        pageBindings.dispose();
        context.elementStore().unsubscribeRegistry(Registries.ENCHANTMENT, storeListener);
    }

    private Row buildRow(ElementEntry<Enchantment> entry) {
        Row row = new Row();
        row.getStyleClass().add("editor-overview-row");
        row.setOnClick(() -> open(entry.id()));
        row.setOnAction(() -> open(entry.id()));
        row.setIcon(buildIcon(entry));

        Label name = new Label(entry.data().description().getString());
        name.getStyleClass().add("editor-overview-row-name");

        Label identifier = new Label(entry.id().toString());
        identifier.getStyleClass().add("editor-overview-row-identifier");

        Label bullet = new Label("\u2022");
        bullet.getStyleClass().add("editor-overview-row-separator");

        Label level = new Label(I18n.get("enchantment:overview.level") + " " + entry.data().getMaxLevel());
        level.getStyleClass().add("editor-overview-row-level");

        HBox subInfo = new HBox(8, identifier, bullet, level);
        subInfo.setAlignment(Pos.CENTER_LEFT);

        row.setContent(name, subInfo);

        var sw = row.toggle();
        sw.setValue(!EnchantmentActions.isSoftDeleted(entry));
        sw.valueProperty().addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v))
                return;
            var current = context.elementStore().get(Registries.ENCHANTMENT, entry.id());
            boolean enabled = current == null || !EnchantmentActions.isSoftDeleted(current);
            if (Boolean.valueOf(v).equals(enabled))
                return;
            var result = context.gateway().dispatch(Registries.ENCHANTMENT, entry.id(),
                new EditorAction.ToggleDisabled(),
                optimisticEntry -> optimisticEntry.withCustom(
                    EnchantmentActions.toggleDisabled().apply(optimisticEntry.custom())));
            if (!result.isApplied())
                sw.setValue(enabled);
        });

        return row;
    }

    private StackPane buildIcon(ElementEntry<Enchantment> entry) {
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("editor-overview-row-icon");
        iconWrap.setMinSize(32, 32);
        iconWrap.setPrefSize(32, 32);
        iconWrap.setMaxSize(32, 32);
        var itemId = EnchantmentActions.previewItemId(entry.data(),
            tag -> context.resolveTag(Registries.ITEM, tag));
        if (itemId != null) {
            iconWrap.getChildren().add(new ItemSprite(itemId, 32));
        } else {
            Label fallback = new Label("?");
            fallback.getStyleClass().add("editor-overview-row-placeholder");
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
        var sidebarView = context.uiState().sidebarView();

        return context.allTypedEntries(Registries.ENCHANTMENT).stream()
            .filter(e -> search.isEmpty() || e.id().getPath().contains(search))
            .filter(e -> EnchantmentViewMatchers.matches(e, filterPath, sidebarView))
            .sorted(Comparator.comparing(e -> e.data().description().getString()))
            .toList();
    }

    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/search.svg");

    private VBox emptyState() {
        var icon = new SvgIcon(SEARCH_ICON, 40, Color.WHITE);
        icon.setOpacity(0.2);
        StackPane circle = new StackPane(icon);
        circle.getStyleClass().add("editor-overview-empty-circle");
        circle.setPrefSize(96, 96);
        circle.setMaxSize(96, 96);
        circle.setAlignment(Pos.CENTER);

        Label title = new Label(I18n.get("enchantment:items.no_results.title"));
        title.getStyleClass().add("editor-overview-empty-title");
        Label body = new Label(I18n.get("enchantment:items.no_results.description"));
        body.getStyleClass().add("editor-overview-empty-body");
        body.setWrapText(true);

        VBox box = new VBox(6, circle, title, body);
        VBox.setMargin(circle, new Insets(0, 0, 18, 0));
        box.getStyleClass().add("editor-overview-empty");
        box.setOpacity(0.6);
        return box;
    }

    private void open(Identifier id) {
        context.tabsState().openElement(id.toString(), StudioRoute.ENCHANTMENT_MAIN);
        context.router().navigate(StudioRoute.ENCHANTMENT_MAIN);
    }
}
