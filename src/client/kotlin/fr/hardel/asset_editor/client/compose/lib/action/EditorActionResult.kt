package fr.hardel.asset_editor.client.compose.lib.action

data class EditorActionResult(val status: Status, val message: String?) {

    enum class Status {
        APPLIED,
        PACK_REQUIRED,
        REJECTED,
        ERROR
    }

    companion object {
        @JvmStatic
        fun applied(): EditorActionResult = EditorActionResult(Status.APPLIED, null)

        @JvmStatic
        fun packRequired(): EditorActionResult = EditorActionResult(Status.PACK_REQUIRED, "error:pack_required")

        @JvmStatic
        fun rejected(reason: String): EditorActionResult = EditorActionResult(Status.REJECTED, reason)

        @JvmStatic
        fun error(message: String): EditorActionResult = EditorActionResult(Status.ERROR, message)
    }
}
