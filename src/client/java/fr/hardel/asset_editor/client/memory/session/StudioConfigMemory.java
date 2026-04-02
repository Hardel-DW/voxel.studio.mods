package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.studio.CompendiumTagEntry;
import fr.hardel.asset_editor.studio.CompendiumTagGroup;
import fr.hardel.asset_editor.studio.RecipeEntryDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class StudioConfigMemory implements ReadableMemory<StudioConfigMemory.Snapshot> {

    public record Snapshot(
        List<CompendiumTagGroup> compendiumItemGroups,
        List<CompendiumTagGroup> compendiumEnchantmentGroups,
        List<RecipeEntryDefinition> recipeEntries
    ) {
        public Snapshot {
            compendiumItemGroups = List.copyOf(compendiumItemGroups == null ? List.of() : compendiumItemGroups);
            compendiumEnchantmentGroups = List.copyOf(compendiumEnchantmentGroups == null ? List.of() : compendiumEnchantmentGroups);
            recipeEntries = List.copyOf(recipeEntries == null ? List.of() : recipeEntries);
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), List.of());
        }

        public List<CompendiumTagEntry> itemEntriesFor(Identifier groupId) {
            return entriesFor(compendiumItemGroups, groupId);
        }

        public List<CompendiumTagEntry> enchantmentEntriesFor(Identifier groupId) {
            return entriesFor(compendiumEnchantmentGroups, groupId);
        }

        private static List<CompendiumTagEntry> entriesFor(List<CompendiumTagGroup> groups, Identifier groupId) {
            for (CompendiumTagGroup group : groups) {
                if (group.id().equals(groupId))
                    return group.entries();
            }
            return List.of();
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void updateCompendiumItemGroups(List<CompendiumTagGroup> groups) {
        memory.update(state -> new Snapshot(groups, state.compendiumEnchantmentGroups(), state.recipeEntries()));
    }

    public void updateCompendiumEnchantmentGroups(List<CompendiumTagGroup> groups) {
        memory.update(state -> new Snapshot(state.compendiumItemGroups(), groups, state.recipeEntries()));
    }

    public void updateRecipeEntries(List<RecipeEntryDefinition> entries) {
        memory.update(state -> new Snapshot(state.compendiumItemGroups(), state.compendiumEnchantmentGroups(), entries));
    }

    public void clear() {
        memory.setSnapshot(Snapshot.empty());
    }
}
