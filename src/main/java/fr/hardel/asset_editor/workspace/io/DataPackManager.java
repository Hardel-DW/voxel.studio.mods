package fr.hardel.asset_editor.workspace.io;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class DataPackManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataPackManager.class);

    public record PackEntry(String packId, String name, boolean writable, List<String> namespaces, byte[] icon) {
        public PackEntry(String packId, String name, boolean writable, List<String> namespaces) {
            this(packId, name, writable, namespaces, new byte[0]);
        }
    }

    private static final int MAX_PACK_ICON_BYTES = 512 * 1024;

    private static DataPackManager instance;

    private final MinecraftServer server;

    private DataPackManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new DataPackManager(server);
    }

    public static void shutdown() {
        instance = null;
    }

    public static DataPackManager get() {
        return instance;
    }

    public List<PackEntry> listPacks() {
        var repo = server.getPackRepository();
        repo.reload();
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        return repo.getAvailablePacks().stream().map(pack -> toPackEntry(pack, datapackDir)).filter(entry -> entry != null).toList();
    }

    public void createPack(String name, String namespace, byte[] icon) {
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        String safeName = name == null ? "" : name.trim();
        String safeNamespace = namespace == null ? "" : namespace.trim();
        if (safeName.isBlank() || safeName.contains("/") || safeName.contains("\\") || safeName.contains(".."))
            return;

        if (!Identifier.isValidNamespace(safeNamespace))
            return;

        Path packRoot = datapackDir.resolve(safeName).normalize();
        if (!packRoot.startsWith(datapackDir.toAbsolutePath().normalize()))
            return;

        try {
            Files.createDirectories(packRoot.resolve("data").resolve(safeNamespace));
            writePackMcmeta(packRoot, safeName);
            writePackIcon(packRoot, icon);
        } catch (IOException e) {
            LOGGER.warn("Failed to create pack: {}", e.getMessage());
            return;
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
    }

    public Optional<Path> resolveWritablePack(String packId) {
        if (packId == null || packId.isBlank())
            return Optional.empty();

        var repo = server.getPackRepository();
        Pack pack = repo.getPack(packId);
        if (pack == null || pack.getPackSource() != PackSource.WORLD)
            return Optional.empty();

        String name = stripPrefix(packId);
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
        Path resolved = datapackDir.resolve(name).normalize();
        if (!resolved.startsWith(datapackDir))
            return Optional.empty();

        if (!Files.isDirectory(resolved))
            return Optional.empty();

        return Optional.of(resolved);
    }

    private PackEntry toPackEntry(Pack pack, Path datapackDir) {
        if (pack.getPackSource() != PackSource.WORLD)
            return null;
        String id = pack.getId();

        try (PackResources resources = pack.open()) {
            if (resources instanceof PathPackResources) {
                Path rootPath = datapackDir.resolve(stripPrefix(id)).normalize();
                return new PackEntry(id, pack.getTitle().getString(), true, scanNamespaces(rootPath), readPackIcon(rootPath));
            }

            return new PackEntry(id, pack.getTitle().getString(), false, List.of(), new byte[0]);
        }
    }

    private static byte[] readPackIcon(Path packRoot) {
        Path iconPath = packRoot.resolve("pack.png");
        if (!Files.isRegularFile(iconPath))
            return new byte[0];

        try {
            long size = Files.size(iconPath);
            if (size <= 0 || size > MAX_PACK_ICON_BYTES)
                return new byte[0];
            return Files.readAllBytes(iconPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to read pack icon {}: {}", iconPath, e.getMessage());
            return new byte[0];
        }
    }

    private static String stripPrefix(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }

    private static List<String> scanNamespaces(Path packRoot) {
        Path dataDir = packRoot.resolve("data");
        if (!Files.isDirectory(dataDir))
            return List.of();
        try (Stream<Path> dirs = Files.list(dataDir)) {
            return dirs.filter(Files::isDirectory).map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static void writePackIcon(Path packRoot, byte[] icon) throws IOException {
        if (icon == null || icon.length == 0 || !isPng(icon))
            return;

        Files.write(packRoot.resolve("pack.png"), icon);
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 8
            && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47
            && data[4] == 0x0D && data[5] == 0x0A && data[6] == 0x1A && data[7] == 0x0A;
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
