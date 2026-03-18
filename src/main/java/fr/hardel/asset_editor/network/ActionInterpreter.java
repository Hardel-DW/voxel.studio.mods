package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class ActionInterpreter {

    private static final Map<String, RegistryInterpreter<?>> INTERPRETERS = Map.of(
        "enchantment", new EnchantmentInterpreter());

    @SuppressWarnings("unchecked")
    public static <T> ElementEntry<T> apply(Identifier registryId, ElementEntry<T> entry,
        EditorAction action, HolderLookup.Provider registries) {
        if (action instanceof EditorAction.ToggleTag toggle)
            return entry.toggleTag(toggle.tagId());

        var interpreter = (RegistryInterpreter<T>) INTERPRETERS.get(registryId.getPath());
        if (interpreter == null)
            return entry;
        return interpreter.apply(entry, action, registries);
    }

    private ActionInterpreter() {}
}
