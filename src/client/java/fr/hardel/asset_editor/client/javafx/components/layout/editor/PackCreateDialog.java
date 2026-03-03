package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Dialog;
import fr.hardel.asset_editor.client.javafx.components.ui.FileInput;
import fr.hardel.asset_editor.client.javafx.components.ui.StudioButton;
import fr.hardel.asset_editor.client.javafx.components.ui.TextInput;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState.PackInfo;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

public final class PackCreateDialog {

    private PackCreateDialog() {}

    public static Dialog create(StudioContext context) {
        TextInput nameInput = new TextInput("studio:pack.create.name.placeholder");
        TextInput namespaceInput = new TextInput("studio:pack.create.namespace.placeholder");
        FileInput iconInput = new FileInput("studio:pack.create.icon.placeholder", "*.png", file -> {});

        nameInput.field().textProperty().addListener((obs, o, v) -> {
            if (v != null && namespaceInput.getText().isEmpty()) {
                namespaceInput.setText(v.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
            }
        });

        Label nameLabel = fieldLabel("studio:pack.create.name");
        Label nsLabel = fieldLabel("studio:pack.create.namespace");
        Label iconLabel = fieldLabel("studio:pack.create.icon");

        VBox form = new VBox(12,
                nameLabel, nameInput,
                nsLabel, namespaceInput,
                iconLabel, iconInput
        );
        form.setPadding(new Insets(8, 0, 0, 0));

        Dialog dialog = new Dialog("studio:pack.create.title", form);

        StudioButton cancelBtn = new StudioButton(StudioButton.Variant.GHOST_BORDER, StudioButton.Size.SM,
                I18n.get("studio:action.cancel"));
        cancelBtn.setOnAction(dialog::close);

        StudioButton createBtn = new StudioButton(StudioButton.Variant.SHIMMER, StudioButton.Size.SM,
                I18n.get("studio:action.create"));
        createBtn.setOnAction(() -> {
            String name = nameInput.getText().trim();
            String namespace = namespaceInput.getText().trim();
            if (name.isEmpty() || namespace.isEmpty()) return;
            context.packState().availablePacks().add(new PackInfo(name, List.of(namespace)));
            context.packState().selectPack(new PackInfo(name, List.of(namespace)));
            dialog.close();
        });

        dialog.addFooterButton(cancelBtn).addFooterButton(createBtn);
        return dialog;
    }

    private static Label fieldLabel(String key) {
        Label label = new Label(I18n.get(key));
        label.setTextFill(VoxelColors.ZINC_300);
        label.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.MEDIUM, 12));
        return label;
    }
}
