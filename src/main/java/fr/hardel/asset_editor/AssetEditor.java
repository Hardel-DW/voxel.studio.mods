package fr.hardel.asset_editor;

import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.network.workspace.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.network.workspace.impl.EnchantmentInterpreter;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissionCommand;
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final boolean DEV_DISABLE_SINGLEPLAYER_ADMIN = false;

    @Override
    public void onInitialize() {
        RegistryWorkspaceBinding<Enchantment> enchantmentBinding = new RegistryWorkspaceBinding<>(
            Registries.ENCHANTMENT,
            Enchantment.DIRECT_CODEC,
            EnchantmentFlushAdapter.INSTANCE,
            new EnchantmentInterpreter(),
            entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags()));
        AssetEditorNetworking.registerBinding(enchantmentBinding);
        AssetEditorNetworking.registerServer();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PermissionManager.init(server);
            WorkspaceRepository.init(server);
            ServerPackManager.init(server);
            snapshotServerRegistries(server, enchantmentBinding);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PermissionManager.shutdown();
            WorkspaceRepository.shutdown();
            ServerPackManager.shutdown();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                var permManager = PermissionManager.get();
                if (permManager != null)
                    permManager.syncToPlayer(handler.getPlayer());

                var packManager = ServerPackManager.get();
                if (packManager != null)
                    AssetEditorNetworking.sendPackList(handler.getPlayer(), packManager.listPacks());
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StudioPermissionCommand.register(dispatcher));
    }

    private static void snapshotServerRegistries(MinecraftServer server, RegistryWorkspaceBinding<Enchantment> enchantmentBinding) {
        var repository = WorkspaceRepository.get();
        if (repository == null)
            return;

        var selectedPacks = server.getPackRepository().getSelectedPacks();
        var vanillaPacks = new ArrayList<>(selectedPacks.stream()
            .filter(p -> p.getPackSource() != PackSource.WORLD)
            .map(p -> p.open()).toList());

        try (var vanillaResources = new MultiPackResourceManager(PackType.SERVER_DATA, vanillaPacks)) {
            server.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(registry -> {
                repository.snapshotBaseline(enchantmentBinding, vanillaResources, registry, server.registryAccess());
            });
        }
    }
}
