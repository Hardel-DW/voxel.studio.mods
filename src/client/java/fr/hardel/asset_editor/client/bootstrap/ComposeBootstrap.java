package fr.hardel.asset_editor.client.bootstrap;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ComposeBootstrap {

    public enum State { IDLE, DOWNLOADING, READY, FAILED }

    private static final String PROBE_CLASS = "androidx.compose.runtime.Composer";

    private static volatile State state = State.IDLE;
    private static volatile long totalBytes = 0L;
    private static final AtomicLong downloadedBytes = new AtomicLong(0L);
    private static volatile String currentFilename = "";
    private static volatile int currentIndex = 0;
    private static volatile int totalArtifacts = 0;
    private static volatile String errorMessage = "";
    private static volatile Thread worker;

    private static final AtomicBoolean linkedFromCache = new AtomicBoolean(false);
    private static final Set<String> addedJars = new HashSet<>();
    private static ComposeManifest cachedManifest;
    private static IOException manifestLoadError;

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

    public static String currentFilename() {
        return currentFilename;
    }

    public static int currentIndex() {
        return currentIndex;
    }

    public static int totalArtifacts() {
        return totalArtifacts;
    }

    public static String errorMessage() {
        return errorMessage;
    }

    public static boolean tryLinkFromCache() {
        if (linkedFromCache.get()) return isReady();
        ComposeManifest manifest = manifest();
        if (manifest == null) return false;

        Path cacheDir = cacheDirectory(manifest.composeVersion());
        try {
            for (ComposeArtifact artifact : manifest.artifacts()) {
                if (!ComposeDownloader.isCached(cacheDir, artifact)) return false;
            }
            for (ComposeArtifact artifact : manifest.artifacts()) {
                Path jar = ComposeDownloader.resolveCachePath(cacheDir, artifact);
                if (addedJars.add(jar.toAbsolutePath().toString()))
                    ComposeDownloader.registerOnClassPath(jar);
            }
            linkedFromCache.set(true);
            state = State.READY;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized boolean startInstall() {
        if (state == State.DOWNLOADING) return false;
        if (isReady()) return false;

        ComposeManifest manifest = manifest();
        if (manifest == null) {
            state = State.FAILED;
            errorMessage = manifestLoadError == null
                ? "Manifest unavailable"
                : manifestLoadError.getMessage();
            return false;
        }

        resetProgress(manifest);
        state = State.DOWNLOADING;
        errorMessage = "";

        Thread t = new Thread(() -> runInstall(manifest), "asset_editor-compose-bootstrap");
        t.setDaemon(true);
        worker = t;
        t.start();
        return true;
    }

    private static void runInstall(ComposeManifest manifest) {
        Path cacheDir = cacheDirectory(manifest.composeVersion());
        List<ComposeArtifact> artifacts = manifest.artifacts();
        try {
            for (int i = 0; i < artifacts.size(); i++) {
                ComposeArtifact artifact = artifacts.get(i);
                currentIndex = i + 1;
                currentFilename = artifact.filename();

                if (ComposeDownloader.isCached(cacheDir, artifact)) {
                    downloadedBytes.addAndGet(artifact.size());
                } else {
                    long before = downloadedBytes.get();
                    try {
                        ComposeDownloader.download(artifact, cacheDir, downloadedBytes::addAndGet);
                    } catch (IOException e) {
                        downloadedBytes.set(before);
                        throw e;
                    }
                }

                Path jar = ComposeDownloader.resolveCachePath(cacheDir, artifact);
                if (addedJars.add(jar.toAbsolutePath().toString()))
                    ComposeDownloader.registerOnClassPath(jar);
            }

            state = State.READY;
            currentFilename = "";
        } catch (IOException e) {
            errorMessage = humanMessage(e);
            state = State.FAILED;
        } catch (RuntimeException e) {
            errorMessage = humanMessage(e);
            state = State.FAILED;
        }
    }

    private static void resetProgress(ComposeManifest manifest) {
        totalBytes = manifest.totalSize();
        totalArtifacts = manifest.artifacts().size();
        downloadedBytes.set(0L);
        currentIndex = 0;
        currentFilename = "";
    }

    public static ComposeManifest manifest() {
        if (cachedManifest != null) return cachedManifest;
        if (manifestLoadError != null) return null;
        try {
            cachedManifest = ComposeManifest.loadForCurrentPlatform();
        } catch (IOException e) {
            manifestLoadError = e;
            return null;
        }
        return cachedManifest;
    }

    public static IOException manifestLoadError() {
        return manifestLoadError;
    }

    private static Path cacheDirectory(String version) {
        return Paths.get(System.getProperty("user.home"), ".asset_editor", "libs", "compose-" + version);
    }

    private static String humanMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) return t.getClass().getSimpleName();
        return msg;
    }

    private ComposeBootstrap() {}
}
