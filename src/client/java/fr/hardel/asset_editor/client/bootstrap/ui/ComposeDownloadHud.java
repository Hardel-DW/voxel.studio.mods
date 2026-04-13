package fr.hardel.asset_editor.client.bootstrap.ui;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

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

    private static final long BANNER_DURATION_MS = 2_000L;
    private static final long BANNER_FADE_MS = 200L;

    private static volatile boolean hidden;
    private static volatile long bannerStartMs;
    private static volatile Component bannerMessage = Component.empty();
    private static volatile boolean bannerSuccess;

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, ComposeDownloadHud::render);
    }

    public static void toggleHidden() {
        hidden = !hidden;
    }

    public static boolean isHidden() {
        return hidden;
    }

    public static void showCompletionBanner(Component message, boolean success) {
        bannerMessage = message;
        bannerSuccess = success;
        bannerStartMs = System.currentTimeMillis();
    }

    public static void playCompletionSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F));
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        renderCompletionBanner(graphics);

        if (hidden || ComposeBootstrap.state() != ComposeBootstrap.State.DOWNLOADING)
            return;

        Font font = Minecraft.getInstance().font;
        drawPanel(graphics);
        drawTitle(graphics, font);
        drawDetail(graphics, font);
        drawProgressBar(graphics);
    }

    private static void drawPanel(GuiGraphics graphics) {
        graphics.fill(MARGIN, MARGIN, MARGIN + PANEL_WIDTH, MARGIN + PANEL_HEIGHT, COLOR_BG);
        drawBorder(graphics, MARGIN, MARGIN, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);
    }

    private static void drawTitle(GuiGraphics graphics, Font font) {
        graphics.drawString(font,
            Component.translatable("asset_editor.bootstrap.hud.title"),
            MARGIN + PADDING,
            MARGIN + PADDING,
            COLOR_TITLE);
    }

    private static void drawDetail(GuiGraphics graphics, Font font) {
        graphics.drawString(font, buildProgressDetail(), MARGIN + PADDING, MARGIN + PADDING + DETAIL_Y_OFFSET, COLOR_DETAIL);
    }

    private static void drawProgressBar(GuiGraphics graphics) {
        int barX = MARGIN + PADDING;
        int barY = MARGIN + PANEL_HEIGHT - PADDING - BAR_HEIGHT;
        int barWidth = TEXT_MAX_WIDTH;
        graphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, COLOR_BAR_BG);
        int fill = Math.round(barWidth * ComposeBootstrap.progress());
        if (fill > 0)
            graphics.fill(barX, barY, barX + fill, barY + BAR_HEIGHT, COLOR_BAR_FILL);
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

    private static void renderCompletionBanner(GuiGraphics graphics) {
        long started = bannerStartMs;
        if (started == 0L) return;

        long elapsed = System.currentTimeMillis() - started;
        if (elapsed >= BANNER_DURATION_MS) {
            bannerStartMs = 0L;
            bannerMessage = Component.empty();
            return;
        }

        float alpha = computeBannerAlpha(elapsed);
        if (alpha <= 0f) return;

        Font font = Minecraft.getInstance().font;
        if (bannerMessage.getString().isEmpty()) return;

        int width = Math.min(240, font.width(bannerMessage) + PADDING * 2);
        int height = 18;
        int bg = withAlpha(COLOR_BG, Math.round(255f * alpha));
        int border = withAlpha(bannerSuccess ? COLOR_BAR_FILL : COLOR_BAR_ERROR, Math.round(255f * alpha));
        int textColor = withAlpha(COLOR_TITLE, Math.round(255f * alpha));

        graphics.fill(MARGIN, MARGIN, MARGIN + width, MARGIN + height, bg);
        drawBorder(graphics, MARGIN, MARGIN, width, height, border);
        graphics.drawString(font, bannerMessage, MARGIN + PADDING, MARGIN + 5, textColor);
    }

    private static float computeBannerAlpha(long elapsedMs) {
        if (elapsedMs < BANNER_FADE_MS)
            return elapsedMs / (float) BANNER_FADE_MS;

        long fadeOutStart = BANNER_DURATION_MS - BANNER_FADE_MS;
        if (elapsedMs > fadeOutStart)
            return (BANNER_DURATION_MS - elapsedMs) / (float) BANNER_FADE_MS;

        return 1f;
    }

    private static int withAlpha(int color, int alpha) {
        int rgb = color & 0x00FFFFFF;
        return (Math.max(0, Math.min(255, alpha)) << 24) | rgb;
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private ComposeDownloadHud() {}
}
