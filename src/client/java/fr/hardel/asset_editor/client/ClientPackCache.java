package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;

import java.util.List;

public final class ClientPackCache {

    private static List<PackEntry> packs = List.of();
    private static boolean received;
    private static Runnable onChange;

    public static List<PackEntry> get() {
        return packs;
    }

    public static boolean hasReceived() {
        return received;
    }

    public static void setOnChange(Runnable listener) {
        onChange = listener;
    }

    public static void update(List<PackEntry> newPacks) {
        packs = List.copyOf(newPacks);
        received = true;
        if (onChange != null) onChange.run();
    }

    public static void reset() {
        packs = List.of();
        received = false;
    }

    private ClientPackCache() {
    }
}
