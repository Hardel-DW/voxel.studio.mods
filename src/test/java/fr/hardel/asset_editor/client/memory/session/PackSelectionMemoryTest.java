package fr.hardel.asset_editor.client.memory.session;
import fr.hardel.asset_editor.client.memory.session.ui.PackSelectionMemory;
import fr.hardel.asset_editor.workspace.io.DataPackManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PackSelectionMemoryTest {

    @Test
    void syncsSelectionFromPreferredAndPersistsManualSelection() {
        SessionMemory sessionMemory = new SessionMemory();
        AtomicReference<String> preferredPackId = new AtomicReference<>("file/pack_a");
        PackSelectionMemory memory = new PackSelectionMemory(sessionMemory, preferredPackId::get, preferredPackId::set);

        sessionMemory.updatePacks(List.of(
            new DataPackManager.PackEntry("file/pack_a", "Pack A", true, List.of("a")),
            new DataPackManager.PackEntry("file/pack_b", "Pack B", true, List.of("b"))
        ));

        assertNotNull(memory.selectedPack());
        assertEquals("file/pack_a", memory.selectedPack().packId());

        memory.selectPack(sessionMemory.availablePacks().get(1));
        assertEquals("file/pack_b", preferredPackId.get());

        sessionMemory.updatePacks(List.of(
            new DataPackManager.PackEntry("file/pack_b", "Pack B", true, List.of("b"))
        ));

        assertNotNull(memory.selectedPack());
        assertEquals("file/pack_b", memory.selectedPack().packId());
    }
}
