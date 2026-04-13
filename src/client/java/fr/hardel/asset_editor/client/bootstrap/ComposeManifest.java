package fr.hardel.asset_editor.client.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ComposeManifest(String composeVersion, List<ComposeArtifact> artifacts) {

    private static final String RESOURCE_PATH = "/asset_editor/compose-manifest.json";

    public static ComposeManifest loadForCurrentPlatform() throws IOException {
        try (InputStream stream = ComposeManifest.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null)
                throw new IOException("compose-manifest.json missing from classpath (" + RESOURCE_PATH + ")");

            JsonObject root = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            String version = root.get("composeVersion").getAsString();
            String platform = detectPlatform();

            List<ComposeArtifact> all = new ArrayList<>();
            root.getAsJsonArray("common").forEach(el -> all.add(parseArtifact(el.getAsJsonObject())));

            JsonObject platforms = root.getAsJsonObject("platforms");
            if (!platforms.has(platform))
                throw new IOException("Unsupported platform: " + platform + " (available: " + platforms.keySet() + ")");

            platforms.getAsJsonArray(platform).forEach(el -> all.add(parseArtifact(el.getAsJsonObject())));
            return new ComposeManifest(version, List.copyOf(all));
        }
    }

    public long totalSize() {
        return artifacts.stream().mapToLong(ComposeArtifact::size).sum();
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

    public static String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");

        if (os.contains("win")) return "windows-x64";
        if (os.contains("mac") || os.contains("darwin")) {
            if (!arm64)
                throw new UnsupportedOperationException("macOS Intel (x86_64) is not supported — only Apple Silicon");
            return "macos-arm64";
        }
        if (os.contains("nux") || os.contains("nix")) return arm64 ? "linux-arm64" : "linux-x64";
        throw new UnsupportedOperationException("Unsupported OS: " + os + " " + arch);
    }
}
