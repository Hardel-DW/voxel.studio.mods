package fr.hardel.asset_editor.client.bootstrap;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class ComposeBootstrap {

    public enum State { IDLE, DOWNLOADING, READY, FAILED }

    public record CompletionNotice(boolean success, boolean networkIssue) {}

    private static final String PROBE_CLASS = "androidx.compose.runtime.Composer";
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeBootstrap.class);

    private static volatile State state = State.IDLE;
    private static volatile long totalBytes = 0L;
    private static final AtomicLong downloadedBytes = new AtomicLong(0L);
    private static volatile int currentIndex = 0;
    private static volatile int totalArtifacts = 0;
    private static volatile Component errorMessage = Component.empty();

    private static final Set<String> addedJars = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<CompletionNotice> completionNotice = new AtomicReference<>();
    private static ComposeManifest cachedManifest;
    private static BootstrapError manifestLoadError;

    public static State state() {
        return state;
    }

    public static boolean isReady() {
        if (state == State.READY) return true;
        try {
            Class.forName(PROBE_CLASS, false, ComposeBootstrap.class.getClassLoader());
            state = State.READY;
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isDownloading() {
        return state == State.DOWNLOADING;
    }

    public static boolean hasFailed() {
        return state == State.FAILED;
    }

    public static float progress() {
        long total = totalBytes;
        if (total <= 0) return 0f;
        return Math.min(1f, (float) downloadedBytes.get() / (float) total);
    }

    public static long totalBytes() {
        return totalBytes;
    }

    public static long downloadedBytes() {
        return downloadedBytes.get();
    }

    public static int currentIndex() {
        return currentIndex;
    }

    public static int totalArtifacts() {
        return totalArtifacts;
    }

    public static Component errorMessage() {
        return errorMessage;
    }

    public static CompletionNotice consumeCompletionNotice() {
        return completionNotice.getAndSet(null);
    }

    public static ComposeManifest manifest() {
        if (cachedManifest != null) return cachedManifest;
        if (manifestLoadError != null) return null;
        try {
            cachedManifest = ComposeManifest.loadForCurrentPlatform();
            return cachedManifest;
        } catch (BootstrapError e) {
            manifestLoadError = e;
            return null;
        }
    }

    public static void purgeCache() {
        ComposeManifest manifest = manifest();
        if (manifest == null) return;
        Path dir = cacheDirectory(manifest.composeVersion());
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(ComposeBootstrap::deleteQuietly);
        } catch (IOException ignored) {
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    public static boolean tryLinkFromCache() {
        if (isReady()) return true;

        ComposeManifest manifest = manifest();
        if (manifest == null) return false;

        Path cacheDir = cacheDirectory(manifest.composeVersion());
        if (!isCacheComplete(manifest, cacheDir)) return false;

        linkArtifacts(manifest.artifacts(), cacheDir);
        state = State.READY;
        return true;
    }

    public static synchronized void startInstall() {
        if (state == State.DOWNLOADING || isReady()) return;

        ComposeManifest manifest = manifest();
        if (manifest == null) {
            reportFailure(manifestLoadError);
            return;
        }

        resetProgress(manifest);
        errorMessage = Component.empty();
        state = State.DOWNLOADING;
        completionNotice.set(null);

        Thread worker = new Thread(() -> runInstall(manifest), "asset_editor-compose-bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private static void runInstall(ComposeManifest manifest) {
        Path cacheDir = cacheDirectory(manifest.composeVersion());
        List<ComposeArtifact> artifacts = manifest.artifacts();
        try {
            for (int i = 0; i < artifacts.size(); i++) {
                currentIndex = i + 1;
                installArtifact(artifacts.get(i), cacheDir);
            }
            state = State.READY;
            completionNotice.set(new CompletionNotice(true, false));
            LOGGER.info("Compose runtime install completed: {} artifacts cached in {}", artifacts.size(), cacheDir);
        } catch (BootstrapError e) {
            reportFailure(e);
        } catch (RuntimeException e) {
            reportFailure(e);
        }
    }

    private static void installArtifact(ComposeArtifact artifact, Path cacheDir) throws BootstrapError {
        if (ComposeDownloader.isCached(cacheDir, artifact)) {
            downloadedBytes.addAndGet(artifact.size());
        } else {
            long before = downloadedBytes.get();
            try {
                ComposeDownloader.download(artifact, cacheDir, downloadedBytes::addAndGet);
            } catch (BootstrapError e) {
                downloadedBytes.set(before);
                throw e;
            }
        }
        linkArtifact(artifact, cacheDir);
    }

    private static boolean isCacheComplete(ComposeManifest manifest, Path cacheDir) {
        try {
            for (ComposeArtifact artifact : manifest.artifacts())
                if (!ComposeDownloader.isCached(cacheDir, artifact)) return false;
            return true;
        } catch (BootstrapError e) {
            return false;
        }
    }

    private static void linkArtifacts(List<ComposeArtifact> artifacts, Path cacheDir) {
        for (ComposeArtifact artifact : artifacts)
            linkArtifact(artifact, cacheDir);
    }

    private static void linkArtifact(ComposeArtifact artifact, Path cacheDir) {
        Path jar = ComposeDownloader.resolveCachePath(cacheDir, artifact);
        if (addedJars.add(jar.toAbsolutePath().toString()))
            ComposeDownloader.registerOnClassPath(jar);
    }

    private static void resetProgress(ComposeManifest manifest) {
        totalBytes = manifest.totalSize();
        totalArtifacts = manifest.artifacts().size();
        downloadedBytes.set(0L);
        currentIndex = 0;
    }

    private static void reportFailure(BootstrapError error) {
        errorMessage = error.asComponent();
        state = State.FAILED;

        boolean networkIssue = isLikelyNetworkIssue(error);
        completionNotice.set(new CompletionNotice(false, networkIssue));
        if (networkIssue) {
            LOGGER.warn("Compose runtime install failed due to a network issue: {}", errorMessage.getString(), error);
            return;
        }
        LOGGER.error("Compose runtime install failed: {}", errorMessage.getString(), error);
    }

    private static void reportFailure(RuntimeException error) {
        errorMessage = Component.translatable("asset_editor.bootstrap.error.generic",
            error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        state = State.FAILED;
        completionNotice.set(new CompletionNotice(false, false));
        LOGGER.error("Compose runtime install failed unexpectedly: {}", errorMessage.getString(), error);
    }

    private static boolean isLikelyNetworkIssue(BootstrapError error) {
        if (!"asset_editor.bootstrap.error.download_failed".equals(error.translationKey()))
            return false;

        Throwable current = error;
        while (current != null) {
            if (current instanceof UnknownHostException
                || current instanceof ConnectException
                || current instanceof NoRouteToHostException
                || current instanceof SocketTimeoutException
                || current instanceof SocketException)
                return true;
            current = current.getCause();
        }
        return false;
    }

    private static Path cacheDirectory(String version) {
        return Paths.get(System.getProperty("user.home"), ".asset_editor", "libs", "compose-" + version);
    }

    private ComposeBootstrap() {}
}
