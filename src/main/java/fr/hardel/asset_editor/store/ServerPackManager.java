package fr.hardel.asset_editor.store;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class ServerPackManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPackManager.class);

    public record PackEntry(String packId, String name, boolean writable, List<String> namespaces) {}

    private static ServerPackManager instance;

    private final MinecraftServer server;

    private ServerPackManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new ServerPackManager(server);
    }

    public static void shutdown() {
        instance = null;
    }

    public static ServerPackManager get() {
        return instance;
    }

    public List<PackEntry> listPacks() {
        var repo = server.getPackRepository();
        repo.reload();
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        List<PackEntry> result = new ArrayList<>();
        for (Pack pack : repo.getAvailablePacks()) {
            PackEntry entry = toPackEntry(pack, datapackDir);
            if (entry != null) result.add(entry);
        }
        return result;
    }

    public PackEntry createPack(String name, String namespace) {
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        String safeName = name == null ? "" : name.trim();
        String safeNamespace = namespace == null ? "" : namespace.trim();
        if (safeName.isBlank() || safeName.contains("/") || safeName.contains("\\") || safeName.contains(".."))
            return null;
        if (!Identifier.isValidNamespace(safeNamespace))
            return null;

        Path packRoot = datapackDir.resolve(safeName).normalize();
        if (!packRoot.startsWith(datapackDir.toAbsolutePath().normalize()))
            return null;

        try {
            Files.createDirectories(packRoot.resolve("data").resolve(safeNamespace));
            writePackMcmeta(packRoot, safeName);
        } catch (IOException e) {
            LOGGER.warn("Failed to create pack: {}", e.getMessage());
            return null;
        }

        try {
            var repo = server.getPackRepository();
            repo.reload();
            var selected = new LinkedHashSet<>(repo.getSelectedIds());
            selected.add("file/" + safeName);
            repo.setSelected(selected);
        } catch (Exception e) {
            LOGGER.warn("Failed to register pack: {}", e.getMessage());
        }

        return new PackEntry("file/" + safeName, safeName, true, List.of(safeNamespace));
    }

    public Optional<Path> resolveWritablePack(String packId) {
        if (packId == null || packId.isBlank()) return Optional.empty();

        var repo = server.getPackRepository();
        Pack pack = repo.getPack(packId);
        if (pack == null || pack.getPackSource() != PackSource.WORLD) return Optional.empty();

        String name = stripPrefix(packId);
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
        Path resolved = datapackDir.resolve(name).normalize();
        if (!resolved.startsWith(datapackDir)) return Optional.empty();
        if (!Files.isDirectory(resolved)) return Optional.empty();

        return Optional.of(resolved);
    }

    public void ensureNamespace(String packId, String namespace) {
        if (!Identifier.isValidNamespace(namespace)) return;
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        String name = stripPrefix(packId);
        Path nsDir = datapackDir.resolve(name).resolve("data").resolve(namespace);
        if (Files.exists(nsDir)) return;
        try {
            Files.createDirectories(nsDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create namespace dir: {}", e.getMessage());
        }
    }

    private PackEntry toPackEntry(Pack pack, Path datapackDir) {
        if (pack.getPackSource() != PackSource.WORLD) return null;
        String id = pack.getId();

        try (PackResources resources = pack.open()) {
            if (resources instanceof PathPackResources) {
                Path rootPath = datapackDir.resolve(stripPrefix(id)).normalize();
                return new PackEntry(id, pack.getTitle().getString(), true, scanNamespaces(rootPath));
            }
            return new PackEntry(id, pack.getTitle().getString(), false, List.of());
        }
    }

    private static String stripPrefix(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }

    private static List<String> scanNamespaces(Path packRoot) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir)) return List.of();
        try (Stream<Path> dirs = Files.list(dataDir)) {
            return dirs.filter(Files::isDirectory).map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static void writePackMcmeta(Path packRoot, String name) throws IOException {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minorRange();
        var section = new PackMetadataSection(Component.literal(name), format);
        var json = PackMetadataSection.SERVER_TYPE.codec()
                .encodeStart(JsonOps.INSTANCE, section)
                .getOrThrow(msg -> new IOException("Failed to encode pack.mcmeta: " + msg));
        var meta = new JsonObject();
        meta.add("pack", json);
        Files.writeString(packRoot.resolve("pack.mcmeta"),
                new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(meta));
    }
}
