package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.studio.RecipeEntryDefinition;
import fr.hardel.asset_editor.studio.SuggestedTagEntry;
import fr.hardel.asset_editor.studio.SuggestedTagGroup;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class StudioConfigMemory implements ReadableMemory<StudioConfigMemory.Snapshot> {

    public record Snapshot(
        List<SuggestedTagGroup> suggestedItemGroups,
        List<SuggestedTagGroup> suggestedEnchantmentGroups,
        List<RecipeEntryDefinition> recipeEntries
    ) {
        public Snapshot {
            suggestedItemGroups = List.copyOf(suggestedItemGroups == null ? List.of() : suggestedItemGroups);
            suggestedEnchantmentGroups = List.copyOf(suggestedEnchantmentGroups == null ? List.of() : suggestedEnchantmentGroups);
            recipeEntries = List.copyOf(recipeEntries == null ? List.of() : recipeEntries);
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), List.of());
        }

        public List<SuggestedTagEntry> itemEntriesFor(Identifier groupId) {
            return entriesFor(suggestedItemGroups, groupId);
        }

        public List<SuggestedTagEntry> enchantmentEntriesFor(Identifier groupId) {
            return entriesFor(suggestedEnchantmentGroups, groupId);
        }

        private static List<SuggestedTagEntry> entriesFor(List<SuggestedTagGroup> groups, Identifier groupId) {
            for (SuggestedTagGroup group : groups) {
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

    public void updateSuggestedItemGroups(List<SuggestedTagGroup> groups) {
        memory.update(state -> new Snapshot(groups, state.suggestedEnchantmentGroups(), state.recipeEntries()));
    }

    public void updateSuggestedEnchantmentGroups(List<SuggestedTagGroup> groups) {
        memory.update(state -> new Snapshot(state.suggestedItemGroups(), groups, state.recipeEntries()));
    }

    public void updateRecipeEntries(List<RecipeEntryDefinition> entries) {
        memory.update(state -> new Snapshot(state.suggestedItemGroups(), state.suggestedEnchantmentGroups(), entries));
    }

    public void clear() {
        memory.setSnapshot(Snapshot.empty());
    }
}
