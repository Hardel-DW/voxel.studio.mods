package fr.hardel.asset_editor.network.data;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerDataKeys {

    private static final Map<Identifier, ServerDataKey<?>> KEYS = new LinkedHashMap<>();

    private ServerDataKeys() {}

    public static <T> ServerDataKey<T> register(ServerDataKey<T> key) {
        if (KEYS.containsKey(key.id()))
            throw new IllegalStateException("Duplicate server data key: " + key.id());

        KEYS.put(key.id(), key);
        return key;
    }

    public static ServerDataKey<?> get(Identifier id) {
        ServerDataKey<?> key = KEYS.get(id);
        if (key == null)
            throw new IllegalArgumentException("Unknown server data key: " + id);

        return key;
    }

    public static Collection<ServerDataKey<?>> all() {
        return List.copyOf(KEYS.values());
    }
}
