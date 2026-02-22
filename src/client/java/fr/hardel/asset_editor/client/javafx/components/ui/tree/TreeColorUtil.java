package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import javafx.scene.paint.Color;

public final class TreeColorUtil {

    public static int stringToHue(String text) {
        int hash = 0;
        for (int i = 0; i < text.length(); i++) {
            hash = text.charAt(i) + ((hash << 5) - hash);
        }
        return (int) (Math.abs((long) hash) % 360L);
    }

    public static Color hueToColor(int hue) {
        return hslToColor(hue, 70, 60);
    }

    public static Color hslToColor(double h, double s, double l) {
        double hue = ((h % 360) + 360) % 360;
        double sat = clamp01(s / 100.0);
        double lig = clamp01(l / 100.0);
        double c = (1.0 - Math.abs(2.0 * lig - 1.0)) * sat;
        double x = c * (1.0 - Math.abs((hue / 60.0) % 2.0 - 1.0));
        double m = lig - c / 2.0;

        double r = 0;
        double g = 0;
        double b = 0;
        if (hue < 60.0) {
            r = c;
            g = x;
        } else if (hue < 120.0) {
            r = x;
            g = c;
        } else if (hue < 180.0) {
            g = c;
            b = x;
        } else if (hue < 240.0) {
            g = x;
            b = c;
        } else if (hue < 300.0) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }

        return Color.color(clamp01(r + m), clamp01(g + m), clamp01(b + m));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private TreeColorUtil() {
    }
}
