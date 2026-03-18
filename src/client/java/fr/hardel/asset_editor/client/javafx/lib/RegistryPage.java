package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.Dialog;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.PackCreateDialog;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionResult;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionStatus;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StoreSelector;
import fr.hardel.asset_editor.network.EditorAction;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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

    protected StudioContext context() {
        return context;
    }

    protected VBox content() {
        return content;
    }

    protected Identifier currentId() {
        return currentId;
    }

    protected ResourceKey<Registry<T>> registry() {
        return registry;
    }

    protected <R> StoreSelector<R> select(Function<ElementEntry<T>, R> extractor) {
        var selector = context.elementStore().select(registry, currentId, extractor);
        selectors.add(selector);
        return selector;
    }

    protected EditorActionResult applyAction(UnaryOperator<T> transform) {
        return applyAction(transform, null);
    }

    protected EditorActionResult applyAction(UnaryOperator<T> transform, EditorAction action) {
        var result = context.gateway().apply(registry, currentId, transform, action);
        if (!result.isApplied())
            handleActionFailure(result);
        return result;
    }

    protected EditorActionResult applyCustomAction(UnaryOperator<CustomFields> transform, EditorAction action) {
        var result = context.gateway().applyCustom(registry, currentId, transform, action);
        if (!result.isApplied())
            handleActionFailure(result);
        return result;
    }

    protected EditorActionResult applyTagToggle(Identifier tagId) {
        var result = context.gateway().toggleTag(registry, currentId, tagId);
        if (!result.isApplied())
            handleActionFailure(result);
        return result;
    }

    protected void bindInt(IntegerProperty property,
        Function<ElementEntry<T>, Integer> extractor,
        java.util.function.IntFunction<UnaryOperator<T>> mutation,
        java.util.function.IntFunction<EditorAction> actionFactory) {
        var selector = select(extractor);
        if (selector.get() != null)
            property.set(selector.get());
        selector.subscribe(val -> {
            if (val != null)
                property.set(val);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.intValue() == v.intValue())
                return;
            if (Integer.valueOf(v.intValue()).equals(selector.get()))
                return;
            var result = applyAction(mutation.apply(v.intValue()), actionFactory.apply(v.intValue()));
            if (!result.isApplied() && selector.get() != null)
                property.set(selector.get());
        });
    }

    protected void bindToggle(BooleanProperty property,
        Function<ElementEntry<T>, Boolean> extractor,
        Supplier<EditorActionResult> action) {
        var selector = select(extractor);
        if (selector.get() != null)
            property.set(selector.get());
        selector.subscribe(val -> {
            if (val != null)
                property.set(val);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v))
                return;
            if (v.equals(selector.get()))
                return;
            var result = action.get();
            if (!result.isApplied() && selector.get() != null)
                property.set(selector.get());
        });
    }

    protected void bindString(StringProperty property,
        Function<ElementEntry<T>, String> extractor,
        Function<String, EditorActionResult> action) {
        var selector = select(extractor);
        if (selector.get() != null)
            property.set(selector.get());
        selector.subscribe(val -> {
            if (val != null)
                property.set(val);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v))
                return;
            if (v.equals(selector.get()))
                return;
            var result = action.apply(v);
            if (!result.isApplied() && selector.get() != null)
                property.set(selector.get());
        });
    }

    protected <R> StoreSelector<R> bindView(Function<ElementEntry<T>, R> extractor, Consumer<R> setter) {
        var selector = select(extractor);
        if (selector.get() != null)
            setter.accept(selector.get());
        selector.subscribe(val -> {
            if (val != null)
                setter.accept(val);
        });
        return selector;
    }

    protected boolean hasWritablePack() {
        var pack = context.packState().selectedPack();
        return pack != null && pack.writable();
    }

    protected void showPackGuard() {
        if (!context.packState().hasSelectedPack())
            showPackRequiredDialog();
        else
            showErrorDialog("error:pack_readonly");
    }

    protected abstract void buildContent();

    protected void onReady() {}

    @Override
    public final void onActivate() {
        var entry = context.currentEntry(registry);
        Identifier newId = entry != null ? entry.id() : null;
        if (newId == null) {
            onNoElement();
            return;
        }
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

    private void handleActionFailure(EditorActionResult result) {
        if (result.status() == EditorActionStatus.PACK_REQUIRED) {
            showPackRequiredDialog();
            return;
        }
        if (result.status() == EditorActionStatus.REJECTED) {
            showErrorDialog(result.message());
            return;
        }
        showErrorDialog(result.message());
    }

    private void showPackRequiredDialog() {
        Label message = new Label(I18n.get("studio:pack.required.message"));
        message.setTextFill(VoxelColors.ZINC_400);
        message.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        message.setWrapText(true);

        Dialog dialog = new Dialog(I18n.get("studio:pack.required.title"), message);
        Button cancelBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("studio:action.cancel"));
        cancelBtn.setOnAction(dialog::close);

        Button createBtn = new Button(Button.Variant.SHIMMER, Button.Size.SM, I18n.get("studio:pack.create"));
        createBtn.setOnAction(() -> {
            dialog.close();
            PackCreateDialog.create(context).show(getScene().getWindow());
        });

        dialog.addFooterButton(cancelBtn).addFooterButton(createBtn);
        dialog.show(getScene().getWindow());
    }

    private void showErrorDialog(String messageKey) {
        Label message = new Label(I18n.get(messageKey == null ? "error:unknown" : messageKey));
        message.setTextFill(VoxelColors.ZINC_400);
        message.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        message.setWrapText(true);

        Dialog dialog = new Dialog(I18n.get("error:dialog.title"), message);
        Button closeBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("studio:action.cancel"));
        closeBtn.setOnAction(dialog::close);
        dialog.addFooterButton(closeBtn);
        dialog.show(getScene().getWindow());
    }
}
