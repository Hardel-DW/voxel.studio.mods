package fr.hardel.asset_editor.command;

import fr.hardel.asset_editor.permission.StudioPermissionCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class AssetEditorCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            StudioPermissionCommand.register(dispatcher));
    }

    private AssetEditorCommands() {
    }
}
