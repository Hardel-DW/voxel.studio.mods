package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.Dialog;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.PackCreateDialog;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionResult;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.store.ElementEntry;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class RegistryPage<T> extends VBox implements Page {

    private final StudioContext context;
    private final ResourceKey<Registry<T>> registry;
    private final VBox content;
    private final FxSelectionBindings bindings = new FxSelectionBindings();
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

    protected <R> StoreSelection<ElementEntry<?>, R> selectValue(Function<ElementEntry<T>, R> extractor) {
        return context.elementStore().selectValue(registry, currentId, extractor);
    }

    protected FxSelectionBindings selectionBindings() {
        return bindings;
    }

    protected EditorActionResult applyAction(EditorAction action) {
        if (action == null)
            return EditorActionResult.error("error:invalid_action");
        var result = context.gateway().dispatch(registry, currentId, action);
        if (!result.isApplied())
            handleActionFailure(result);
        return result;
    }

    protected EditorActionResult applyTagToggle(Identifier tagId) {
        return applyAction(new EditorAction.ToggleTag(tagId));
    }

    protected void bindInt(IntegerProperty property,
        Function<ElementEntry<T>, Integer> extractor,
        java.util.function.IntFunction<EditorAction> actionFactory) {
        var selection = selectValue(extractor);
        bindings.observe(selection, value -> {
            if (value != null && !Integer.valueOf(value).equals(property.getValue()))
                property.set(value);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.intValue() == v.intValue())
                return;
            if (Integer.valueOf(v.intValue()).equals(selection.get()))
                return;
            var result = applyAction(actionFactory.apply(v.intValue()));
            if (!result.isApplied() && selection.get() != null)
                property.set(selection.get());
        });
    }

    protected void bindToggle(BooleanProperty property,
        Function<ElementEntry<T>, Boolean> extractor,
        Supplier<EditorActionResult> action) {
        var selection = selectValue(extractor);
        bindings.observe(selection, value -> {
            if (value != null && !value.equals(property.getValue()))
                property.set(value);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v))
                return;
            if (v.equals(selection.get()))
                return;
            var result = action.get();
            if (!result.isApplied() && selection.get() != null)
                property.set(selection.get());
        });
    }

    protected void bindString(StringProperty property,
        Function<ElementEntry<T>, String> extractor,
        Function<String, EditorAction> actionFactory) {
        var selection = selectValue(extractor);
        bindings.observe(selection, value -> {
            if (value != null && !value.equals(property.getValue()))
                property.set(value);
        });
        property.addListener((obs, o, v) -> {
            if (o == null || v == null || o.equals(v))
                return;
            if (v.equals(selection.get()))
                return;
            var result = applyAction(actionFactory.apply(v));
            if (!result.isApplied() && selection.get() != null)
                property.set(selection.get());
        });
    }

    protected <R> StoreSelection<ElementEntry<?>, R> bindView(Function<ElementEntry<T>, R> extractor, Consumer<R> setter) {
        var selection = selectValue(extractor);
        bindings.observe(selection, value -> {
            if (value != null)
                setter.accept(value);
        });
        return selection;
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
        bindings.dispose();
        built = false;
    }

    private void handleActionFailure(EditorActionResult result) {
        if (result.status() == EditorActionResult.Status.PACK_REQUIRED) {
            showPackRequiredDialog();
            return;
        }
        if (result.status() == EditorActionResult.Status.REJECTED) {
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
