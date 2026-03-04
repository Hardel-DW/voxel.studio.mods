package fr.hardel.asset_editor.client.javafx;

import javafx.scene.text.Font;

import java.util.EnumMap;
import java.util.Map;

public final class VoxelFonts {

    public enum Variant {
        LIGHT("rubik-light", "Rubik"),
        LIGHT_ITALIC("rubik-lightitalic", "Rubik"),
        REGULAR("rubik-regular", "Rubik"),
        ITALIC("rubik-italic", "Rubik"),
        MEDIUM("rubik-medium", "Rubik"),
        MEDIUM_ITALIC("rubik-mediumitalic", "Rubik"),
        SEMI_BOLD("rubik-semibold", "Rubik"),
        SEMI_BOLD_ITALIC("rubik-semibolditalic", "Rubik"),
        BOLD("rubik-bold", "Rubik"),
        BOLD_ITALIC("rubik-bolditalic", "Rubik"),
        EXTRA_BOLD("rubik-extrabold", "Rubik"),
        EXTRA_BOLD_ITALIC("rubik-extrabolditalic", "Rubik"),
        BLACK("rubik-black", "Rubik"),
        BLACK_ITALIC("rubik-blackitalic", "Rubik"),
        MINECRAFT_TEN("minecraftten", "Minecraft"),
        MINECRAFT_SEVEN("seven", "Minecraft");

        public final String fileName;
        public final String defaultFamily;

        Variant(String fileName, String defaultFamily) {
            this.fileName = fileName;
            this.defaultFamily = defaultFamily;
        }
    }

    private static final Map<Variant, String> registry = new EnumMap<>(Variant.class);

    static void register(Variant variant, Font f) {
        if (f != null) registry.put(variant, f.getName());
    }

    public static Font of(Variant variant, double size) {
        return new Font(registry.getOrDefault(variant, variant.defaultFamily), size);
    }

    private VoxelFonts() {}
}
