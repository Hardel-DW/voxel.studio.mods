package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Dialog;
import fr.hardel.asset_editor.client.javafx.components.ui.FileInput;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.InputText;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

public final class PackCreateDialog {

    private PackCreateDialog() {}

    public static Dialog create(StudioContext context) {
        InputText nameInput = new InputText(I18n.get("studio:pack.create.name.placeholder"));
        InputText namespaceInput = new InputText(I18n.get("studio:pack.create.namespace.placeholder"));
        FileInput iconInput = new FileInput(I18n.get("studio:pack.create.icon.placeholder"), "*.png", file -> {});

        boolean[] syncing = {false};
        boolean[] userEditedNamespace = {false};
        namespaceInput.field().textProperty().addListener((obs, o, v) -> {
            if (!syncing[0]) userEditedNamespace[0] = true;
        });
        nameInput.field().textProperty().addListener((obs, o, v) -> {
            if (v != null && !userEditedNamespace[0]) {
                syncing[0] = true;
                namespaceInput.setText(v.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
                syncing[0] = false;
            }
        });

        Label nameLabel = fieldLabel("studio:pack.create.name");
        Label nsLabel = fieldLabel("studio:pack.create.namespace");
        Label iconLabel = fieldLabel("studio:pack.create.icon");
        Label errorLabel = new Label();
        errorLabel.setTextFill(VoxelColors.RED_400);
        errorLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        nameInput.field().textProperty().addListener((obs, o, v) -> clearError(errorLabel));
        namespaceInput.field().textProperty().addListener((obs, o, v) -> clearError(errorLabel));

        VBox form = new VBox(12,
                nameLabel, nameInput,
                nsLabel, namespaceInput,
                iconLabel, iconInput,
                errorLabel
        );
        form.setPadding(new Insets(8, 0, 0, 0));

        Dialog dialog = new Dialog(I18n.get("studio:pack.create.title"), form);

        Button cancelBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM,
                I18n.get("studio:action.cancel"));
        cancelBtn.setOnAction(dialog::close);

        Button createBtn = new Button(Button.Variant.SHIMMER, Button.Size.SM,
                I18n.get("studio:action.create"));
        createBtn.setOnAction(() -> {
            String name = nameInput.getText().trim();
            String namespace = namespaceInput.getText().trim();
            if (name.isEmpty() || namespace.isEmpty()) {
                setError(errorLabel, "error:pack_name_and_namespace_required");
                return;
            }
            if (!Identifier.isValidNamespace(namespace)) {
                setError(errorLabel, "error:invalid_namespace");
                return;
            }
            if (context.packState().createPack(name, namespace) == null) {
                setError(errorLabel, "error:pack_create_failed");
                return;
            }
            dialog.close();
        });

        dialog.addFooterButton(cancelBtn).addFooterButton(createBtn);
        return dialog;
    }

    private static Label fieldLabel(String key) {
        Label label = new Label(I18n.get(key));
        label.setTextFill(VoxelColors.ZINC_300);
        label.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 12));
        return label;
    }

    private static void setError(Label label, String messageKey) {
        label.setText(I18n.get(messageKey));
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void clearError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}
