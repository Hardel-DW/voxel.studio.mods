package fr.hardel.asset_editor.permission;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class StudioPermissionCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ROLES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"admin", "contributor", "none"}, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("studio")
                .requires(StudioPermissionCommand::canManagePermissions)
                .then(Commands.literal("info")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(StudioPermissionCommand::showInfo)))
                .then(Commands.literal("role")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .suggests(SUGGEST_ROLES)
                                                .executes(StudioPermissionCommand::setRole))))));
    }

    private static boolean canManagePermissions(CommandSourceStack source) {
        var manager = PermissionManager.get();
        if (manager == null) return false;
        if (source.getPlayer() == null) return true;
        return manager.isAdmin(source.getPlayer().getUUID());
    }

    private static int setRole(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String roleName = StringArgumentType.getString(ctx, "role");
        StudioRole role = switch (roleName) {
            case "admin" -> StudioRole.ADMIN;
            case "contributor" -> StudioRole.CONTRIBUTOR;
            case "none" -> StudioRole.NONE;
            default -> null;
        };
        if (role == null) {
            ctx.getSource().sendFailure(Component.translatable("studio:permission.invalid_role"));
            return 0;
        }

        PermissionManager.get().setPermissions(target.getUUID(), new StudioPermissions(role));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.role_set", target.getDisplayName(), roleName), true);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var perms = PermissionManager.get().getEffectivePermissions(target);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.info.header", target.getDisplayName()), false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.info.role", perms.role().getSerializedName()), false);
        return 1;
    }

    private StudioPermissionCommand() {
    }
}
