package fr.hardel.asset_editor.client.javafx;

import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores loaded Rubik font names after Font.loadFont() registration.
 * Font.font(family, weight, size) is unreliable when static TTF files register
 * under legacy family names like "Rubik ExtraBold" instead of "Rubik".
 * Using new Font(name, size) with the exact registered name is always correct.
 */
public final class VoxelFonts {

    public enum Rubik {
        LIGHT("rubik-light"),
        LIGHT_ITALIC("rubik-lightitalic"),
        REGULAR("rubik-regular"),
        ITALIC("rubik-italic"),
        MEDIUM("rubik-medium"),
        MEDIUM_ITALIC("rubik-mediumitalic"),
        SEMI_BOLD("rubik-semibold"),
        SEMI_BOLD_ITALIC("rubik-semibolditalic"),
        BOLD("rubik-bold"),
        BOLD_ITALIC("rubik-bolditalic"),
        EXTRA_BOLD("rubik-extrabold"),
        EXTRA_BOLD_ITALIC("rubik-extrabolditalic"),
        BLACK("rubik-black"),
        BLACK_ITALIC("rubik-blackitalic");

        public final String fileName;

        Rubik(String fileName) {
            this.fileName = fileName;
        }
    }

    public enum Minecraft {
        TEN("minecraftten"),
        SEVEN("seven");

        public final String fileName;

        Minecraft(String fileName) {
            this.fileName = fileName;
        }
    }

    private static final Map<Rubik, String> registry = new HashMap<>();
    private static final Map<Minecraft, String> minecraftRegistry = new HashMap<>();

    static void register(Rubik weight, Font f) {
        if (f != null) registry.put(weight, f.getName());
    }

    static void registerMinecraft(Minecraft font, Font f) {
        if (f != null) minecraftRegistry.put(font, f.getName());
    }

    public static Font rubik(Rubik weight, double size) {
        return new Font(registry.getOrDefault(weight, "Rubik"), size);
    }

    public static Font minecraft(Minecraft font, double size) {
        return new Font(minecraftRegistry.getOrDefault(font, "Minecraft"), size);
    }

    private VoxelFonts() {}
}


