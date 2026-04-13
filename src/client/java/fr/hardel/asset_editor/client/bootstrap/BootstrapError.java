package fr.hardel.asset_editor.client.bootstrap;

import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class BootstrapError extends IOException {

    private final String translationKey;
    private final Object[] args;

    public BootstrapError(String translationKey, Object... args) {
        super(translationKey);
        this.translationKey = translationKey;
        this.args = args;
    }

    public BootstrapError(String translationKey, Throwable cause, Object... args) {
        super(translationKey, cause);
        this.translationKey = translationKey;
        this.args = args;
    }

    public Component asComponent() {
        return Component.translatable(translationKey, args);
    }
}
