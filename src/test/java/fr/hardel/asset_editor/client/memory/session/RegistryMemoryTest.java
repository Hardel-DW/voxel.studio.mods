package fr.hardel.asset_editor.client.memory.session;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry;
import fr.hardel.asset_editor.client.memory.session.server.RegistryMemory;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryMemoryTest {

    @Test
    void putReplaceAllAndClearMaintainCountsAndValues() {
        ClientWorkspaceRegistry<String> registry = testRegistry("registry");
        RegistryMemory memory = new RegistryMemory(List.of(registry));

        ElementEntry<String> first = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "first"),
            "alpha",
            Set.of(),
            CustomFields.EMPTY
        );
        ElementEntry<String> second = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "second"),
            "beta",
            Set.of(),
            CustomFields.EMPTY
        );

        memory.put(registry, first.id(), first);
        assertEquals(first, memory.get(registry, first.id()));
        assertEquals(1, memory.entryCountsSnapshot().get("test:registry"));

        memory.replaceAll(registry, List.of(second));
        assertNull(memory.get(registry, first.id()));
        assertEquals(second, memory.get(registry, second.id()));
        assertEquals(1, memory.allTypedElements(registry).size());

        memory.clearAll();
        assertEquals(0, memory.entryCountsSnapshot().size());
        assertEquals(List.of(), memory.allTypedElements(registry));
    }

    @Test
    void registryObserversOnlyReceiveUpdatesForTheirRegistryUntilClearAll() {
        ClientWorkspaceRegistry<String> firstRegistry = testRegistry("first_registry");
        ClientWorkspaceRegistry<String> secondRegistry = testRegistry("second_registry");
        RegistryMemory memory = new RegistryMemory(List.of(firstRegistry, secondRegistry));

        ElementEntry<String> first = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "first"),
            "alpha",
            Set.of(),
            CustomFields.EMPTY
        );
        ElementEntry<String> second = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "second"),
            "beta",
            Set.of(),
            CustomFields.EMPTY
        );

        AtomicInteger firstNotifications = new AtomicInteger();
        AtomicInteger secondNotifications = new AtomicInteger();
        memory.observeTypedRegistry(firstRegistry).subscribe(firstNotifications::incrementAndGet);
        memory.observeTypedRegistry(secondRegistry).subscribe(secondNotifications::incrementAndGet);

        memory.put(firstRegistry, first.id(), first);
        assertEquals(1, firstNotifications.get());
        assertEquals(0, secondNotifications.get());

        memory.put(secondRegistry, second.id(), second);
        assertEquals(1, firstNotifications.get());
        assertEquals(1, secondNotifications.get());

        memory.replaceAll(firstRegistry, List.of(first));
        assertEquals(1, firstNotifications.get());
        assertEquals(1, secondNotifications.get());

        memory.clearAll();
        assertEquals(2, firstNotifications.get());
        assertEquals(2, secondNotifications.get());
        assertEquals(Map.of(), memory.typedRegistrySnapshot(firstRegistry));
        assertEquals(Map.of(), memory.typedRegistrySnapshot(secondRegistry));
    }

    @Test
    void duplicateRegistryHandlesAreRejected() {
        ClientWorkspaceRegistry<String> first = testRegistry("duplicate");
        ClientWorkspaceRegistry<String> second = testRegistry("duplicate");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new RegistryMemory(List.of(first, second)));
        assertEquals("Duplicate client workspace registry: test:duplicate", exception.getMessage());
    }

    @Test
    void snapshotFromRegistryInitializesCustomFields() {
        ResourceKey<Registry<String>> registryKey = registryKey("snapshot_registry");
        WorkspaceDefinition<String> definition = WorkspaceDefinition.of(
            registryKey,
            Codec.STRING,
            FlushAdapter.identity(),
            entry -> CustomFields.EMPTY.with("mode", "workspace"));
        ClientWorkspaceRegistry<String> workspace = ClientWorkspaceRegistry.of(definition);
        RegistryMemory memory = new RegistryMemory(List.of(workspace));
        Registry<String> registry = new MappedRegistry<>(registryKey, Lifecycle.stable());
        Identifier elementId = Identifier.fromNamespaceAndPath("test", "entry");

        Registry.register(registry, elementId, "alpha");
        registry.freeze();
        memory.snapshotFromRegistry(workspace, registry);

        ElementEntry<String> entry = memory.get(workspace, elementId);
        assertEquals("alpha", entry.data());
        assertEquals("workspace", entry.custom().getString("mode", ""));
    }

    private static ClientWorkspaceRegistry<String> testRegistry(String path) {
        return ClientWorkspaceRegistry.of(WorkspaceDefinition.of(registryKey(path), Codec.STRING, FlushAdapter.identity()));
    }

    private static ResourceKey<Registry<String>> registryKey(String path) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("test", path));
    }
}
