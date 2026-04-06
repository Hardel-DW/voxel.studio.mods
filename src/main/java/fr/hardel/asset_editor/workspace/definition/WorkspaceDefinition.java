package fr.hardel.asset_editor.workspace.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.workspace.CustomFields;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryWorkspace;
import fr.hardel.asset_editor.workspace.action.ActionHandler;
import fr.hardel.asset_editor.workspace.action.EditorActionRegistry;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class WorkspaceDefinition<T> {

    private static final Map<String, WorkspaceDefinition<?>> DEFINITIONS = new ConcurrentHashMap<>();

    private final ResourceKey<Registry<T>> registryKey;
    private final Codec<T> codec;
    private final FlushAdapter<T> flushAdapter;
    private final Function<ElementEntry<T>, CustomFields> customInitializer;
    private final Map<String, BoundHandler<T>> handlers;
    private final List<EditorActionType<?>> actionTypes;

    private Map<Identifier, ElementEntry<T>> baseline = Map.of();
    private final Map<String, RegistryWorkspace<T>> workspaces = new HashMap<>();

    private WorkspaceDefinition(Builder<T> builder) {
        this.registryKey = builder.registryKey;
        this.codec = builder.codec;
        this.flushAdapter = builder.flushAdapter;
        this.customInitializer = builder.customInitializer;
        this.handlers = Map.copyOf(builder.handlers);
        this.actionTypes = List.copyOf(builder.actionTypes);
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

    List<EditorActionType<?>> actionTypes() {
        return actionTypes;
    }

    public ElementEntry<T> initializeEntry(ElementEntry<T> entry) {
        return entry.withCustom(customInitializer.apply(entry));
    }

    public void beforeApply(EditorAction action, RegistryMutationContext context) {
        BoundHandler<T> handler = handlers.get(action.typeId().toString());
        if (handler != null) {
            handler.beforeApply(action, context);
        }
    }

    public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context) {
        BoundHandler<T> handler = handlers.get(action.typeId().toString());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for action: " + action.typeId());
        }
        return handler.apply(entry, action, context);
    }

    public WorkspaceElementSnapshot toSnapshot(ElementEntry<T> entry, HolderLookup.Provider registries) {
        return new WorkspaceElementSnapshot(registryKey.identifier(), entry.id(), encode(entry, registries), entry.tags(), entry.custom());
    }

    public ElementEntry<T> fromSnapshot(WorkspaceElementSnapshot snapshot, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        JsonElement json = JsonParser.parseString(snapshot.dataJson());
        T data = codec.parse(ops, json).getOrThrow(message ->
            new IllegalArgumentException("Failed to decode workspace snapshot for " + snapshot.targetId() + ": " + message));
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

    public RegistryWorkspace<T> workspaceOrLoad(String packId, java.util.function.Supplier<RegistryWorkspace<T>> loader) {
        return workspaces.computeIfAbsent(packId, ignored -> loader.get());
    }

    private void clearState() {
        baseline = Map.of();
        workspaces.clear();
    }

    public static void clearAllState() {
        DEFINITIONS.values().forEach(WorkspaceDefinition::clearState);
    }

    public static <T> Builder<T> builder(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
        return new Builder<>(registryKey, codec);
    }

    public static final class Builder<T> {

        private final ResourceKey<Registry<T>> registryKey;
        private final Codec<T> codec;
        private FlushAdapter<T> flushAdapter = FlushAdapter.identity();
        private Function<ElementEntry<T>, CustomFields> customInitializer = entry -> CustomFields.EMPTY;
        private final Map<String, BoundHandler<T>> handlers = new LinkedHashMap<>();
        private final List<EditorActionType<?>> actionTypes = new ArrayList<>();

        private Builder(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
            this.registryKey = registryKey;
            this.codec = codec;
        }

        public Builder<T> flushAdapter(FlushAdapter<T> flushAdapter) {
            this.flushAdapter = flushAdapter;
            return this;
        }

        public Builder<T> customInitializer(Function<ElementEntry<T>, CustomFields> customInitializer) {
            this.customInitializer = customInitializer;
            return this;
        }

        public <A extends EditorAction> Builder<T> action(EditorActionType<A> type, ActionHandler<T, A> handler) {
            actionTypes.add(type);
            handlers.put(type.id().toString(), new TypedHandler<>(type, handler, null));
            return this;
        }

        public <A extends EditorAction> Builder<T> actionWithHook(EditorActionType<A> type,
            ActionHandler<T, A> handler, PreApplyHook<A> hook) {
            actionTypes.add(type);
            handlers.put(type.id().toString(), new TypedHandler<>(type, handler, hook));
            return this;
        }

        public WorkspaceDefinition<T> build() {
            return new WorkspaceDefinition<>(this);
        }
    }

    private interface BoundHandler<T> {
        void beforeApply(EditorAction action, RegistryMutationContext context);
        ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context);
    }

    @FunctionalInterface
    public interface PreApplyHook<A extends EditorAction> {
        void beforeApply(A action, RegistryMutationContext context);
    }

    private record TypedHandler<T, A extends EditorAction>(
        EditorActionType<A> type,
        ActionHandler<T, A> handler,
        PreApplyHook<A> hook
    ) implements BoundHandler<T> {

        @Override
        public void beforeApply(EditorAction action, RegistryMutationContext context) {
            if (hook != null) {
                hook.beforeApply(type.cast(action), context);
            }
        }

        @Override
        public ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, RegistryMutationContext context) {
            return handler.apply(entry, type.cast(action), context);
        }
    }

    public static <T> void register(WorkspaceDefinition<T> definition) {
        if (definition == null)
            throw new IllegalArgumentException("Workspace definition cannot be null");

        WorkspaceDefinition<?> previous = DEFINITIONS.putIfAbsent(definition.registryId().toString(), definition);
        if (previous != null)
            throw new IllegalStateException("Duplicate workspace definition: " + definition.registryId());

        for (var actionType : definition.actionTypes()) {
            EditorActionRegistry.register(actionType);
        }
    }

    public static WorkspaceDefinition<?> get(Identifier registryId) {
        if (registryId == null)
            return null;
        return DEFINITIONS.get(registryId.toString());
    }

    public static Collection<WorkspaceDefinition<?>> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }
}
