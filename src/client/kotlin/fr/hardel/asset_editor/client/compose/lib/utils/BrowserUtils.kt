package fr.hardel.asset_editor.client.compose.lib.utils

import org.slf4j.LoggerFactory

object BrowserUtils {

    private val logger = LoggerFactory.getLogger(BrowserUtils::class.java)

    fun openBrowser(url: String): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
        true
    } catch (exception: Exception) {
        logger.warn("Failed to open browser for {}: {}", url, exception.message)
        false
    }
}
