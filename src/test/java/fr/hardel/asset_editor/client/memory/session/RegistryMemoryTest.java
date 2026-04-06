package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.session.server.RegistryMemory;
import fr.hardel.asset_editor.workspace.CustomFields;
import fr.hardel.asset_editor.workspace.ElementEntry;
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

class RegistryMemoryTest {

    @Test
    void putReplaceAllAndClearMaintainCountsAndValues() {
        RegistryMemory memory = new RegistryMemory();
        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("test", "registry")
        );

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

        memory.put(registryKey, first.id(), first);
        assertEquals(first, memory.get(registryKey, first.id()));
        assertEquals(1, memory.entryCountsSnapshot().get("test:registry"));

        memory.replaceAll(registryKey, List.of(second));
        assertNull(memory.get(registryKey, first.id()));
        assertEquals(second, memory.get(registryKey, second.id()));
        assertEquals(1, memory.allTypedElements(registryKey).size());

        memory.clearAll();
        assertEquals(0, memory.entryCountsSnapshot().size());
        assertEquals(List.of(), memory.allTypedElements(registryKey));
    }

    @Test
    void registryObserversOnlyReceiveUpdatesForTheirRegistryUntilClearAll() {
        RegistryMemory memory = new RegistryMemory();
        ResourceKey<Registry<String>> firstRegistry = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("test", "first_registry")
        );
        ResourceKey<Registry<String>> secondRegistry = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("test", "second_registry")
        );

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
}
