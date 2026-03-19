package fr.hardel.asset_editor.store.workspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiskWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskWriter.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public <T> void write(RegistryDiffPlan<T> plan, Codec<T> codec, DynamicOps<JsonElement> ops) {
        for (Path path : plan.elementDeletes()) {
            deleteIfExists(path, "element");
        }

        for (RegistryDiffPlan.ElementWrite<T> write : plan.elementWrites()) {
            codec.encodeStart(ops, write.data())
                .ifSuccess(json -> writeFile(write.path(), GSON.toJson(json)))
                .ifError(error -> LOGGER.warn("Failed to encode element {}: {}", write.path(), error.message()));
        }

        for (Path path : plan.tagDeletes()) {
            deleteIfExists(path, "tag");
        }

        for (RegistryDiffPlan.TagWrite write : plan.tagWrites()) {
            ExtendedTagFile.CODEC.encodeStart(JsonOps.INSTANCE, write.file())
                .ifSuccess(json -> writeFile(write.path(), GSON.toJson(json)))
                .ifError(error -> LOGGER.warn("Failed to encode tag {}: {}", write.path(), error.message()));
        }
    }

    private void deleteIfExists(Path path, String type) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("Failed to delete {} file {}: {}", type, path, exception.getMessage());
        }
    }

    private void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException exception) {
            LOGGER.warn("Failed to write {}: {}", path, exception.getMessage());
        }
    }
}
