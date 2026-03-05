package fr.hardel.asset_editor.client.javafx.lib.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class StudioPackState {

    public enum PackKind { DIRECTORY, ZIP, JAR, BUILTIN }

    public record PackInfo(String name, Path rootPath, boolean writable, PackKind kind, List<String> namespaces) {}

    private final ObservableList<PackInfo> availablePacks = FXCollections.observableArrayList();
    private final ObjectProperty<PackInfo> selectedPack = new SimpleObjectProperty<>(null);
    private final StringProperty selectedNamespace = new SimpleStringProperty(null);

    public ObservableList<PackInfo> availablePacks() {
        return availablePacks;
    }

    public ObjectProperty<PackInfo> selectedPackProperty() {
        return selectedPack;
    }

    public StringProperty selectedNamespaceProperty() {
        return selectedNamespace;
    }

    public PackInfo selectedPack() {
        return selectedPack.get();
    }

    public String selectedNamespace() {
        return selectedNamespace.get();
    }

    public boolean hasSelectedPack() {
        return selectedPack.get() != null;
    }

    public void selectPack(PackInfo pack) {
        selectedPack.set(pack);
        if (pack != null && !pack.namespaces().isEmpty()) {
            selectedNamespace.set(pack.namespaces().getFirst());
        } else {
            selectedNamespace.set(null);
        }
    }

    public void selectNamespace(String namespace) {
        selectedNamespace.set(namespace);
    }

    public void refreshFromServer() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;

        PackRepository repo = server.getPackRepository();
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);

        availablePacks.clear();
        for (Pack pack : repo.getSelectedPacks()) {
            PackInfo info = toPackInfo(pack, datapackDir);
            if (info != null) availablePacks.add(info);
        }
    }

    public PackInfo createPack(String name, String namespace) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return null;

        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        Path packRoot = datapackDir.resolve(name);

        try {
            Files.createDirectories(packRoot.resolve("data").resolve(namespace));
            writePackMcmeta(packRoot, name);
        } catch (IOException e) {
            System.err.println("Failed to create pack: " + e.getMessage());
            return null;
        }

        PackInfo info = new PackInfo(name, packRoot, true, PackKind.DIRECTORY, new ArrayList<>(List.of(namespace)));
        availablePacks.add(info);
        selectPack(info);
        return info;
    }

    public void ensureNamespace(String namespace) {
        PackInfo pack = selectedPack.get();
        if (pack == null || !pack.writable()) return;

        Path nsDir = pack.rootPath().resolve("data").resolve(namespace);
        if (Files.exists(nsDir)) return;

        try {
            Files.createDirectories(nsDir);
            var updatedNs = new ArrayList<>(pack.namespaces());
            if (!updatedNs.contains(namespace)) {
                updatedNs.add(namespace);
                PackInfo updated = new PackInfo(pack.name(), pack.rootPath(), true, pack.kind(), updatedNs);
                int idx = availablePacks.indexOf(pack);
                if (idx >= 0) availablePacks.set(idx, updated);
                selectedPack.set(updated);
            }
        } catch (IOException e) {
            System.err.println("Failed to create namespace dir: " + e.getMessage());
        }
    }

    private PackInfo toPackInfo(Pack pack, Path datapackDir) {
        PackSource source = pack.getPackSource();
        String id = pack.getId();

        if (source == PackSource.BUILT_IN) {
            return null;
        }

        try (PackResources resources = pack.open()) {
            if (resources instanceof PathPackResources) {
                Path rootPath = datapackDir.resolve(id.replaceFirst("^file/", ""));
                List<String> namespaces = scanNamespaces(rootPath);
                return new PackInfo(
                        pack.getTitle().getString(),
                        rootPath, true, PackKind.DIRECTORY,
                        namespaces
                );
            }

            String fileName = id.replaceFirst("^file/", "");
            PackKind kind = fileName.endsWith(".jar") ? PackKind.JAR : PackKind.ZIP;
            return new PackInfo(pack.getTitle().getString(), datapackDir.resolve(fileName), false, kind, List.of());
        }
    }

    private List<String> scanNamespaces(Path packRoot) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir)) return List.of();

        try (Stream<Path> dirs = Files.list(dataDir)) {
            return dirs
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void writePackMcmeta(Path packRoot, String name) throws IOException {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minorRange();
        var section = new PackMetadataSection(Component.literal(name), format);
        var json = PackMetadataSection.SERVER_TYPE.codec()
                .encodeStart(JsonOps.INSTANCE, section)
                .getOrThrow(msg -> new IOException("Failed to encode pack.mcmeta: " + msg));

        var meta = new JsonObject();
        meta.add("pack", json);

        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Files.writeString(packRoot.resolve("pack.mcmeta"), gson.toJson(meta));
    }
}
