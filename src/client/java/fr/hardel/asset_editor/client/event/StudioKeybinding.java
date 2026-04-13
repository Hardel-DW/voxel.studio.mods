package fr.hardel.asset_editor.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import fr.hardel.asset_editor.AssetEditor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class StudioKeybinding {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "main"));

    private static final KeyMapping OPEN_STUDIO = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.asset_editor.open_studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
    private static final KeyMapping TOGGLE_BOOTSTRAP_HUD = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.asset_editor.toggle_bootstrap_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY));

    public static boolean consumeOpenStudio() {
        return OPEN_STUDIO.consumeClick();
    }

    public static boolean consumeToggleBootstrapHud() {
        return TOGGLE_BOOTSTRAP_HUD.consumeClick();
    }

    public static void register() {
        // Force class loading to register keybindings
    }

    private StudioKeybinding() {}
}
