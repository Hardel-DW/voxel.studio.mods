package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.javafx.components.page.changes.ChangesLayout;
import fr.hardel.asset_editor.client.javafx.components.page.enchantment.EnchantmentLayout;
import fr.hardel.asset_editor.client.javafx.components.page.loot_table.LootTableLayout;
import fr.hardel.asset_editor.client.javafx.components.page.recipe.RecipeLayout;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.debug.DebugLayout;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class StudioEditorRoot extends HBox {

    private static final Map<StudioConcept, Function<StudioContext, ConceptLayout>> LAYOUT_FACTORIES = Map.of(
        StudioConcept.ENCHANTMENT, EnchantmentLayout::create,
        StudioConcept.LOOT_TABLE, LootTableLayout::create,
        StudioConcept.RECIPE, RecipeLayout::create);

    private final StudioContext context;
    private final StackPane contentOutlet = new StackPane();
    private final EnumMap<StudioConcept, ConceptLayout> layouts = new EnumMap<>(StudioConcept.class);
    private ChangesLayout changesLayout;
    private DebugLayout debugLayout;
    private NoPermissionPage noPermissionPage;

    public StudioEditorRoot(Stage stage, ClientSessionState sessionState, ClientSessionDispatch dispatch) {
        this.context = new StudioContext(sessionState, dispatch);
        getStyleClass().add("studio-root");

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

        contentSurface.widthProperty().addListener((obs, oldWidth, newWidth) -> refreshSurfaceGeometry(newWidth.doubleValue(), contentSurface.getHeight(), contentBody, frame));
        contentSurface.heightProperty().addListener((obs, oldHeight, newHeight) -> refreshSurfaceGeometry(contentSurface.getWidth(), newHeight.doubleValue(), contentBody, frame));

        VBox workspace = new VBox(header, contentSurface);
        workspace.getStyleClass().add("studio-workspace");
        HBox.setHgrow(workspace, Priority.ALWAYS);

        getChildren().addAll(sidebar, workspace);
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refreshOutlet());
        StudioConcept.firstAccessible(context.permissions())
            .ifPresent(concept -> context.router().navigate(concept.overviewRoute()));
        refreshOutlet();

        contentSurface.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null)
                return;
            refreshSurfaceGeometry(contentSurface.getWidth(), contentSurface.getHeight(), contentBody, frame);
        });
    }

    public StudioContext context() {
        return context;
    }

    public void dispose() {
        context.dispose();
    }

    private void refreshOutlet() {
        StudioRoute route = context.router().currentRoute();
        if (route == StudioRoute.NO_PERMISSION) {
            if (noPermissionPage == null)
                noPermissionPage = new NoPermissionPage();
            contentOutlet.getChildren().setAll(noPermissionPage);
            return;
        }
        if (route == StudioRoute.CHANGES_MAIN) {
            if (changesLayout == null)
                changesLayout = new ChangesLayout();
            contentOutlet.getChildren().setAll(changesLayout);
            return;
        }
        if (route == StudioRoute.DEBUG) {
            if (debugLayout == null)
                debugLayout = new DebugLayout(context);
            contentOutlet.getChildren().setAll(debugLayout);
            return;
        }
        StudioConcept concept = StudioConcept.byRoute(route);
        Function<StudioContext, ConceptLayout> factory = LAYOUT_FACTORIES.get(concept);
        if (factory == null)
            return;
        contentOutlet.getChildren().setAll(layouts.computeIfAbsent(concept, current -> factory.apply(context)));
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
        if (width <= 0 || height <= 0)
            return new Path();
        double radius = Math.min(24, Math.min(width, height));
        Path clip = new Path(
            new MoveTo(radius, 0),
            new LineTo(width, 0),
            new LineTo(width, height),
            new LineTo(0, height),
            new LineTo(0, radius),
            new QuadCurveTo(0, 0, radius, 0),
            new ClosePath());
        clip.setFill(Color.WHITE);
        clip.setStroke(null);
        return clip;
    }

    private static Path buildTlFrame(double width, double height) {
        if (width <= 0 || height <= 0)
            return new Path();
        double inset = 0.5;
        double safeWidth = Math.max(inset, width - inset);
        double safeHeight = Math.max(inset, height - inset);
        double radius = Math.max(0, Math.min(24, Math.min(safeWidth, safeHeight)) - inset);
        return new Path(
            new MoveTo(inset + radius, inset),
            new QuadCurveTo(inset, inset, inset, inset + radius),
            new LineTo(inset, safeHeight),
            new MoveTo(inset + radius, inset),
            new LineTo(safeWidth, inset));
    }
}
