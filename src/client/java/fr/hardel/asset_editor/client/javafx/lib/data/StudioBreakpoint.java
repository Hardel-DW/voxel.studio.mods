package fr.hardel.asset_editor.client.javafx.lib.data;

public enum StudioBreakpoint {
    SM(640),
    MD(768),
    LG(1024),
    XL(1280),
    XXL(1536);

    private final double px;

    StudioBreakpoint(double px) {
        this.px = px;
    }

    public double px() {
        return px;
    }
}
