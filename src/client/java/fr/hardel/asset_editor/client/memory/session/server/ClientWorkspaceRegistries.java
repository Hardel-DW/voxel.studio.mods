package fr.hardel.asset_editor.client.memory.session.server;

import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.flush.Workspaces;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientWorkspaceRegistries {

    private static final Map<String, ClientWorkspaceRegistry<?>> REGISTRIES = new LinkedHashMap<>();

    public static final ClientWorkspaceRegistry<Enchantment> ENCHANTMENT = register(Workspaces.ENCHANTMENT);
    public static final ClientWorkspaceRegistry<LootTable> LOOT_TABLE = register(Workspaces.LOOT_TABLE);
    public static final ClientWorkspaceRegistry<Recipe<?>> RECIPE = register(Workspaces.RECIPE);

    private static <T> ClientWorkspaceRegistry<T> register(WorkspaceDefinition<T> definition) {
        ClientWorkspaceRegistry<T> registry = ClientWorkspaceRegistry.of(definition);
        ClientWorkspaceRegistry<?> previous = REGISTRIES.putIfAbsent(definition.registryId().toString(), registry);
        if (previous != null)
            throw new IllegalStateException("Duplicate client workspace registry: " + definition.registryId());
        return registry;
    }

    public static ClientWorkspaceRegistry<?> get(Identifier registryId) {
        if (registryId == null)
            return null;
        return REGISTRIES.get(registryId.toString());
    }

    public static Collection<ClientWorkspaceRegistry<?>> all() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    private ClientWorkspaceRegistries() {}
}
