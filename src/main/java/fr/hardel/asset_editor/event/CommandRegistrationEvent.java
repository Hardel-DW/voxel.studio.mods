package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.permission.StudioPermissionCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CommandRegistrationEvent {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            StudioPermissionCommand.register(dispatcher));
    }

    private CommandRegistrationEvent() {
    }
}
