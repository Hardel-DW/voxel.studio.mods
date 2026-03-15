package fr.hardel.asset_editor.permission;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Collectors;

public final class StudioPermissionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("studio")
                .requires(StudioPermissionCommand::canManagePermissions)
                .then(Commands.literal("permission")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("admin")
                                        .executes(StudioPermissionCommand::setAdmin))
                                .then(Commands.literal("contributor")
                                        .executes(StudioPermissionCommand::setContributor))
                                .then(Commands.literal("info")
                                        .executes(StudioPermissionCommand::showInfo))))
                .then(Commands.literal("allow")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("registry", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::allowRegistry))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("registry", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::denyRegistry))))
                .then(Commands.literal("whitelist")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("element", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::addWhitelist))))
                .then(Commands.literal("blacklist")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("element", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::addBlacklist))))
                .then(Commands.literal("unwhitelist")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("element", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::removeWhitelist))))
                .then(Commands.literal("unblacklist")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("element", IdentifierArgument.id())
                                        .executes(StudioPermissionCommand::removeBlacklist)))));
    }

    private static boolean canManagePermissions(CommandSourceStack source) {
        var manager = PermissionManager.get();
        if (manager == null) return false;
        if (source.getPlayer() == null) return true;
        return manager.isAdmin(source.getPlayer().getUUID());
    }

    private static int setAdmin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PermissionManager.get().setPermissions(target.getUUID(), StudioPermissions.ADMIN);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.set_admin", target.getDisplayName()), true);
        return 1;
    }

    private static int setContributor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var current = PermissionManager.get().getStoredPermissions(target);
        PermissionManager.get().setPermissions(target.getUUID(), current.withRole(StudioRole.CONTRIBUTOR));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.set_contributor", target.getDisplayName()), true);
        return 1;
    }

    private static int allowRegistry(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier registry = IdentifierArgument.getId(ctx, "registry");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withRegistry(registry, true));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.allowed_registry", target.getDisplayName(), registry.toString()), true);
        return 1;
    }

    private static int denyRegistry(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier registry = IdentifierArgument.getId(ctx, "registry");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withRegistry(registry, false));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.denied_registry", target.getDisplayName(), registry.toString()), true);
        return 1;
    }

    private static int addWhitelist(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier element = IdentifierArgument.getId(ctx, "element");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withWhitelist(element, true));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.whitelisted", target.getDisplayName(), element.toString()), true);
        return 1;
    }

    private static int removeWhitelist(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier element = IdentifierArgument.getId(ctx, "element");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withWhitelist(element, false));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.unwhitelisted", target.getDisplayName(), element.toString()), true);
        return 1;
    }

    private static int addBlacklist(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier element = IdentifierArgument.getId(ctx, "element");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withBlacklist(element, true));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.blacklisted", target.getDisplayName(), element.toString()), true);
        return 1;
    }

    private static int removeBlacklist(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Identifier element = IdentifierArgument.getId(ctx, "element");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withBlacklist(element, false));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.unblacklisted", target.getDisplayName(), element.toString()), true);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var perms = PermissionManager.get().getStoredPermissions(target);
        var source = ctx.getSource();

        source.sendSuccess(() -> Component.translatable("studio:permission.info.header", target.getDisplayName()), false);
        source.sendSuccess(() -> Component.translatable("studio:permission.info.role", perms.role().getSerializedName()), false);

        if (!perms.isAdmin()) {
            String registries = perms.allowedRegistries().isEmpty() ? "none"
                    : perms.allowedRegistries().stream().map(Identifier::toString).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.translatable("studio:permission.info.registries", registries), false);

            if (!perms.whitelist().isEmpty()) {
                String wl = perms.whitelist().stream().map(Identifier::toString).collect(Collectors.joining(", "));
                source.sendSuccess(() -> Component.translatable("studio:permission.info.whitelist", wl), false);
            }
            if (!perms.blacklist().isEmpty()) {
                String bl = perms.blacklist().stream().map(Identifier::toString).collect(Collectors.joining(", "));
                source.sendSuccess(() -> Component.translatable("studio:permission.info.blacklist", bl), false);
            }
        }
        return 1;
    }

    private StudioPermissionCommand() {
    }
}
