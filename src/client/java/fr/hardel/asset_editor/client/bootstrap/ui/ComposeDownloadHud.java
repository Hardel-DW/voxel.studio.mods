package fr.hardel.asset_editor.client.bootstrap.ui;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ComposeDownloadHud {

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "compose_bootstrap");

    private static final int MARGIN = 8;
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 44;
    private static final int PADDING = 6;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_GAP_TOP = 4;

    private static final int COLOR_BG = 0xCC101217;
    private static final int COLOR_BORDER = 0xFF2A2F3A;
    private static final int COLOR_BAR_BG = 0xFF1E2330;
    private static final int COLOR_BAR_FILL = 0xFF4EA1FF;
    private static final int COLOR_BAR_ERROR = 0xFFE15554;
    private static final int COLOR_TITLE = 0xFFEDEFF2;
    private static final int COLOR_DETAIL = 0xFF8A94A6;
    private static final int COLOR_TITLE_ERROR = 0xFFE15554;

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, ComposeDownloadHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ComposeBootstrap.State state = ComposeBootstrap.state();
        if (state != ComposeBootstrap.State.DOWNLOADING && state != ComposeBootstrap.State.FAILED)
            return;

        Font font = Minecraft.getInstance().font;
        int x = MARGIN;
        int y = MARGIN;

        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BG);
        drawBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        boolean failed = state == ComposeBootstrap.State.FAILED;
        Component title = Component.translatable(failed
            ? "asset_editor.bootstrap.hud.failed"
            : "asset_editor.bootstrap.hud.title");
        graphics.drawString(font, title, x + PADDING, y + PADDING, failed ? COLOR_TITLE_ERROR : COLOR_TITLE);

        Component detail = failed
            ? truncate(font, ComposeBootstrap.errorMessage(), PANEL_WIDTH - PADDING * 2)
            : buildProgressDetail(font);
        graphics.drawString(font, detail, x + PADDING, y + PADDING + 12, COLOR_DETAIL);

        int barX = x + PADDING;
        int barY = y + PANEL_HEIGHT - PADDING - BAR_HEIGHT;
        int barWidth = PANEL_WIDTH - PADDING * 2;
        graphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, COLOR_BAR_BG);
        float progress = failed ? 1f : ComposeBootstrap.progress();
        int fill = Math.round(barWidth * progress);
        if (fill > 0)
            graphics.fill(barX, barY, barX + fill, barY + BAR_HEIGHT, failed ? COLOR_BAR_ERROR : COLOR_BAR_FILL);
    }

    private static Component buildProgressDetail(Font font) {
        long downloaded = ComposeBootstrap.downloadedBytes();
        long total = ComposeBootstrap.totalBytes();
        int index = ComposeBootstrap.currentIndex();
        int count = ComposeBootstrap.totalArtifacts();
        return Component.translatable(
            "asset_editor.bootstrap.hud.progress",
            formatMb(downloaded),
            formatMb(total),
            index,
            count);
    }

    private static String formatMb(long bytes) {
        float mb = bytes / (1024f * 1024f);
        return String.format("%.1f MB", mb);
    }

    private static Component truncate(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (font.width(text) <= maxWidth) return Component.literal(text);
        String suffix = "…";
        int suffixWidth = font.width(suffix);
        StringBuilder cut = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String probe = cut.toString() + text.charAt(i) + suffix;
            if (font.width(probe) > maxWidth) break;
            cut.append(text.charAt(i));
        }
        return Component.literal(cut.toString() + suffix);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private ComposeDownloadHud() {}
}
