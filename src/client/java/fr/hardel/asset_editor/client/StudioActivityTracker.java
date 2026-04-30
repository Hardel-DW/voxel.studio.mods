package fr.hardel.asset_editor.client;

import net.minecraft.util.Util;

public final class StudioActivityTracker {

    private static final long RECENT_ACTIVITY_MS = 30_000L;

    private static volatile boolean visible;
    private static volatile boolean focused;
    private static volatile long lastActivityMillis;

    public static void markVisible() {
        visible = true;
        markActivity();
    }

    public static void markHidden() {
        visible = false;
        focused = false;
    }

    public static void markFocused() {
        focused = true;
        markActivity();
    }

    public static void markUnfocused() {
        focused = false;
        markActivity();
    }

    public static void markActivity() {
        lastActivityMillis = Util.getMillis();
    }

    public static boolean shouldUseMinecraftFramerateLimit() {
        if (!visible) {
            return false;
        }
        return focused || Util.getMillis() - lastActivityMillis <= RECENT_ACTIVITY_MS;
    }

    private StudioActivityTracker() {}
}
