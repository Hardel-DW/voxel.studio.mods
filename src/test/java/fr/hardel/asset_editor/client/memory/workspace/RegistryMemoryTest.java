package fr.hardel.asset_editor.client.memory.workspace;

import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
}
