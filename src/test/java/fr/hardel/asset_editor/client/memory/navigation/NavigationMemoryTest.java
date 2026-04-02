package fr.hardel.asset_editor.client.memory.navigation;

import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination;
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination;
import fr.hardel.asset_editor.client.navigation.StudioEditorTab;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationMemoryTest {

    @Test
    void revalidateClearsTabsWhenCurrentDestinationBecomesForbidden() {
        NavigationMemory memory = new NavigationMemory(() -> StudioPermissions.ADMIN);
        ElementEditorDestination destination = new ElementEditorDestination(
            new StudioConcept(Identifier.fromNamespaceAndPath("asset_editor", "enchantment")),
            "minecraft:sharpness",
            new StudioEditorTab(Identifier.fromNamespaceAndPath("asset_editor", "main"))
        );

        memory.openElement(destination);
        memory.revalidate(StudioPermissions.NONE);

        assertEquals(NoPermissionDestination.INSTANCE, memory.snapshot().current());
        assertTrue(memory.snapshot().tabs().isEmpty());
        assertNull(memory.snapshot().activeTabId());
    }
}
