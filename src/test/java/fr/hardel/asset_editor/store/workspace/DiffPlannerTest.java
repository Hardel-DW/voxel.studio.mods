package fr.hardel.asset_editor.store.workspace;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.adapter.FlushAdapter;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffPlannerTest {

    @Test
    void revertingAnAddedTagDeletesTheExistingTagFile() {
        Identifier entryId = Identifier.fromNamespaceAndPath("minecraft", "aqua_affinity");
        Identifier curseTagId = Identifier.fromNamespaceAndPath("minecraft", "curse");

        ElementEntry<String> referenceEntry = new ElementEntry<>(
            entryId,
            "aqua_affinity",
            Set.of(),
            CustomFields.EMPTY
        );
        ElementEntry<String> addedTagEntry = referenceEntry.toggleTag(curseTagId);

        RegistryWorkspace<String> workspace = new RegistryWorkspace<>(
            Map.of(entryId, referenceEntry),
            Map.of(entryId, referenceEntry)
        );

        RegistryWorkspaceBinding<String> binding = new RegistryWorkspaceBinding<>(
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("minecraft", "enchantment")),
            Codec.STRING,
            FlushAdapter.identity(),
            entry -> CustomFields.EMPTY
        );

        DiffPlanner planner = new DiffPlanner();
        Path packRoot = Path.of("test-pack");
        Path cursePath = packRoot.resolve("data").resolve("minecraft")
            .resolve("tags").resolve("enchantment").resolve("curse.json");

        workspace.put(entryId, addedTagEntry);
        RegistryDiffPlan<String> addPlan = planner.plan(packRoot, binding, workspace);
        assertEquals(1, addPlan.tagWrites().size());
        assertEquals(cursePath, addPlan.tagWrites().getFirst().path());

        workspace.clearDirty();
        workspace.put(entryId, referenceEntry);

        RegistryDiffPlan<String> revertPlan = planner.plan(packRoot, binding, workspace);
        assertTrue(revertPlan.tagWrites().isEmpty());
        assertEquals(Set.of(cursePath), Set.copyOf(revertPlan.tagDeletes()));
    }
}
