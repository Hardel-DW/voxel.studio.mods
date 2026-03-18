package fr.hardel.asset_editor.client.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfig.class);

    private final Path file;
    private final Codec<T> codec;
    private T value;

    public ModConfig(Path file, Codec<T> codec, T defaultValue) {
        this.file = file;
        this.codec = codec;
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public void load() {
        if (!Files.exists(file))
            return;
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            codec.decode(JsonOps.INSTANCE, json)
                .ifSuccess(pair -> value = pair.getFirst())
                .ifError(error -> LOGGER.warn("Failed to decode config {}: {}", file.getFileName(), error.message()));
        } catch (Exception e) {
            LOGGER.warn("Failed to load config {}: {}", file.getFileName(), e.getMessage());
        }
    }

    public void save() {
        codec.encodeStart(JsonOps.INSTANCE, value).ifSuccess(json -> {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json));
            } catch (IOException e) {
                LOGGER.warn("Failed to save config {}: {}", file.getFileName(), e.getMessage());
            }
        }).ifError(error -> LOGGER.warn("Failed to encode config {}: {}", file.getFileName(), error.message()));
    }
}
