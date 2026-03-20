package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.debug.RecordIntrospector;
import fr.hardel.asset_editor.client.debug.RecordIntrospector.Field;
import fr.hardel.asset_editor.client.debug.RecordIntrospector.FieldValue;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

public final class KeyValueGrid extends VBox {

    private static final int DEFAULT_ITEMS_VISIBLE = 5;

    public KeyValueGrid(List<Field> fields) {
        setFillWidth(true);
        setSpacing(2);

        for (Field field : fields)
            getChildren().add(buildFieldRow(field, 0));
    }

    public KeyValueGrid(Object record) {
        this(RecordIntrospector.introspect(record));
    }

    private static Node buildFieldRow(Field field, int depth) {
        return switch (field.value()) {
            case FieldValue.Scalar scalar -> scalarRow(field.name(), scalar.text(), depth);
            case FieldValue.Nested nested -> nestedSection(field.name(), nested.children(), depth);
            case FieldValue.Items items -> itemsSection(field.name(), items, depth);
        };
    }

    private static Node scalarRow(String key, String value, int depth) {
        Label keyLabel = new Label(key);
        keyLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.MEDIUM, 11));
        keyLabel.setTextFill(VoxelColors.ZINC_500);
        keyLabel.setMinWidth(Region.USE_PREF_SIZE);

        Label valueLabel = new Label(value);
        valueLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        valueLabel.setTextFill(VoxelColors.ZINC_300);
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        CopyButton copy = new CopyButton(value);

        HBox row = new HBox(8, keyLabel, valueLabel, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8 + depth * 12));
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static Node nestedSection(String key, List<Field> children, int depth) {
        Label header = new Label(key);
        header.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        header.setTextFill(VoxelColors.ZINC_400);
        header.setPadding(new Insets(4, 8, 2, 8 + depth * 12));

        VBox section = new VBox(2, header);
        section.setFillWidth(true);
        for (Field child : children)
            section.getChildren().add(buildFieldRow(child, depth + 1));

        return section;
    }

    private static Node itemsSection(String key, FieldValue.Items items, int depth) {
        Label header = new Label(key + " (" + items.totalSize() + ")");
        header.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 11));
        header.setTextFill(VoxelColors.ZINC_400);
        header.setPadding(new Insets(4, 8, 2, 8 + depth * 12));

        VBox section = new VBox(2, header);
        section.setFillWidth(true);

        int shown = 0;
        for (FieldValue item : items.preview()) {
            switch (item) {
                case FieldValue.Scalar scalar -> {
                    Label val = new Label(scalar.text());
                    val.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
                    val.setTextFill(VoxelColors.ZINC_300);
                    val.setPadding(new Insets(2, 8, 2, 8 + (depth + 1) * 12));
                    val.setWrapText(true);
                    section.getChildren().add(val);
                    shown++;
                }
                case FieldValue.Nested nested -> {
                    for (Field child : nested.children()) {
                        section.getChildren().add(buildFieldRow(child, depth + 1));
                        shown++;
                    }
                }
                case FieldValue.Items subItems -> {
                    section.getChildren().add(itemsSection("[" + shown + "]", subItems, depth + 1));
                    shown++;
                }
            }
        }

        if (items.totalSize() > DEFAULT_ITEMS_VISIBLE && shown < items.totalSize()) {
            int remaining = items.totalSize() - shown;
            Label more = new Label(I18n.get("debug:keyvalue.more", remaining));
            more.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 11));
            more.setTextFill(VoxelColors.ZINC_600);
            more.setPadding(new Insets(2, 8, 2, 8 + (depth + 1) * 12));
            section.getChildren().add(more);
        }

        return section;
    }
}
