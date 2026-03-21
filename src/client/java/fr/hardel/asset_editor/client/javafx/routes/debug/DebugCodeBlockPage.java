package fr.hardel.asset_editor.client.javafx.routes.debug;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.CopyButton;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.codeblock.CodeBlock;
import fr.hardel.asset_editor.client.javafx.components.ui.codeblock.JsonCodeBlockHighlighter;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class DebugCodeBlockPage extends StackPane implements Page {

    private static final SamplePayload SAMPLE_PAYLOAD = new SamplePayload(
        "John Doe",
        "john@example.com",
        30,
        true,
        List.of("admin", "user"),
        new Address("123 Main St", "New York", "USA"),
        List.of(new Project(1, "Website Redesign"), new Project(2, "Mobile App")));

    private final CodeBlock jsonPreview = new CodeBlock();

    public DebugCodeBlockPage() {
        getStyleClass().add("concept-main-page");

        configureJsonPreview();

        Section previewSection = new Section(I18n.get("debug:code.title"));

        Label description = new Label(I18n.get("debug:code.description"));
        description.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
        description.setTextFill(VoxelColors.ZINC_400);
        description.setWrapText(true);

        CopyButton copyButton = new CopyButton(jsonPreview::getText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(12, spacer, copyButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        previewSection.addContent(description, toolbar, jsonPreview);
        previewSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(previewSection, Priority.ALWAYS);
        VBox.setVgrow(jsonPreview, Priority.ALWAYS);

        VBox column = new VBox(24, previewSection);
        column.setPadding(new Insets(32));
        column.setFillWidth(true);
        column.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(column);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("debug-subpage-scroll");

        column.minHeightProperty().bind(Bindings.createDoubleBinding(
            () -> Math.max(0, scroll.getViewportBounds().getHeight()),
            scroll.viewportBoundsProperty()
        ));

        getChildren().add(scroll);
    }

    private void configureJsonPreview() {
        JsonCodeBlockHighlighter.installDefaultPalette(jsonPreview.palette());
        jsonPreview.setHighlighter(new JsonCodeBlockHighlighter());
        jsonPreview.setTextFill(VoxelColors.ZINC_300);
        jsonPreview.setBackgroundFill(VoxelColors.ZINC_960);
        jsonPreview.setBorderFill(VoxelColors.ZINC_800);
        jsonPreview.setFont(VoxelFonts.codeBlock(14));
        jsonPreview.setFontSmoothingType(FontSmoothingType.LCD);
        jsonPreview.setContentPadding(new Insets(18));
        jsonPreview.setLineSpacing(5);
        jsonPreview.setWrapText(false);
        jsonPreview.setMinHeight(360);
        jsonPreview.setPrefHeight(Region.USE_COMPUTED_SIZE);
        jsonPreview.setMaxHeight(Double.MAX_VALUE);
        jsonPreview.setMaxWidth(Double.MAX_VALUE);
        jsonPreview.setText(encodeSampleJson());
    }

    private static String encodeSampleJson() {
        AtomicReference<String> encoded = new AtomicReference<>("{}");
        SamplePayload.CODEC.encodeStart(JsonOps.INSTANCE, SAMPLE_PAYLOAD).ifSuccess(json -> encoded.set(prettyPrint(json)));
        return encoded.get();
    }

    private static String prettyPrint(JsonElement json) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json);
    }

    private record Address(String street, String city, String country) {
        private static final Codec<Address> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("street").forGetter(Address::street),
            Codec.STRING.fieldOf("city").forGetter(Address::city),
            Codec.STRING.fieldOf("country").forGetter(Address::country)).apply(instance, Address::new));
    }

    private record Project(int id, String name) {
        private static final Codec<Project> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(Project::id),
            Codec.STRING.fieldOf("name").forGetter(Project::name)).apply(instance, Project::new));
    }

    private record SamplePayload(String name, String email, int age, boolean active, List<String> roles, Address address, List<Project> projects) {
        private static final Codec<SamplePayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SamplePayload::name),
            Codec.STRING.fieldOf("email").forGetter(SamplePayload::email),
            Codec.INT.fieldOf("age").forGetter(SamplePayload::age),
            Codec.BOOL.fieldOf("active").forGetter(SamplePayload::active),
            Codec.STRING.listOf().fieldOf("roles").forGetter(SamplePayload::roles),
            Address.CODEC.fieldOf("address").forGetter(SamplePayload::address),
            Project.CODEC.listOf().fieldOf("projects").forGetter(SamplePayload::projects)).apply(instance, SamplePayload::new));
    }
}
