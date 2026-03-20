package fr.hardel.asset_editor.client.javafx.routes.debug;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.DataTable;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.SelectableTextBlock;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.state.ClientPackInfo;
import fr.hardel.asset_editor.client.state.PendingClientAction;
import fr.hardel.asset_editor.client.state.StudioOpenTab;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.Map;

public final class DebugWorkspacePage extends StackPane implements Page {

    private record Metric(String title, String value) {}
    private record RegistryCount(String registry, int count) {}

    private final StudioContext context;
    private final FlowPane overviewCards = new FlowPane(12, 12);
    private final FlowPane sessionCards = new FlowPane(12, 12);
    private final FlowPane uiCards = new FlowPane(12, 12);
    private final DataTable<StudioOpenTab> tabsTable = new DataTable<>();
    private final DataTable<PendingClientAction<?>> pendingTable = new DataTable<>();
    private final DataTable<RegistryCount> registryTable = new DataTable<>();
    private final SelectableTextBlock warningsBlock =
        new SelectableTextBlock("", VoxelFonts.Variant.REGULAR, 12, VoxelColors.ZINC_300);
    private final SelectableTextBlock errorsBlock =
        new SelectableTextBlock("", VoxelFonts.Variant.REGULAR, 12, VoxelColors.ZINC_300);
    private final SelectableTextBlock snapshotBlock =
        new SelectableTextBlock("", VoxelFonts.Variant.REGULAR, 12, VoxelColors.ZINC_300);
    private final ChangeListener<Object> routeListener = (obs, oldValue, newValue) -> refresh();
    private final Runnable registryRefreshListener = this::refresh;
    private FxSelectionBindings bindings;
    private boolean attached;

    public DebugWorkspacePage(StudioContext context) {
        this.context = context;
        getStyleClass().add("concept-main-page");

        overviewCards.setMaxWidth(Double.MAX_VALUE);
        sessionCards.setMaxWidth(Double.MAX_VALUE);
        uiCards.setMaxWidth(Double.MAX_VALUE);

        configureTabsTable();
        configurePendingTable();
        configureRegistryTable();
        configureTextBlocks();

        Section overviewSection = new Section(I18n.get("debug:workspace.section.overview"));
        overviewSection.addContent(overviewCards);

        Section sessionSection = new Section(I18n.get("debug:workspace.section.session"));
        sessionSection.addContent(sessionCards);

        Section uiSection = new Section(I18n.get("debug:workspace.section.ui"));
        uiSection.addContent(uiCards);

        Section tabsSection = new Section(I18n.get("debug:workspace.section.tabs"));
        tabsSection.addContent(tabsTable);

        Section pendingSection = new Section(I18n.get("debug:workspace.section.pending"));
        pendingSection.addContent(pendingTable);

        Section registriesSection = new Section(I18n.get("debug:workspace.section.registries"));
        registriesSection.addContent(registryTable);

        Section issuesSection = new Section(I18n.get("debug:workspace.section.issues"));
        issuesSection.addContent(
            labeledBlock(I18n.get("debug:workspace.issues.warnings"), warningsBlock),
            labeledBlock(I18n.get("debug:workspace.issues.errors"), errorsBlock));

        Section rawSection = new Section(I18n.get("debug:workspace.section.raw"));
        rawSection.addContent(snapshotBlock);

        VBox content = new VBox(24,
            overviewSection,
            sessionSection,
            uiSection,
            tabsSection,
            pendingSection,
            registriesSection,
            issuesSection,
            rawSection);
        content.setPadding(new Insets(24, 32, 32, 32));
        content.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("debug-subpage-scroll");

        getChildren().add(scroll);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                detach();
                return;
            }
            attach();
        });

        refresh();
    }

    private void attach() {
        if (attached)
            return;
        attached = true;
        bindings = new FxSelectionBindings();
        bindings.observe(context.sessionState().select(snapshot -> snapshot), snapshot -> refresh());
        bindings.observe(context.workspaceState().select(snapshot -> snapshot), snapshot -> refresh());
        bindings.observe(context.packState().select(snapshot -> snapshot), snapshot -> refresh());
        bindings.observe(context.tabsState().select(snapshot -> snapshot), snapshot -> refresh());
        bindings.observe(context.uiState().select(snapshot -> snapshot), snapshot -> refresh());
        bindings.observe(context.workspaceState().issueState().select(snapshot -> snapshot), snapshot -> refresh());
        context.router().routeProperty().addListener(routeListener);
        context.elementStore().subscribeAll(registryRefreshListener);
        refresh();
    }

    private void detach() {
        if (!attached)
            return;
        attached = false;
        if (bindings != null) {
            bindings.dispose();
            bindings = null;
        }
        context.router().routeProperty().removeListener(routeListener);
        context.elementStore().unsubscribeAll(registryRefreshListener);
    }

    private void refresh() {
        refreshOverviewCards();
        refreshSessionCards();
        refreshUiCards();
        refreshTables();
        refreshIssues();
        refreshRawSnapshot();
    }

    private void refreshOverviewCards() {
        Map<String, Integer> counts = context.elementStore().entryCountsSnapshot();
        int totalEntries = counts.values().stream().mapToInt(Integer::intValue).sum();
        ClientPackInfo selectedPack = context.packState().selectedPack();
        overviewCards.getChildren().setAll(buildMetricCards(List.of(
            new Metric(I18n.get("debug:workspace.metric.route"), context.router().currentRoute().name()),
            new Metric(I18n.get("debug:workspace.metric.pack"), selectedPack == null ? I18n.get("debug:workspace.none") : selectedPack.packId()),
            new Metric(I18n.get("debug:workspace.metric.element"), blankFallback(context.tabsState().currentElementId())),
            new Metric(I18n.get("debug:workspace.metric.pending"), Integer.toString(context.workspaceState().snapshot().pendingActionCount())),
            new Metric(I18n.get("debug:workspace.metric.tabs"), Integer.toString(context.tabsState().openTabsView().size())),
            new Metric(I18n.get("debug:workspace.metric.registries"), Integer.toString(counts.size())),
            new Metric(I18n.get("debug:workspace.metric.entries"), Integer.toString(totalEntries)))));
    }

    private void refreshSessionCards() {
        var session = context.sessionState().snapshot();
        ClientPackInfo selectedPack = context.packState().selectedPack();
        sessionCards.getChildren().setAll(buildMetricCards(List.of(
            new Metric(I18n.get("debug:workspace.session.world_session_key"), blankFallback(session.worldSessionKey())),
            new Metric(I18n.get("debug:workspace.session.workspace_world_session_key"), blankFallback(context.workspaceState().worldSessionKey())),
            new Metric(I18n.get("debug:workspace.session.permissions_role"), session.permissions().role().name()),
            new Metric(I18n.get("debug:workspace.session.can_edit"), bool(session.permissions().canEdit())),
            new Metric(I18n.get("debug:workspace.session.permissions_received"), bool(session.permissionsReceived())),
            new Metric(I18n.get("debug:workspace.session.pack_list_received"), bool(session.packListReceived())),
            new Metric(I18n.get("debug:workspace.session.available_packs"), Integer.toString(session.availablePacks().size())),
            new Metric(I18n.get("debug:workspace.session.pack_name"), selectedPack == null ? I18n.get("debug:workspace.none") : selectedPack.name()),
            new Metric(I18n.get("debug:workspace.session.pack_writable"), selectedPack == null ? I18n.get("debug:workspace.none") : bool(selectedPack.writable())))));
    }

    private void refreshUiCards() {
        var ui = context.uiState().snapshot();
        var tabs = context.tabsState().snapshot();
        uiCards.getChildren().setAll(buildMetricCards(List.of(
            new Metric(I18n.get("debug:workspace.ui.search"), blankFallback(ui.search())),
            new Metric(I18n.get("debug:workspace.ui.filter_path"), blankFallback(ui.filterPath())),
            new Metric(I18n.get("debug:workspace.ui.view_mode"), ui.viewMode().name()),
            new Metric(I18n.get("debug:workspace.ui.sidebar_view"), ui.sidebarView().name()),
            new Metric(I18n.get("debug:workspace.ui.active_tab_index"), Integer.toString(tabs.activeTabIndex())),
            new Metric(I18n.get("debug:workspace.ui.current_element_id"), blankFallback(tabs.currentElementId())))));
    }

    private void refreshTables() {
        tabsTable.setItems(context.tabsState().openTabsView());
        pendingTable.setItems(context.workspaceState().pendingActionsSnapshot());
        registryTable.setItems(context.elementStore().entryCountsSnapshot().entrySet().stream()
            .map(entry -> new RegistryCount(entry.getKey(), entry.getValue()))
            .toList());
    }

    private void refreshIssues() {
        var issues = context.workspaceState().issueState().snapshot();
        warningsBlock.setText(linesOrFallback(issues.warnings()));
        errorsBlock.setText(linesOrFallback(issues.errors()));
    }

    private void refreshRawSnapshot() {
        snapshotBlock.setText("""
            session {
              worldSessionKey: %s
              permissionsRole: %s
              permissionsReceived: %s
              packListReceived: %s
              availablePacks: %s
            }
            workspace {
              worldSessionKey: %s
              route: %s
              selectedPack: %s
              currentElementId: %s
              pendingActionCount: %s
            }
            ui {
              search: %s
              filterPath: %s
              viewMode: %s
              sidebarView: %s
            }
            tabs {
              activeTabIndex: %s
              openTabs: %s
            }
            registries {
              counts: %s
            }
            issues {
              warnings: %s
              errors: %s
            }
            """.formatted(
            context.sessionState().worldSessionKey(),
            context.sessionState().permissions().role().name(),
            context.sessionState().hasReceivedPermissions(),
            context.sessionState().hasReceivedPackList(),
            context.sessionState().snapshot().availablePacks().stream().map(ClientPackInfo::packId).toList(),
            context.workspaceState().worldSessionKey(),
            context.router().currentRoute().name(),
            context.packState().selectedPack() == null ? "" : context.packState().selectedPack().packId(),
            context.tabsState().currentElementId(),
            context.workspaceState().snapshot().pendingActionCount(),
            context.uiState().search(),
            context.uiState().filterPath(),
            context.uiState().viewMode().name(),
            context.uiState().sidebarView().name(),
            context.tabsState().activeTabIndex(),
            context.tabsState().openTabsView().stream().map(tab -> tab.route().name() + ":" + tab.elementId()).toList(),
            context.elementStore().entryCountsSnapshot(),
            context.workspaceState().issueState().snapshot().warnings(),
            context.workspaceState().issueState().snapshot().errors()));
    }

    private void configureTabsTable() {
        tabsTable.addColumn(I18n.get("debug:workspace.tabs.index"), 60, tab -> text(Integer.toString(context.tabsState().openTabsView().indexOf(tab)), VoxelColors.ZINC_500, 12));
        tabsTable.addColumn(I18n.get("debug:workspace.tabs.route"), 180, tab -> text(tab.route().name(), VoxelColors.ZINC_300, 12));
        tabsTable.addColumn(I18n.get("debug:workspace.tabs.element"), -1, tab -> text(tab.elementId(), VoxelColors.ZINC_300, 12));
        tabsTable.setPlaceholder(I18n.get("debug:workspace.tabs.empty"));
    }

    private void configurePendingTable() {
        pendingTable.addColumn(I18n.get("debug:workspace.pending.action_id"), 240, action -> text(action.actionId().toString(), VoxelColors.ZINC_400, 12));
        pendingTable.addColumn(I18n.get("debug:workspace.pending.pack"), 180, action -> text(action.packId(), VoxelColors.ZINC_300, 12));
        pendingTable.addColumn(I18n.get("debug:workspace.pending.registry"), 180, action -> text(action.registry().identifier().toString(), VoxelColors.ZINC_300, 12));
        pendingTable.addColumn(I18n.get("debug:workspace.pending.target"), -1, action -> text(action.target().toString(), VoxelColors.ZINC_300, 12));
        pendingTable.setPlaceholder(I18n.get("debug:workspace.pending.empty"));
    }

    private void configureRegistryTable() {
        registryTable.addColumn(I18n.get("debug:workspace.registries.registry"), -1, entry -> text(entry.registry(), VoxelColors.ZINC_300, 12));
        registryTable.addColumn(I18n.get("debug:workspace.registries.count"), 100, entry -> text(Integer.toString(entry.count()), VoxelColors.ZINC_500, 12));
        registryTable.setPlaceholder(I18n.get("debug:workspace.registries.empty"));
    }

    private void configureTextBlocks() {
        VBox.setVgrow(tabsTable, Priority.NEVER);
        VBox.setVgrow(pendingTable, Priority.NEVER);
        VBox.setVgrow(registryTable, Priority.NEVER);
    }

    private static Node labeledBlock(String title, Node content) {
        Label label = new Label(title);
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        label.setTextFill(VoxelColors.ZINC_500);
        VBox box = new VBox(6, label, content);
        box.setFillWidth(true);
        return box;
    }

    private static List<Node> buildMetricCards(List<Metric> metrics) {
        return metrics.stream().map(DebugWorkspacePage::metricCard).toList();
    }

    private static Node metricCard(Metric metric) {
        Label title = new Label(metric.title());
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        title.setTextFill(VoxelColors.ZINC_500);

        Label value = new Label(metric.value());
        value.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 14));
        value.setTextFill(VoxelColors.ZINC_100);
        value.setWrapText(true);

        VBox content = new VBox(6, title, value);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillWidth(true);

        StackPane card = new StackPane(content);
        card.getStyleClass().add("ui-simple-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(220);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        return card;
    }

    private static Label text(String value, javafx.scene.paint.Color color, double size) {
        Label label = new Label(blankFallback(value));
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, size));
        label.setTextFill(color);
        label.setWrapText(true);
        return label;
    }

    private static String linesOrFallback(List<String> values) {
        if (values == null || values.isEmpty())
            return I18n.get("debug:workspace.none");
        return String.join(System.lineSeparator(), values);
    }

    private static String bool(boolean value) {
        return value ? I18n.get("debug:workspace.bool.true") : I18n.get("debug:workspace.bool.false");
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? I18n.get("debug:workspace.none") : value;
    }
}
