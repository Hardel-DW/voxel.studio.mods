package fr.hardel.asset_editor.client.splash;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SplashAssets {

    record SvgShape(Path2D path, double viewBoxWidth, double viewBoxHeight) {}

    private static final String ASSET_ROOT = "/assets/asset_editor/";
    private static final String ICON_ROOT = ASSET_ROOT + "icons/";
    private static final String FONT_ROOT = ASSET_ROOT + "fonts/";

    private static final Pattern VIEW_BOX = Pattern.compile("viewBox\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern PATH_D = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern RECT = Pattern.compile(
        "<rect\\b[^>]*" +
            "x\\s*=\\s*\"([^\"]+)\"[^>]*" +
            "y\\s*=\\s*\"([^\"]+)\"[^>]*" +
            "width\\s*=\\s*\"([^\"]+)\"[^>]*" +
            "height\\s*=\\s*\"([^\"]+)\"");

    private static volatile SvgShape logo;
    private static volatile SvgShape github;
    private static volatile SvgShape minimize;
    private static volatile SvgShape maximize;
    private static volatile SvgShape close;
    private static volatile Font rubikExtraBold;
    private static volatile Font rubikMedium;

    static SvgShape logo() {
        if (logo == null)
            logo = loadSvg("logo.svg");
        return logo;
    }

    static SvgShape github() {
        if (github == null)
            github = loadSvg("company/github.svg");
        return github;
    }

    static SvgShape minimize() {
        if (minimize == null)
            minimize = loadSvg("window/minimize.svg");
        return minimize;
    }

    static SvgShape maximize() {
        if (maximize == null)
            maximize = loadSvg("window/maximize.svg");
        return maximize;
    }

    static SvgShape close() {
        if (close == null)
            close = loadSvg("window/close.svg");
        return close;
    }

    static Font rubikExtraBold() {
        if (rubikExtraBold == null)
            rubikExtraBold = loadFont("rubik-extrabold.ttf");
        return rubikExtraBold;
    }

    static Font rubikMedium() {
        if (rubikMedium == null)
            rubikMedium = loadFont("rubik-medium.ttf");
        return rubikMedium;
    }

    public static void preload() {
        logo();
        github();
        minimize();
        maximize();
        close();
        rubikExtraBold();
        rubikMedium();
    }

    public static void preloadAsync() {
        Thread worker = new Thread(SplashAssets::preload, "asset_editor-splash-preload");
        worker.setDaemon(true);
        worker.start();
    }

    private static SvgShape loadSvg(String name) {
        String xml = readResource(ICON_ROOT + name);
        double[] viewBox = parseViewBox(xml);

        Matcher pathMatcher = PATH_D.matcher(xml);
        if (pathMatcher.find()) {
            Path2D path = SvgPath.parse(pathMatcher.group(1));
            return new SvgShape(path, viewBox[0], viewBox[1]);
        }

        Matcher rectMatcher = RECT.matcher(xml);
        if (rectMatcher.find()) {
            double x = Double.parseDouble(rectMatcher.group(1));
            double y = Double.parseDouble(rectMatcher.group(2));
            double w = Double.parseDouble(rectMatcher.group(3));
            double h = Double.parseDouble(rectMatcher.group(4));
            Path2D path = new Path2D.Double();
            path.moveTo(x, y);
            path.lineTo(x + w, y);
            path.lineTo(x + w, y + h);
            path.lineTo(x, y + h);
            path.closePath();
            return new SvgShape(path, viewBox[0], viewBox[1]);
        }

        throw new IllegalStateException("Unsupported SVG (no <path d> or <rect>): " + name);
    }

    private static double[] parseViewBox(String xml) {
        Matcher m = VIEW_BOX.matcher(xml);
        if (!m.find())
            return new double[] { 16.0, 16.0 };
        String[] parts = m.group(1).trim().split("\\s+");
        return new double[] { Double.parseDouble(parts[2]), Double.parseDouble(parts[3]) };
    }

    private static Font loadFont(String name) {
        try (InputStream stream = resourceStream(FONT_ROOT + name)) {
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Failed to load splash font: " + name, e);
        }
    }

    private static String readResource(String absolutePath) {
        try (InputStream stream = resourceStream(absolutePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read splash resource: " + absolutePath, e);
        }
    }

    private static InputStream resourceStream(String absolutePath) throws IOException {
        InputStream stream = SplashAssets.class.getResourceAsStream(absolutePath);
        if (stream == null)
            throw new IOException("Resource not found: " + absolutePath);
        return stream;
    }

    private SplashAssets() {}
}
