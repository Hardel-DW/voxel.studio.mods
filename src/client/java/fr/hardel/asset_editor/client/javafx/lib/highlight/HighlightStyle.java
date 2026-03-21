package fr.hardel.asset_editor.client.javafx.lib.highlight;

import javafx.scene.paint.Paint;

public final class HighlightStyle {

    private final Paint foreground;
    private final Paint background;
    private final Paint underline;

    public HighlightStyle(Paint foreground, Paint background, Paint underline) {
        this.foreground = foreground;
        this.background = background;
        this.underline = underline;
    }

    public static HighlightStyle foreground(Paint foreground) {
        return new HighlightStyle(foreground, null, null);
    }

    public static HighlightStyle background(Paint background) {
        return new HighlightStyle(null, background, null);
    }

    public Paint foreground() {
        return foreground;
    }

    public Paint background() {
        return background;
    }

    public Paint underline() {
        return underline;
    }

    public boolean hasForeground() {
        return foreground != null;
    }

    public boolean hasBackground() {
        return background != null;
    }

    public boolean hasUnderline() {
        return underline != null;
    }

    public HighlightStyle withForeground(Paint value) {
        return new HighlightStyle(value, background, underline);
    }

    public HighlightStyle withBackground(Paint value) {
        return new HighlightStyle(foreground, value, underline);
    }

    public HighlightStyle withUnderline(Paint value) {
        return new HighlightStyle(foreground, background, value);
    }
}
