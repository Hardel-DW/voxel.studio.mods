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

import java.util.stream.Collectors;

public final class StudioPermissionCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ROLES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"admin", "contributor", "none"}, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CONCEPTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ConceptRegistry.all().stream().map(ConceptRegistry::name), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_GRANTED_CONCEPTS = (ctx, builder) -> {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var perms = PermissionManager.get().getStoredPermissions(target);
        return SharedSuggestionProvider.suggest(perms.grantedConcepts(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ALL_FOLDERS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ConceptRegistry.allDataFolders(), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ALLOWED_FOLDERS = (ctx, builder) -> {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var perms = PermissionManager.get().getStoredPermissions(target);
        return SharedSuggestionProvider.suggest(perms.allowedRegistries(), builder);
    };

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
                                                .executes(StudioPermissionCommand::setRole)))))
                .then(Commands.literal("permission")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("concept")
                                        .then(Commands.literal("grant")
                                                .then(Commands.argument("concept", StringArgumentType.word())
                                                        .suggests(SUGGEST_CONCEPTS)
                                                        .executes(StudioPermissionCommand::conceptGrant)))
                                        .then(Commands.literal("revoke")
                                                .then(Commands.argument("concept", StringArgumentType.word())
                                                        .suggests(SUGGEST_GRANTED_CONCEPTS)
                                                        .executes(StudioPermissionCommand::conceptRevoke))))
                                .then(Commands.literal("registry")
                                        .then(Commands.literal("ban")
                                                .then(Commands.argument("registry", StringArgumentType.word())
                                                        .suggests(SUGGEST_ALLOWED_FOLDERS)
                                                        .executes(StudioPermissionCommand::registryBan)))
                                        .then(Commands.literal("pardon")
                                                .then(Commands.argument("registry", StringArgumentType.word())
                                                        .suggests(SUGGEST_ALL_FOLDERS)
                                                        .executes(StudioPermissionCommand::registryPardon)))))));
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

        var manager = PermissionManager.get();
        var current = manager.getStoredPermissions(target);
        manager.setPermissions(target.getUUID(), current.withRole(role));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.role_set", target.getDisplayName(), roleName), true);
        return 1;
    }

    private static int conceptGrant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String conceptName = StringArgumentType.getString(ctx, "concept");
        if (ConceptRegistry.byName(conceptName) == null) {
            ctx.getSource().sendFailure(Component.translatable("studio:permission.invalid_concept"));
            return 0;
        }

        var manager = PermissionManager.get();
        var current = manager.getStoredPermissions(target);
        var updated = current.withConcept(conceptName, true);

        if (current.isNone() && !updated.isNone()) {
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("studio:permission.auto_promoted", target.getDisplayName()), true);
        }

        manager.setPermissions(target.getUUID(), updated);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.concept_granted", target.getDisplayName(), conceptName), true);
        return 1;
    }

    private static int conceptRevoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String conceptName = StringArgumentType.getString(ctx, "concept");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withConcept(conceptName, false));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.concept_revoked", target.getDisplayName(), conceptName), true);
        return 1;
    }

    private static int registryBan(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String folder = StringArgumentType.getString(ctx, "registry");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withRegistryBan(folder));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.registry_banned", target.getDisplayName(), folder), true);
        return 1;
    }

    private static int registryPardon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String folder = StringArgumentType.getString(ctx, "registry");
        var manager = PermissionManager.get();
        manager.setPermissions(target.getUUID(), manager.getStoredPermissions(target).withRegistryPardon(folder));
        ctx.getSource().sendSuccess(() ->
                Component.translatable("studio:permission.registry_pardoned", target.getDisplayName(), folder), true);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var perms = PermissionManager.get().getStoredPermissions(target);
        var source = ctx.getSource();

        source.sendSuccess(() -> Component.translatable("studio:permission.info.header", target.getDisplayName()), false);
        source.sendSuccess(() -> Component.translatable("studio:permission.info.role", perms.role().getSerializedName()), false);

        if (!perms.isAdmin()) {
            String concepts = perms.grantedConcepts().isEmpty() ? "none"
                    : String.join(", ", perms.grantedConcepts());
            source.sendSuccess(() -> Component.translatable("studio:permission.info.concepts", concepts), false);

            String registries = perms.allowedRegistries().isEmpty() ? "none"
                    : String.join(", ", perms.allowedRegistries());
            source.sendSuccess(() -> Component.translatable("studio:permission.info.registries", registries), false);
        }
        return 1;
    }

    private StudioPermissionCommand() {
    }
}
