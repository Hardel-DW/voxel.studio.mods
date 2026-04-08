package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * Synchronous AWT-backed clipboard accessor used by [CodeBlock] and
 * [fr.hardel.asset_editor.client.compose.components.ui.editor.CodeEditor].
 *
 * Compose's `LocalClipboardManager` is deprecated and the replacement
 * `LocalClipboard` exposes only suspending `getClipEntry` / `setClipEntry`,
 * which is awkward to call from `onPreviewKeyEvent` handlers. The rest of
 * the project (`CopyButton`, `DebugLogsPage`) already talks to AWT directly,
 * so we follow the same convention to keep clipboard access uniform.
 */
internal object SystemClipboard {

    fun setText(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(text), null)
        }
    }

    fun getText(): String? = runCatching {
        val clip = Toolkit.getDefaultToolkit().systemClipboard
        if (!clip.isDataFlavorAvailable(DataFlavor.stringFlavor)) return@runCatching null
        clip.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
