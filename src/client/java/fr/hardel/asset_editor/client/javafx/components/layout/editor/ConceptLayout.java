package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeController;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

public final class ConceptLayout extends HBox {

    public record Config(
            StudioConcept concept,
            Identifier icon,
            String sidebarTitleKey,
            TreeController.Config treeConfig,
            StudioRoute simulationRoute,
            boolean showViewModeToggle,
            Function<StudioRoute, Node> pageFactory,
            List<Node> sidebarExtras
    ) {}

    private final StudioContext context;
    private final StudioConcept concept;
    private final TreeController tree;
    private final Function<StudioRoute, Node> pageFactory;
    private final EnumMap<StudioRoute, Node> pageCache = new EnumMap<>(StudioRoute.class);
    private final FxSelectionBindings bindings = new FxSelectionBindings();
    private final StackPane outlet = new StackPane();
    private Page activePage;

    public ConceptLayout(StudioContext context, Config config) {
        this.context = context;
        this.concept = config.concept();
        this.pageFactory = config.pageFactory();

        getStyleClass().add("layout-concept");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        tree = new TreeController(context, config.treeConfig());

        Node sidebar = new EditorSidebar(
                context, tree,
                config.sidebarTitleKey(),
                config.icon(),
                config.sidebarExtras());

        VBox main = new VBox(
                new EditorHeader(context, tree, config.concept(), config.showViewModeToggle(), config.simulationRoute()),
                outlet);
        main.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        main.getStyleClass().add("layout-concept-main");
        VBox.setVgrow(outlet, Priority.ALWAYS);
        HBox.setHgrow(main, Priority.ALWAYS);

        getChildren().addAll(sidebar, main);

        context.router().routeProperty().addListener((obs, old, route) -> {
            if (getScene() == null) return;
            switchPage(route);
        });

        bindings.observe(context.selectCurrentElementId(), id -> {
            if (getScene() == null) return;
            if (activePage != null) activePage.onActivate();
        });

        switchPage(context.router().currentRoute());
    }

    public TreeController tree() { return tree; }

    private void switchPage(StudioRoute route) {
        if (!concept.registry().equals(route.concept())) return;

        if (activePage != null) activePage.onDeactivate();

        if (!ensureDetailSelectionInvariant(route)) {
            setOutlet(concept.overviewRoute());
            return;
        }

        setOutlet(route);
    }

    private void setOutlet(StudioRoute route) {
        if (route == concept.overviewRoute())
            context.uiState().setSearch("");
        Node page = pageCache.computeIfAbsent(route, pageFactory);
        outlet.getChildren().setAll(page);
        activePage = page instanceof Page ep ? ep : null;
        if (activePage != null) activePage.onActivate();
    }

    private boolean ensureDetailSelectionInvariant(StudioRoute route) {
        if (!concept.tabRoutes().contains(route)) return true;
        String id = context.tabsState().currentElementId();
        if (id != null && !id.isBlank()) return true;

        context.tabsState().setCurrentElementId("");
        context.router().navigate(concept.overviewRoute());
        return false;
    }
}
