package fr.hardel.asset_editor.client.javafx.lib.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrowserUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserUtils.class);

    public static boolean openBrowser(String url) {
        try {
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to open browser for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private BrowserUtils() {
    }
}
