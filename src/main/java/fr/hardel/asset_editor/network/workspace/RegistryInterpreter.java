package fr.hardel.asset_editor.network.workspace;

import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.HolderLookup;

public interface RegistryInterpreter<T> {
    ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, HolderLookup.Provider registries);
}
