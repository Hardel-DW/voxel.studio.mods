package fr.hardel.asset_editor.workspace.action.recipe;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;

import java.util.Map;
import java.util.Optional;

public final class DataComponentPatches {

    public static <T> DataComponentPatch set(DataComponentPatch old, DataComponentType<T> type, T value) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : old.entrySet()) {
            if (entry.getKey() == type) continue;
            copyEntry(builder, entry);
        }
        builder.set(type, value);
        return builder.build();
    }

    public static DataComponentPatch forget(DataComponentPatch old, DataComponentType<?> type) {
        return old.forget(t -> t == type);
    }

    private static void copyEntry(DataComponentPatch.Builder builder, Map.Entry<DataComponentType<?>, Optional<?>> entry) {
        Optional<?> value = entry.getValue();
        if (value.isPresent()) {
            builder.set(TypedDataComponent.createUnchecked(entry.getKey(), value.get()));
        } else {
            builder.remove(entry.getKey());
        }
    }

    private DataComponentPatches() {}
}
