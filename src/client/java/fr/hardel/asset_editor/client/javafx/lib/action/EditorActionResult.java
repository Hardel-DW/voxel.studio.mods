package fr.hardel.asset_editor.client.javafx.lib.action;

public record EditorActionResult(EditorActionStatus status, String message) {

    public static EditorActionResult applied() {
        return new EditorActionResult(EditorActionStatus.APPLIED, null);
    }

    public static EditorActionResult packRequired() {
        return new EditorActionResult(EditorActionStatus.PACK_REQUIRED, "studio:editor.pack_required");
    }

    public static EditorActionResult rejected(String reason) {
        return new EditorActionResult(EditorActionStatus.REJECTED, reason);
    }

    public static EditorActionResult error(String message) {
        return new EditorActionResult(EditorActionStatus.ERROR, message);
    }

    public boolean isApplied() {
        return status == EditorActionStatus.APPLIED;
    }
}
