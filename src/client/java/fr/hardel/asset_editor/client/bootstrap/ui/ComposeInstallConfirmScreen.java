package fr.hardel.asset_editor.client.bootstrap.ui;

import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ComposeInstallConfirmScreen extends Screen {

    private static final Component TITLE = Component.translatable("asset_editor.bootstrap.confirm.title");
    private static final Component DESCRIPTION = Component.translatable("asset_editor.bootstrap.confirm.description");
    private static final Component INSTALL = Component.translatable("asset_editor.bootstrap.confirm.install");
    private static final Component RETRY = Component.translatable("asset_editor.bootstrap.confirm.retry");
    private static final Component CANCEL = CommonComponents.GUI_CANCEL;
    private static final Component ERROR_TITLE = Component.translatable("asset_editor.bootstrap.confirm.error_title");

    private static final int TITLE_Y = 60;
    private static final int MESSAGE_Y = 96;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;

    private MultiLineLabel description = MultiLineLabel.EMPTY;
    private MultiLineLabel error = MultiLineLabel.EMPTY;
    private final boolean errorMode;

    public ComposeInstallConfirmScreen() {
        super(TITLE);
        this.errorMode = ComposeBootstrap.hasFailed();
    }

    @Override
    protected void init() {
        super.init();
        Component body = errorMode
            ? Component.translatable("asset_editor.bootstrap.confirm.error", ComposeBootstrap.errorMessage())
            : bodyWithSize();
        this.description = MultiLineLabel.create(this.font, DESCRIPTION, this.width - 60);
        this.error = MultiLineLabel.create(this.font, body, this.width - 60);

        int buttonY = this.height - 48;
        int twoButtonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int leftX = (this.width - twoButtonsWidth) / 2;

        Component primaryLabel = errorMode ? RETRY : INSTALL;
        this.addRenderableWidget(
            Button.builder(primaryLabel, button -> onPrimary())
                .bounds(leftX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(
            Button.builder(CANCEL, button -> this.onClose())
                .bounds(leftX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private Component bodyWithSize() {
        ComposeBootstrap.manifest();
        var manifest = ComposeBootstrap.manifest();
        long mb = manifest == null ? 0L : Math.max(1L, manifest.totalSize() / (1024L * 1024L));
        return Component.translatable("asset_editor.bootstrap.confirm.size", mb);
    }

    private void onPrimary() {
        ComposeBootstrap.startInstall();
        this.onClose();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        Component displayedTitle = errorMode ? ERROR_TITLE : this.title;
        graphics.drawCenteredString(this.font, displayedTitle, this.width / 2, TITLE_Y, -1);

        if (errorMode) {
            this.error.visitLines(TextAlignment.CENTER, this.width / 2, MESSAGE_Y, 12, textRenderer);
        } else {
            this.description.visitLines(TextAlignment.CENTER, this.width / 2, MESSAGE_Y, 12, textRenderer);
            int sizeY = MESSAGE_Y + (this.description.getLineCount() + 1) * 12;
            this.error.visitLines(TextAlignment.CENTER, this.width / 2, sizeY, 12, textRenderer);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
