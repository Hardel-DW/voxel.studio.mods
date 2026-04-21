package fr.hardel.asset_editor.client.splash;

import fr.hardel.asset_editor.AssetEditor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SplashAssets {

    public record SvgShape(Path2D path, double viewBoxWidth, double viewBoxHeight) {}

    static final String ICON_LOGO = "logo.svg";
    static final String ICON_GITHUB = "company/github.svg";

    static final String FONT_EXTRABOLD = "rubik-extrabold.ttf";
    static final String FONT_MEDIUM = "rubik-medium.ttf";

    private static final String ASSET_ROOT = "/assets/" + AssetEditor.MOD_ID + "/";
    private static final String ICON_ROOT = ASSET_ROOT + "icons/";
    private static final String FONT_ROOT = ASSET_ROOT + "fonts/";

    private static final Pattern VIEW_BOX = Pattern.compile("viewBox\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern PATH_D = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern RECT = Pattern.compile("<rect\\b[^>]*x\\s*=\\s*\"([^\"]+)\"[^>]*y\\s*=\\s*\"([^\"]+)\"[^>]*width\\s*=\\s*\"([^\"]+)\"[^>]*height\\s*=\\s*\"([^\"]+)\"");

    private static final ConcurrentMap<String, SvgShape> SHAPES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Font> FONTS = new ConcurrentHashMap<>();

    static SvgShape shape(String name) {
        return SHAPES.computeIfAbsent(name, SplashAssets::loadSvg);
    }

    static Font font(String name) {
        return FONTS.computeIfAbsent(name, SplashAssets::loadFont);
    }

    public static List<BufferedImage> logoIcons() {
        SvgShape logo = shape(ICON_LOGO);
        return List.of(16, 24, 32, 48, 64, 128, 256).stream()
            .map(size -> rasterize(logo, size))
            .toList();
    }

    private static BufferedImage rasterize(SvgShape shape, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double scale = size / shape.viewBoxWidth();
        g.scale(scale, scale);
        g.setColor(Color.WHITE);
        g.fill(shape.path());
        g.dispose();
        return image;
    }

    public static void preloadAsync() {
        Thread worker = new Thread(SplashAssets::preload, "asset_editor-splash-preload");
        worker.setDaemon(true);
        worker.start();
    }

    private static void preload() {
        shape(ICON_LOGO);
        shape(ICON_GITHUB);
        font(FONT_EXTRABOLD);
        font(FONT_MEDIUM);
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
            Path2D path = rectPath(
                Double.parseDouble(rectMatcher.group(1)),
                Double.parseDouble(rectMatcher.group(2)),
                Double.parseDouble(rectMatcher.group(3)),
                Double.parseDouble(rectMatcher.group(4)));
            return new SvgShape(path, viewBox[0], viewBox[1]);
        }

        throw new IllegalStateException("Unsupported SVG (no <path d> or <rect>): " + name);
    }

    private static Path2D rectPath(double x, double y, double w, double h) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + w, y);
        path.lineTo(x + w, y + h);
        path.lineTo(x, y + h);
        path.closePath();
        return path;
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
