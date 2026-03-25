package fr.hardel.asset_editor.client.memory.workspace;

import fr.hardel.asset_editor.client.memory.session.SessionMemory;
import fr.hardel.asset_editor.client.memory.ClientPackInfo;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkspaceMemoryTest {

    @Test
    void tracksPendingActionsAndResetsWorldScopedState() {
        SessionMemory sessionMemory = new SessionMemory();
        AtomicReference<String> preferredPackId = new AtomicReference<>();
        WorkspaceMemory memory = new WorkspaceMemory(sessionMemory, preferredPackId::get, preferredPackId::set);
        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("test", "registry")
        );
        ElementEntry<String> entry = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "item"),
            "value",
            Set.of(),
            CustomFields.EMPTY
        );

        memory.setWorldSessionKey("world-a");
        memory.packSelection().selectPack(new ClientPackInfo("file/pack", "Pack", true, List.of("test")));
        memory.registries().put(registryKey, entry.id(), entry);
        memory.issues().pushError("error:test");
        memory.trackPendingAction(
            UUID.randomUUID(),
            new PendingClientAction<>(UUID.randomUUID(), "file/pack", registryKey, entry.id(), entry)
        );

        assertEquals(1, memory.snapshot().pendingActionCount());

        memory.resetForWorldSync();
        assertEquals(0, memory.snapshot().pendingActionCount());
        assertNull(memory.packSelection().selectedPack());
        assertEquals(List.of(), memory.issues().errors());
        assertEquals(List.of(), memory.registries().allTypedElements(registryKey));

        memory.setWorldSessionKey("world-b");
        memory.resetForWorldClose();
        assertEquals("", memory.worldSessionKey());
        assertEquals(0, memory.snapshot().pendingActionCount());
    }
}
