package fr.hardel.asset_editor.client.javafx.lib.utils;

public final class BrowserUtils {

    public static boolean openBrowser(String url) {
        try {
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private BrowserUtils() {
    }
}
