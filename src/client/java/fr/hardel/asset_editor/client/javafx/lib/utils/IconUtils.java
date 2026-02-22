package fr.hardel.asset_editor.client.javafx.lib.utils;

import net.minecraft.resources.Identifier;

public final class IconUtils {

    public static boolean isSvgIcon(Identifier icon) {
        if (icon == null) return false;
        String value = icon.toString();
        return value.startsWith("asset_editor:icons/") || value.endsWith(".svg");
    }

    private IconUtils() {
    }
}
