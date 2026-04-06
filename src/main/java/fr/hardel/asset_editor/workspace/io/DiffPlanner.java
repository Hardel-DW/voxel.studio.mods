package fr.hardel.asset_editor.workspace.io;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryWorkspace;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
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

    public <T> RegistryDiffPlan<T> plan(Path packRoot, WorkspaceDefinition<T> definition, RegistryWorkspace<T> workspace, DynamicOps<JsonElement> ops) {
        if (workspace.dirty().isEmpty())
            return new RegistryDiffPlan<>(List.of(), List.of(), List.of(), List.of());
        return new Planner<>(packRoot, definition, workspace, ops).plan();
    }

    private static final class Planner<T> {
        private final Path packRoot;
        private final String registryDir;
        private final FlushAdapter<T> adapter;
        private final Codec<T> codec;
        private final DynamicOps<JsonElement> ops;
        private final Set<Identifier> dirtyTags;
        private final Map<Identifier, ElementEntry<T>> referenceEntries;
        private final Map<Identifier, ElementEntry<T>> currentEntries;
        private final Set<Identifier> dirty;

        private final ArrayList<RegistryDiffPlan.ElementWrite<T>> elementWrites = new ArrayList<>();
        private final ArrayList<Path> elementDeletes = new ArrayList<>();
        private final ArrayList<RegistryDiffPlan.TagWrite> tagWrites = new ArrayList<>();
        private final ArrayList<Path> tagDeletes = new ArrayList<>();

        private Planner(Path packRoot, WorkspaceDefinition<T> definition, RegistryWorkspace<T> workspace, DynamicOps<JsonElement> ops) {
            this.packRoot = packRoot;
            this.registryDir = definition.registryKey().identifier().getPath();
            this.adapter = definition.flushAdapter();
            this.codec = definition.codec();
            this.ops = ops;
            this.dirtyTags = workspace.dirtyTags();
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
            Set<Identifier> tags = new HashSet<>(dirtyTags);
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
            if (referenceEntry == null)
                return false;
            if (Objects.equals(referenceEntry.data(), currentEntry.data()))
                return true;

            JsonElement referenceJson = encode(referenceEntry.data());
            JsonElement currentJson = encode(currentEntry.data());
            return referenceJson != null && referenceJson.equals(currentJson);
        }

        private JsonElement encode(T value) {
            return codec.encodeStart(ops, value).result().orElse(null);
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
