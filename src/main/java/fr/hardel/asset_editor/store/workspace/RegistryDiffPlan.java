package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.tag.ExtendedTagFile;

import java.nio.file.Path;
import java.util.List;

public record RegistryDiffPlan<T>(
    List<ElementWrite<T>> elementWrites,
    List<Path> elementDeletes,
    List<TagWrite> tagWrites,
    List<Path> tagDeletes) {

    public boolean isEmpty() {
        return elementWrites.isEmpty() && elementDeletes.isEmpty() && tagWrites.isEmpty() && tagDeletes.isEmpty();
    }

    public record ElementWrite<T>(Path path, T data) {}

    public record TagWrite(Path path, ExtendedTagFile file) {}
}
