package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import net.minecraft.core.HolderLookup;

public interface RegistryInterpreter<T> {
    ElementEntry<T> apply(ElementEntry<T> entry, EditorAction action, HolderLookup.Provider registries);
}
