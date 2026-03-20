package fr.hardel.asset_editor.client.javafx.routes.debug;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.KeyValueGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DebugWorkspacePage extends StackPane implements Page {

    private final StudioContext context;
    private final VBox content = new VBox(24);
    private FxSelectionBindings bindings;
    private boolean attached;

    public DebugWorkspacePage(StudioContext context) {
        this.context = context;
        getStyleClass().add("concept-main-page");

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
        if (attached) return;
        attached = true;
        bindings = new FxSelectionBindings();
        bindings.observe(context.sessionState().select(s -> s), s -> refresh());
        bindings.observe(context.workspaceState().select(s -> s), s -> refresh());
        bindings.observe(context.packState().select(s -> s), s -> refresh());
        bindings.observe(context.tabsState().select(s -> s), s -> refresh());
        bindings.observe(context.uiState().select(s -> s), s -> refresh());
        bindings.observe(context.workspaceState().issueState().select(s -> s), s -> refresh());
        context.elementStore().subscribeAll(this::refresh);
        refresh();
    }

    private void detach() {
        if (!attached) return;
        attached = false;
        if (bindings != null) {
            bindings.dispose();
            bindings = null;
        }
        context.elementStore().unsubscribeAll(this::refresh);
    }

    private void refresh() {
        content.getChildren().clear();

        Map<String, Supplier<Object>> sections = new LinkedHashMap<>();
        sections.put(I18n.get("debug:workspace.section.overview"), this::overviewSnapshot);
        sections.put(I18n.get("debug:workspace.section.session"), () -> context.sessionState().snapshot());
        sections.put(I18n.get("debug:workspace.section.ui"), () -> context.uiState().snapshot());
        sections.put("Tabs", () -> context.tabsState().snapshot());
        sections.put("Pack", () -> context.packState().snapshot());
        sections.put(I18n.get("debug:workspace.section.registries"), this::registriesSnapshot);
        sections.put(I18n.get("debug:workspace.section.issues"), () -> context.workspaceState().issueState().snapshot());

        for (var entry : sections.entrySet()) {
            Object snapshot = entry.getValue().get();
            if (snapshot == null) continue;

            Section section = new Section(entry.getKey());
            if (snapshot.getClass().isRecord()) {
                section.addContent(new KeyValueGrid(snapshot));
            } else {
                Label fallback = new Label(snapshot.toString());
                fallback.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
                fallback.setTextFill(VoxelColors.ZINC_400);
                fallback.setWrapText(true);
                section.addContent(fallback);
            }
            content.getChildren().add(section);
        }
    }

    private record OverviewSnapshot(
        String route,
        String selectedPack,
        String currentElement,
        int pendingActions,
        int openTabs,
        int registries,
        int totalEntries
    ) {}

    private Object overviewSnapshot() {
        var counts = context.elementStore().entryCountsSnapshot();
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        var pack = context.packState().selectedPack();
        return new OverviewSnapshot(
            context.router().currentRoute().name(),
            pack == null ? "none" : pack.packId(),
            context.tabsState().currentElementId() == null ? "none" : context.tabsState().currentElementId(),
            context.workspaceState().snapshot().pendingActionCount(),
            context.tabsState().openTabsView().size(),
            counts.size(),
            total
        );
    }

    private record RegistriesSnapshot(Map<String, Integer> counts) {}

    private Object registriesSnapshot() {
        return new RegistriesSnapshot(context.elementStore().entryCountsSnapshot());
    }
}
