package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.FlushAdapter;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagEntry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DiffPlanner {

    private static final RegistryDiffPlan<?> EMPTY_PLAN = new RegistryDiffPlan<>(List.of(), List.of(), List.of(), List.of());

    public <T> RegistryDiffPlan<T> plan(Path packRoot, RegistryWorkspaceBinding<T> binding, RegistryWorkspace<T> workspace) {
        if (workspace.dirty().isEmpty())
            return emptyPlan();
        return new Planner<>(packRoot, binding, workspace).plan();
    }

    @SuppressWarnings("unchecked")
    private <T> RegistryDiffPlan<T> emptyPlan() {
        return (RegistryDiffPlan<T>) EMPTY_PLAN;
    }

    private static final class Planner<T> {
        private final Path packRoot;
        private final String registryDir;
        private final FlushAdapter<T> adapter;
        private final Map<Identifier, ElementEntry<T>> referenceEntries;
        private final Map<Identifier, ElementEntry<T>> currentEntries;
        private final Set<Identifier> dirty;

        private final ArrayList<RegistryDiffPlan.ElementWrite<T>> elementWrites = new ArrayList<>();
        private final ArrayList<Path> elementDeletes = new ArrayList<>();
        private final ArrayList<RegistryDiffPlan.TagWrite> tagWrites = new ArrayList<>();
        private final ArrayList<Path> tagDeletes = new ArrayList<>();

        private Planner(Path packRoot, RegistryWorkspaceBinding<T> binding, RegistryWorkspace<T> workspace) {
            this.packRoot = packRoot;
            this.registryDir = binding.registryKey().identifier().getPath();
            this.adapter = binding.adapter() == null ? FlushAdapter.identity() : binding.adapter();
            this.referenceEntries = workspace.referenceEntries();
            this.currentEntries = workspace.currentEntries();
            this.dirty = workspace.dirty();
        }

        private RegistryDiffPlan<T> plan() {
            planElementChanges();
            planTagChanges();
            return new RegistryDiffPlan<>(elementWrites, elementDeletes, tagWrites, tagDeletes);
        }

        private void planElementChanges() {
            for (Identifier id : dirty) {
                ElementEntry<T> currentEntry = preparedEntry(currentEntries, id);
                if (currentEntry == null)
                    continue;
                planElementChange(id, currentEntry);
            }
        }

        private void planElementChange(Identifier id, ElementEntry<T> currentEntry) {
            Path filePath = elementPath(id);
            ElementEntry<T> referenceEntry = preparedEntry(referenceEntries, id);
            if (sameData(referenceEntry, currentEntry)) {
                elementDeletes.add(filePath);
                return;
            }
            elementWrites.add(new RegistryDiffPlan.ElementWrite<>(filePath, currentEntry.data()));
        }

        private void planTagChanges() {
            Set<Identifier> affectedTagIds = collectAffectedTags();
            if (affectedTagIds.isEmpty())
                return;

            Map<Identifier, Set<Identifier>> referenceTags = collectTagMemberships(referenceEntries);
            Map<Identifier, Set<Identifier>> currentTags = collectTagMemberships(currentEntries);

            for (Identifier tagId : affectedTagIds)
                planTagChange(tagId, referenceTags, currentTags);
        }

        private void planTagChange(Identifier tagId,
            Map<Identifier, Set<Identifier>> referenceTags,
            Map<Identifier, Set<Identifier>> currentTags) {
            Set<Identifier> referenceMembers = members(referenceTags, tagId);
            Set<Identifier> currentMembers = members(currentTags, tagId);
            Path filePath = tagPath(tagId);

            if (referenceMembers.equals(currentMembers)) {
                tagDeletes.add(filePath);
                return;
            }

            tagWrites.add(new RegistryDiffPlan.TagWrite(filePath, tagDelta(referenceMembers, currentMembers)));
        }

        private Set<Identifier> collectAffectedTags() {
            Set<Identifier> tags = new HashSet<>();
            for (Identifier id : dirty) {
                ElementEntry<T> referenceEntry = preparedEntry(referenceEntries, id);
                ElementEntry<T> currentEntry = preparedEntry(currentEntries, id);
                if (referenceEntry != null)
                    tags.addAll(referenceEntry.tags());
                if (currentEntry != null)
                    tags.addAll(currentEntry.tags());
            }
            return tags;
        }

        private Map<Identifier, Set<Identifier>> collectTagMemberships(Map<Identifier, ElementEntry<T>> entries) {
            Map<Identifier, Set<Identifier>> memberships = new HashMap<>();
            for (ElementEntry<T> entry : entries.values()) {
                ElementEntry<T> prepared = adapter.prepare(entry);
                for (Identifier tagId : prepared.tags())
                    memberships.computeIfAbsent(tagId, ignored -> new HashSet<>()).add(prepared.id());
            }
            return memberships;
        }

        private ElementEntry<T> preparedEntry(Map<Identifier, ElementEntry<T>> entries, Identifier id) {
            ElementEntry<T> entry = entries.get(id);
            return entry == null ? null : adapter.prepare(entry);
        }

        private boolean sameData(ElementEntry<T> referenceEntry, ElementEntry<T> currentEntry) {
            return referenceEntry != null && Objects.equals(referenceEntry.data(), currentEntry.data());
        }

        private Set<Identifier> members(Map<Identifier, Set<Identifier>> memberships, Identifier tagId) {
            return memberships.getOrDefault(tagId, Set.of());
        }

        private ExtendedTagFile tagDelta(Set<Identifier> referenceMembers, Set<Identifier> currentMembers) {
            return new ExtendedTagFile(
                difference(currentMembers, referenceMembers).stream().map(TagEntry::element).toList(),
                difference(referenceMembers, currentMembers).stream().map(TagEntry::element).toList(),
                false);
        }

        private Set<Identifier> difference(Set<Identifier> source, Set<Identifier> removed) {
            Set<Identifier> diff = new HashSet<>(source);
            diff.removeAll(removed);
            return diff;
        }

        private Path elementPath(Identifier id) {
            return packRoot.resolve("data").resolve(id.getNamespace())
                .resolve(registryDir).resolve(id.getPath() + ".json");
        }

        private Path tagPath(Identifier tagId) {
            return packRoot.resolve("data").resolve(tagId.getNamespace())
                .resolve("tags").resolve(registryDir).resolve(tagId.getPath() + ".json");
        }
    }
}
