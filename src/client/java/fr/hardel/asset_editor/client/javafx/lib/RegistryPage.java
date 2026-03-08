package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreSelector;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class RegistryPage<T> extends VBox implements Page {

    private final StudioContext context;
    private final ResourceKey<Registry<T>> registry;
    private final VBox content;
    private final List<StoreSelector<?>> selectors = new ArrayList<>();
    private Identifier currentId;
    private boolean built;

    protected RegistryPage(StudioContext context, ResourceKey<Registry<T>> registry,
                           String scrollClass, double spacing, Insets padding) {
        this.context = context;
        this.registry = registry;
        this.content = new VBox(spacing);
        content.setPadding(padding);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add(scrollClass);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    protected StudioContext context() { return context; }
    protected VBox content() { return content; }
    protected Identifier currentId() { return currentId; }
    protected ResourceKey<Registry<T>> registry() { return registry; }

    protected <R> StoreSelector<R> select(Function<ElementEntry<T>, R> extractor) {
        var selector = context.elementStore().select(registry, currentId, extractor);
        selectors.add(selector);
        return selector;
    }

    protected abstract void buildContent();

    protected void onReady() {}

    @Override
    public final void onActivate() {
        var holder = context.findElement(registry);
        Identifier newId = holder != null ? holder.key().identifier() : null;
        if (newId == null) { onNoElement(); return; }
        if (!newId.equals(currentId)) {
            disposeSelectors();
            currentId = newId;
            built = false;
        }
        if (!built) {
            buildContent();
            built = true;
        }
        onReady();
    }

    @Override
    public final void onDeactivate() {
        disposeSelectors();
    }

    protected void onNoElement() {
        content.getChildren().clear();
        built = false;
    }

    private void disposeSelectors() {
        if (!selectors.isEmpty()) {
            context.elementStore().disposeSelectors(selectors);
            selectors.clear();
        }
        built = false;
    }
}
