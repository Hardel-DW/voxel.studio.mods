package fr.hardel.asset_editor.client.javafx.lib.utils;

import javafx.scene.paint.Color;

public final class ColorUtils {

    public static int stringToHue(String text) {
        int hash = 0;
        for (int i = 0; i < text.length(); i++) {
            hash = text.charAt(i) + ((hash << 5) - hash);
        }
        return (int) (Math.abs((long) hash) % 360L);
    }

    public static Color hueToColor(int hue) {
        double h = ((hue % 360) + 360) % 360;
        double s = 0.70;
        double l = 0.60;
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = l - c / 2.0;
        double r = 0;
        double g = 0;
        double b = 0;
        if (h < 60) {
            r = c;
            g = x;
        } else if (h < 120) {
            r = x;
            g = c;
        } else if (h < 180) {
            g = c;
            b = x;
        } else if (h < 240) {
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return Color.color(clamp(r + m), clamp(g + m), clamp(b + m));
    }

    public static String toCssRgba(Color color) {
        return "rgba(%d,%d,%d,%.3f)".formatted(
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private ColorUtils() {
    }
}
