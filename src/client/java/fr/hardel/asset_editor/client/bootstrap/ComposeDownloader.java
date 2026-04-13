package fr.hardel.asset_editor.client.bootstrap;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class ComposeDownloader {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int BUFFER_SIZE = 64 * 1024;

    interface ProgressSink {
        void onBytes(long delta);
    }

    static Path resolveCachePath(Path cacheDir, ComposeArtifact artifact) {
        return cacheDir.resolve(artifact.filename());
    }

    static boolean isCached(Path cacheDir, ComposeArtifact artifact) throws BootstrapError {
        try {
            Path target = resolveCachePath(cacheDir, artifact);
            if (!Files.isRegularFile(target)) return false;
            if (Files.size(target) != artifact.size()) return false;
            return sha256(target).equalsIgnoreCase(artifact.sha256());
        } catch (IOException e) {
            throw new BootstrapError("asset_editor.bootstrap.error.cache_probe_failed", e, artifact.filename(), e.getMessage());
        }
    }

    static void download(ComposeArtifact artifact, Path cacheDir, ProgressSink sink) throws BootstrapError {
        Path temp = cacheDir.resolve(artifact.filename() + ".part");
        try {
            Files.createDirectories(cacheDir);
            Files.deleteIfExists(temp);
            fetchToTemp(artifact, temp, sink);
            verifyTempFile(artifact, temp);
            Files.move(temp, resolveCachePath(cacheDir, artifact),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (BootstrapError e) {
            deleteQuietly(temp);
            throw e;
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new BootstrapError("asset_editor.bootstrap.error.download_failed", e, artifact.filename(), e.getMessage());
        }
    }

    static void registerOnClassPath(Path jar) {
        FabricLauncherBase.getLauncher().addToClassPath(jar);
    }

    private static void fetchToTemp(ComposeArtifact artifact, Path temp, ProgressSink sink) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(artifact.url()).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "asset_editor/compose-bootstrap");
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(temp)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
                sink.onBytes(read);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void verifyTempFile(ComposeArtifact artifact, Path temp) throws IOException {
        long actualSize = Files.size(temp);
        if (actualSize != artifact.size())
            throw new BootstrapError("asset_editor.bootstrap.error.size_mismatch",
                artifact.filename(), artifact.size(), actualSize);

        String actualSha = sha256(temp);
        if (!actualSha.equalsIgnoreCase(artifact.sha256()))
            throw new BootstrapError("asset_editor.bootstrap.error.sha_mismatch", artifact.filename());
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new BootstrapError("asset_editor.bootstrap.error.sha_algo_unavailable", e);
        }

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buf)) > 0) md.update(buf, 0, read);
        }

        byte[] digest = md.digest();
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private ComposeDownloader() {}
}
