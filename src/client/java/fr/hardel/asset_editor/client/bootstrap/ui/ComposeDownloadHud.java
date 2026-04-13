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
    private static final int DETAIL_Y_OFFSET = 12;
    private static final int TEXT_MAX_WIDTH = PANEL_WIDTH - PADDING * 2;

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

        boolean failed = state == ComposeBootstrap.State.FAILED;
        Font font = Minecraft.getInstance().font;
        drawPanel(graphics);
        drawTitle(graphics, font, failed);
        drawDetail(graphics, font, failed);
        drawProgressBar(graphics, failed);
    }

    private static void drawPanel(GuiGraphics graphics) {
        graphics.fill(MARGIN, MARGIN, MARGIN + PANEL_WIDTH, MARGIN + PANEL_HEIGHT, COLOR_BG);
        drawBorder(graphics, MARGIN, MARGIN, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);
    }

    private static void drawTitle(GuiGraphics graphics, Font font, boolean failed) {
        Component title = Component.translatable(failed
            ? "asset_editor.bootstrap.hud.failed"
            : "asset_editor.bootstrap.hud.title");
        graphics.drawString(font, title, MARGIN + PADDING, MARGIN + PADDING, failed ? COLOR_TITLE_ERROR : COLOR_TITLE);
    }

    private static void drawDetail(GuiGraphics graphics, Font font, boolean failed) {
        Component detail = failed
            ? truncate(font, ComposeBootstrap.errorMessage())
            : buildProgressDetail();
        graphics.drawString(font, detail, MARGIN + PADDING, MARGIN + PADDING + DETAIL_Y_OFFSET, COLOR_DETAIL);
    }

    private static void drawProgressBar(GuiGraphics graphics, boolean failed) {
        int barX = MARGIN + PADDING;
        int barY = MARGIN + PANEL_HEIGHT - PADDING - BAR_HEIGHT;
        int barWidth = TEXT_MAX_WIDTH;
        graphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, COLOR_BAR_BG);
        float progress = failed ? 1f : ComposeBootstrap.progress();
        int fill = Math.round(barWidth * progress);
        if (fill > 0)
            graphics.fill(barX, barY, barX + fill, barY + BAR_HEIGHT, failed ? COLOR_BAR_ERROR : COLOR_BAR_FILL);
    }

    private static Component buildProgressDetail() {
        return Component.translatable(
            "asset_editor.bootstrap.hud.progress",
            formatMegabytes(ComposeBootstrap.downloadedBytes()),
            formatMegabytes(ComposeBootstrap.totalBytes()),
            ComposeBootstrap.currentIndex(),
            ComposeBootstrap.totalArtifacts());
    }

    private static Component formatMegabytes(long bytes) {
        float mb = bytes / (1024f * 1024f);
        return Component.translatable("asset_editor.bootstrap.unit.mb", String.format("%.1f", mb));
    }

    private static Component truncate(Font font, Component component) {
        String text = component.getString();
        if (text.isEmpty()) return Component.empty();
        if (font.width(text) <= TEXT_MAX_WIDTH) return component;

        String suffix = "…";
        StringBuilder cut = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String probe = cut.toString() + text.charAt(i) + suffix;
            if (font.width(probe) > TEXT_MAX_WIDTH) break;
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
