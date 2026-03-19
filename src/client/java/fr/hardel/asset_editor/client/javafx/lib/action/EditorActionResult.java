package fr.hardel.asset_editor.client.javafx.lib.action;

public record EditorActionResult(Status status, String message) {

    public enum Status {
        APPLIED,
        PACK_REQUIRED,
        REJECTED,
        ERROR
    }

    public static EditorActionResult applied() {
        return new EditorActionResult(Status.APPLIED, null);
    }

    public static EditorActionResult packRequired() {
        return new EditorActionResult(Status.PACK_REQUIRED, "error:pack_required");
    }

    public static EditorActionResult rejected(String reason) {
        return new EditorActionResult(Status.REJECTED, reason);
    }

    public static EditorActionResult error(String message) {
        return new EditorActionResult(Status.ERROR, message);
    }

    public boolean isApplied() {
        return status == Status.APPLIED;
    }
}
