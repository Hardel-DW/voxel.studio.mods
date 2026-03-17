package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.components.page.changes.ChangesLayout;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentLayout;
import fr.hardel.asset_editor.client.javafx.components.page.loot_table.LootTableLayout;
import fr.hardel.asset_editor.client.javafx.components.page.recipe.RecipeLayout;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.debug.DebugItemsPage;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class StudioEditorRoot extends HBox {

    private static final Map<StudioConcept, Function<StudioContext, ConceptLayout>> LAYOUT_FACTORIES = Map.of(
            StudioConcept.ENCHANTMENT, EnchantmentLayout::create,
            StudioConcept.LOOT_TABLE, LootTableLayout::create,
            StudioConcept.RECIPE, RecipeLayout::create
    );

    private final StudioContext context = new StudioContext();
    private final StackPane contentOutlet = new StackPane();
    private final EnumMap<StudioConcept, ConceptLayout> layouts = new EnumMap<>(StudioConcept.class);
    private ChangesLayout changesLayout;
    private DebugItemsPage debugItemsPage;

    public StudioContext context() {
        return context;
    }

    public StudioEditorRoot(Stage stage) {
        getStyleClass().add("studio-root");
        context.packState().refreshFromServer();

        StudioPrimarySidebar sidebar = new StudioPrimarySidebar(context);
        StudioEditorTabsBar header = new StudioEditorTabsBar(context, stage);

        StackPane contentBody = new StackPane(contentOutlet);
        contentBody.getStyleClass().add("studio-content-body");
        contentBody.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentBody.setAlignment(Pos.TOP_LEFT);
        contentOutlet.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Path frame = new Path();
        frame.getStyleClass().add("studio-content-frame");
        frame.setMouseTransparent(true);
        frame.setManaged(false);
        frame.setFill(Color.TRANSPARENT);
        frame.setStrokeType(StrokeType.CENTERED);
        frame.setStrokeLineCap(StrokeLineCap.BUTT);

        StackPane contentSurface = new StackPane(contentBody, frame);
        contentSurface.getStyleClass().add("studio-content-surface");
        contentSurface.setAlignment(Pos.TOP_LEFT);
        StackPane.setAlignment(contentBody, Pos.TOP_LEFT);
        StackPane.setAlignment(contentOutlet, Pos.TOP_LEFT);
        VBox.setVgrow(contentSurface, Priority.ALWAYS);

        contentSurface.widthProperty().addListener((obs, oldW, newW) ->
                refreshSurfaceGeometry(newW.doubleValue(), contentSurface.getHeight(), contentBody, frame));
        contentSurface.heightProperty().addListener((obs, oldH, newH) ->
                refreshSurfaceGeometry(contentSurface.getWidth(), newH.doubleValue(), contentBody, frame));

        VBox workspace = new VBox(header, contentSurface);
        workspace.getStyleClass().add("studio-workspace");
        HBox.setHgrow(workspace, Priority.ALWAYS);

        getChildren().addAll(sidebar, workspace);
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refreshOutlet());
        StudioConcept.firstAccessible(context.permissions())
                .ifPresent(concept -> context.router().navigate(concept.overviewRoute()));
        refreshOutlet();

        contentSurface.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            refreshSurfaceGeometry(contentSurface.getWidth(), contentSurface.getHeight(), contentBody, frame);
        });
    }

    private NoPermissionPage noPermissionPage;

    private void refreshOutlet() {
        StudioRoute route = context.router().currentRoute();
        if (route == StudioRoute.NO_PERMISSION) {
            if (noPermissionPage == null) noPermissionPage = new NoPermissionPage();
            contentOutlet.getChildren().setAll(noPermissionPage);
            return;
        }
        if (route == StudioRoute.CHANGES_MAIN) {
            if (changesLayout == null) changesLayout = new ChangesLayout();
            contentOutlet.getChildren().setAll(changesLayout);
            return;
        }
        if (route == StudioRoute.DEBUG_ITEMS) {
            if (debugItemsPage == null) debugItemsPage = new DebugItemsPage();
            contentOutlet.getChildren().setAll(debugItemsPage);
            return;
        }
        StudioConcept concept = StudioConcept.byRoute(route);
        var factory = LAYOUT_FACTORIES.get(concept);
        if (factory == null) return;
        contentOutlet.getChildren().setAll(layouts.computeIfAbsent(concept, c -> factory.apply(context)));
    }

    private static void refreshSurfaceGeometry(double width, double height, StackPane contentBody, Path frame) {
        if (width <= 1 || height <= 1) {
            contentBody.setClip(null);
            frame.getElements().clear();
            return;
        }
        contentBody.setClip(buildTlClip(width, height));
        frame.getElements().setAll(buildTlFrame(width, height).getElements());
    }

    private static Path buildTlClip(double width, double height) {
        if (width <= 0 || height <= 0) return new Path();
        double r = Math.min(24, Math.min(width, height));
        Path clip = new Path(
                new MoveTo(r, 0), new LineTo(width, 0), new LineTo(width, height),
                new LineTo(0, height), new LineTo(0, r), new QuadCurveTo(0, 0, r, 0), new ClosePath());
        clip.setFill(Color.WHITE);
        clip.setStroke(null);
        return clip;
    }

    private static Path buildTlFrame(double width, double height) {
        if (width <= 0 || height <= 0) return new Path();
        double inset = 0.5;
        double w = Math.max(inset, width - inset);
        double h = Math.max(inset, height - inset);
        double r = Math.max(0, Math.min(24, Math.min(w, h)) - inset);
        return new Path(
                new MoveTo(inset + r, inset), new QuadCurveTo(inset, inset, inset, inset + r),
                new LineTo(inset, h), new MoveTo(inset + r, inset), new LineTo(w, inset));
    }
}
