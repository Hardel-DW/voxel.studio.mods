package fr.hardel.asset_editor.client.javafx;

import javafx.scene.text.Font;

import java.util.EnumMap;
import java.util.List;
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

    public static void register(Variant variant, Font f) {
        if (f != null) registry.put(variant, f.getName());
    }

    public static Font of(Variant variant, double size) {
        return new Font(registry.getOrDefault(variant, variant.defaultFamily), size);
    }

    public static Font firstAvailable(double size, String... families) {
        List<String> availableFamilies = Font.getFamilies();
        for (String family : families) {
            if (availableFamilies.contains(family))
                return Font.font(family, size);
        }

        return Font.font("System", size);
    }

    public static Font codeBlock(double size) {
        return firstAvailable(size, "JetBrains Mono", "Cascadia Code", "Consolas", "Monospaced");
    }

    private VoxelFonts() {}
}
