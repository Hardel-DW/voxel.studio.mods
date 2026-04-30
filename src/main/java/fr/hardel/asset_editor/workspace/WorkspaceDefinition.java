package fr.hardel.asset_editor.workspace;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.Action;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.SetEntryDataAction;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class WorkspaceDefinition<T> {

    private final ResourceKey<Registry<T>> registryKey;
    private final Codec<T> codec;
    private final FlushAdapter<T> flushAdapter;
    private final Function<ElementEntry<T>, CustomFields> customInitializer;
    private final Map<Class<?>, Action<T, ?>> actions = new HashMap<>();

    private Map<Identifier, ElementEntry<T>> baseline = Map.of();
    private final Map<String, WorkspaceRegistry<T>> workspaces = new HashMap<>();

    private WorkspaceDefinition(ResourceKey<Registry<T>> registryKey, Codec<T> codec,
        FlushAdapter<T> flushAdapter, Function<ElementEntry<T>, CustomFields> customInitializer) {
        this.registryKey = registryKey;
        this.codec = codec;
        this.flushAdapter = flushAdapter;
        this.customInitializer = customInitializer;
    }

    public ResourceKey<Registry<T>> registryKey() {
        return registryKey;
    }

    public Codec<T> codec() {
        return codec;
    }

    public FlushAdapter<T> flushAdapter() {
        return flushAdapter;
    }

    public Identifier registryId() {
        return registryKey.identifier();
    }

    public ElementEntry<T> initializeEntry(ElementEntry<T> entry) {
        return entry.withCustom(customInitializer.apply(entry));
    }

    public <A extends EditorAction<T>> void registerAction(Action<T, A> action) {
        Action<T, ?> previous = actions.putIfAbsent(action.actionClass(), action);
        if (previous != null)
            throw new IllegalStateException("Duplicate action class for workspace " + registryId() + ": " + action.actionClass().getName());
    }

    public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction<?> action, RegistryMutationContext ctx) {
        if (action instanceof SetEntryDataAction set)
            return applyRawJson(entry, set.json(), ctx);

        Action<T, ?> definition = actions.get(action.getClass());
        if (definition == null)
            throw new IllegalArgumentException("Action " + action.getClass().getName() + " is not registered for workspace " + registryId());

        return definition.apply(entry, action, ctx);
    }

    private ElementEntry<T> applyRawJson(ElementEntry<T> entry, String jsonString, RegistryMutationContext ctx) {
        JsonElement json = JsonParser.parseString(jsonString);
        var ops = ctx.registries().createSerializationContext(JsonOps.INSTANCE);
        T data = codec.parse(ops, json)
            .getOrThrow(message -> new IllegalArgumentException("Invalid JSON for " + entry.id() + ": " + message));
        return entry.withData(data);
    }

    public WorkspaceElementSnapshot toSnapshot(ElementEntry<T> entry, HolderLookup.Provider registries) {
        return new WorkspaceElementSnapshot(registryKey.identifier(), entry.id(), encode(entry, registries), entry.tags(), entry.custom());
    }

    public ElementEntry<T> fromSnapshot(WorkspaceElementSnapshot snapshot, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonElement json = JsonParser.parseString(snapshot.dataJson());
        T data = codec.parse(ops, json).getOrThrow(message -> new IllegalArgumentException("Failed to decode workspace snapshot for " + snapshot.targetId() + ": " + message));
        return new ElementEntry<>(snapshot.targetId(), data, snapshot.tags(), snapshot.custom());
    }

    public String encode(ElementEntry<T> entry, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        return codec.encodeStart(ops, entry.data())
            .result()
            .map(JsonElement::toString)
            .orElseThrow(() -> new IllegalArgumentException("Failed to encode workspace snapshot for " + entry.id()));
    }

    public void setBaseline(Map<Identifier, ElementEntry<T>> entries) {
        this.baseline = Map.copyOf(entries);
        this.workspaces.clear();
    }

    public Map<Identifier, ElementEntry<T>> baseline() {
        return baseline;
    }

    public WorkspaceRegistry<T> workspaceOrLoad(String packId, java.util.function.Supplier<WorkspaceRegistry<T>> loader) {
        return workspaces.computeIfAbsent(packId, ignored -> loader.get());
    }

    void clearState() {
        baseline = Map.of();
        workspaces.clear();
    }

    public static <T> WorkspaceDefinition<T> of(ResourceKey<Registry<T>> registryKey, Codec<T> codec, FlushAdapter<T> flushAdapter) {
        return new WorkspaceDefinition<>(registryKey, codec, flushAdapter, entry -> CustomFields.EMPTY);
    }

    public static <T> WorkspaceDefinition<T> of(ResourceKey<Registry<T>> registryKey, Codec<T> codec,
        FlushAdapter<T> flushAdapter, Function<ElementEntry<T>, CustomFields> customInitializer) {
        return new WorkspaceDefinition<>(registryKey, codec, flushAdapter, customInitializer);
    }
}
