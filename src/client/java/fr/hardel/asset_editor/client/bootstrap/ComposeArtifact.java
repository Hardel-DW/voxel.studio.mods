package fr.hardel.asset_editor.client.bootstrap;

public record ComposeArtifact(
    String group,
    String name,
    String version,
    String classifier,
    String filename,
    String url,
    String sha256,
    long size
) {
}
