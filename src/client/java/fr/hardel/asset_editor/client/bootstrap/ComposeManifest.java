package fr.hardel.asset_editor.client.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record ComposeManifest(String composeVersion, List<ComposeArtifact> artifacts) {

    private static final String RESOURCE_PATH = "/asset_editor/compose-manifest.json";

    public static ComposeManifest loadForCurrentPlatform() throws BootstrapError {
        JsonObject root = readRoot();
        String version = root.get("composeVersion").getAsString();
        String platform = detectPlatform();

        JsonObject platforms = root.getAsJsonObject("platforms");
        if (!platforms.has(platform))
            throw new BootstrapError("asset_editor.bootstrap.error.unsupported_platform", platform);

        List<ComposeArtifact> all = new ArrayList<>();
        root.getAsJsonArray("common").forEach(el -> all.add(parseArtifact(el.getAsJsonObject())));
        platforms.getAsJsonArray(platform).forEach(el -> all.add(parseArtifact(el.getAsJsonObject())));
        return new ComposeManifest(version, List.copyOf(all));
    }

    public long totalSize() {
        return artifacts.stream().mapToLong(ComposeArtifact::size).sum();
    }

    public static String detectPlatform() throws BootstrapError {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");

        if (os.contains("win")) return "windows-x64";
        if (os.contains("mac") || os.contains("darwin")) return arm64 ? "macos-arm64" : "macos-x64";
        if (os.contains("nux") || os.contains("nix")) return arm64 ? "linux-arm64" : "linux-x64";
        throw new BootstrapError("asset_editor.bootstrap.error.unsupported_os", os, arch);
    }

    private static JsonObject readRoot() throws BootstrapError {
        try (InputStream stream = ComposeManifest.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null)
                throw new BootstrapError("asset_editor.bootstrap.error.manifest_missing");
            return new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
        } catch (java.io.IOException e) {
            throw new BootstrapError("asset_editor.bootstrap.error.manifest_unreadable", e, e.getMessage());
        }
    }

    private static ComposeArtifact parseArtifact(JsonObject obj) {
        return new ComposeArtifact(
            obj.get("group").getAsString(),
            obj.get("name").getAsString(),
            obj.get("version").getAsString(),
            obj.get("classifier").getAsString(),
            obj.get("filename").getAsString(),
            obj.get("url").getAsString(),
            obj.get("sha256").getAsString(),
            obj.get("size").getAsLong()
        );
    }
}
