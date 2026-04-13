package fr.hardel.asset_editor.client.bootstrap.ui;

import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import fr.hardel.asset_editor.client.bootstrap.ComposeManifest;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class ComposeInstallConfirmScreen extends Screen {

    private static final Component TITLE = Component.translatable("asset_editor.bootstrap.confirm.title");
    private static final Component DESCRIPTION = Component.translatable("asset_editor.bootstrap.confirm.description");
    private static final Component INSTALL = Component.translatable("asset_editor.bootstrap.confirm.install");
    private static final Component RETRY = Component.translatable("asset_editor.bootstrap.confirm.retry");
    private static final Component CANCEL = CommonComponents.GUI_CANCEL;
    private static final Component ERROR_TITLE = Component.translatable("asset_editor.bootstrap.confirm.error_title");

    private static final int TITLE_Y = 60;
    private static final int MESSAGE_Y = 96;
    private static final int LINE_HEIGHT = 12;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_OFFSET = 48;
    private static final int SIDE_MARGIN = 60;

    private MultiLineLabel body = MultiLineLabel.EMPTY;
    private final boolean errorMode;

    public ComposeInstallConfirmScreen() {
        super(TITLE);
        this.errorMode = ComposeBootstrap.hasFailed();
    }

    @Override
    protected void init() {
        super.init();
        this.body = MultiLineLabel.create(this.font, buildBody(), this.width - SIDE_MARGIN);
        layoutButtons();
    }

    private void layoutButtons() {
        int buttonY = this.height - BUTTON_BOTTOM_OFFSET;
        int twoButtonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int leftX = (this.width - twoButtonsWidth) / 2;
        Component primaryLabel = errorMode ? RETRY : INSTALL;

        this.addRenderableWidget(Button.builder(primaryLabel, button -> onPrimary())
            .bounds(leftX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        this.addRenderableWidget(Button.builder(CANCEL, button -> this.onClose())
            .bounds(leftX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    private Component buildBody() {
        if (errorMode)
            return Component.translatable("asset_editor.bootstrap.confirm.error", ComposeBootstrap.errorMessage());

        Component sizeText = formatDownloadSize();
        return Component.translatable("asset_editor.bootstrap.confirm.body", DESCRIPTION, sizeText);
    }

    private Component formatDownloadSize() {
        ComposeManifest manifest = ComposeBootstrap.manifest();
        if (manifest == null) return Component.translatable("asset_editor.bootstrap.unit.mb", "?");
        long mb = Math.max(1L, manifest.totalSize() / (1024L * 1024L));
        return Component.translatable("asset_editor.bootstrap.unit.mb", mb);
    }

    private void onPrimary() {
        ComposeBootstrap.startInstall();
        this.onClose();
    }

    @Override
    public void render(final @NonNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        Component displayedTitle = errorMode ? ERROR_TITLE : this.title;
        graphics.drawCenteredString(this.font, displayedTitle, this.width / 2, TITLE_Y, -1);
        this.body.visitLines(TextAlignment.CENTER, this.width / 2, MESSAGE_Y, LINE_HEIGHT, textRenderer);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
