package fr.hardel.asset_editor.client.splash;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.splash.SplashAssets.SvgShape;
import net.minecraft.client.resources.language.I18n;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class SwingSplashPanel extends JPanel {

    public interface Actions {
        void minimize();

        void maximize();

        void close();

        void openHelp();

        void openGithub();

        void dragStart();

        void dragMove();

        void dragEnd();

        void dragDoubleClick();
    }

    private enum HoverTarget {
        MINIMIZE, MAXIMIZE, CLOSE, HELP, GITHUB
    }

    private static final class Dim {
        static final int TITLE_BAR_HEIGHT = 32;
        static final int FRAME_PAD_TOP = 48;
        static final int FRAME_PAD_SIDE = 24;
        static final int FRAME_PAD_BOTTOM = 24;
        static final int FRAME_INNER_PAD = 16;
        static final int GRID_CELL = 64;
        static final int CORNER_SIZE = 16;
        static final int WINDOW_CTRL_WIDTH = 36;
        static final int LOADING_BOTTOM_OFFSET = 80;
        static final int DOT_SIZE = 4;
        static final int DOT_GAP = 4;
        static final int LOGO_SIZE = 96;
        static final int TITLE_BAR_LOGO_SIZE = 16;
        static final int WINDOW_ICON_SIZE = 12;
        static final int GITHUB_ICON_SIZE = 16;
        static final int TITLE_SIZE = 36;
        static final int TITLE_BAR_TEXT_SIZE = 12;
        static final int SMALL_TEXT_SIZE = 10;
        static final int SUBTITLE_SIZE = 12;

        private Dim() {}
    }

    private static final class Palette {
        static final Color GRID_LINE = new Color(0x09090B);
        static final Color GRID_VIGNETTE = new Color(0, 0, 0, 153);
        static final Color DASHED_BORDER = new Color(0x151418);
        static final Color CORNER = new Color(0x52525B);
        static final Color DOT_1 = new Color(0x71717A);
        static final Color DOT_2 = new Color(0x52525B);
        static final Color DOT_3 = new Color(0x3F3F46);
        static final Color ZINC_200 = new Color(0xE4E4E7);
        static final Color ZINC_400 = new Color(0xA1A1AA);
        static final Color ZINC_500 = new Color(0x71717A);
        static final Color ZINC_600 = new Color(0x52525B);
        static final Color RED_400 = new Color(0xF87171);
        static final Color TRANSPARENT = new Color(0, 0, 0, 0);

        private Palette() {}
    }

    private static final float[] DASH_PATTERN = { 6f, 4f };
    private static final long PULSE_PERIOD_MS = 3000L;
    private static final float PULSE_MIN = 0.3f;

    private final Map<HoverTarget, SvgShape> windowIcons;
    private final SvgShape logoShape;
    private final SvgShape githubShape;
    private final Font titleFont;
    private final Font titleBarFont;
    private final Font subtitleFont;
    private final Font monoSmallTracked;

    private final String appTitle;
    private final String splashTitle;
    private final String splashSubtitle;
    private final String splashLoading;
    private final String splashHelp;
    private final String buildVersion;

    private final Actions actions;
    private final Timer animationTimer;
    private final long animStart;
    private final List<HitRegion> hitRegions = new ArrayList<>();

    private Rectangle titleBarDragRegion = new Rectangle();
    private HoverTarget hoverTarget;

    private float globalAlpha = 1f;
    private Timer fadeTimer;
    private float frozenLoadingAlpha = 1f;

    public SwingSplashPanel(Actions actions) {
        this.actions = actions;

        setOpaque(false);
        setBackground(Color.BLACK);
        setLayout(null);

        logoShape = SplashAssets.shape(SplashAssets.ICON_LOGO);
        githubShape = SplashAssets.shape(SplashAssets.ICON_GITHUB);
        windowIcons = new EnumMap<>(HoverTarget.class);
        windowIcons.put(HoverTarget.MINIMIZE, SplashAssets.shape(SplashAssets.ICON_MINIMIZE));
        windowIcons.put(HoverTarget.MAXIMIZE, SplashAssets.shape(SplashAssets.ICON_MAXIMIZE));
        windowIcons.put(HoverTarget.CLOSE, SplashAssets.shape(SplashAssets.ICON_CLOSE));

        titleFont = SplashAssets.font(SplashAssets.FONT_EXTRABOLD).deriveFont((float) Dim.TITLE_SIZE);
        titleBarFont = SplashAssets.font(SplashAssets.FONT_MEDIUM).deriveFont((float) Dim.TITLE_BAR_TEXT_SIZE);
        subtitleFont = withTracking(SplashAssets.font(SplashAssets.FONT_MEDIUM).deriveFont((float) Dim.SUBTITLE_SIZE), 0.3f);
        monoSmallTracked = withTracking(new Font(Font.MONOSPACED, Font.PLAIN, Dim.SMALL_TEXT_SIZE), 0.1f);

        appTitle = I18n.get("app:title");
        splashTitle = I18n.get("splash:title");
        splashSubtitle = I18n.get("splash:subtitle").toUpperCase(Locale.ROOT);
        splashLoading = I18n.get("splash:loading").toUpperCase(Locale.ROOT);
        splashHelp = I18n.get("splash:help");
        buildVersion = "BUILD " + AssetEditorClient.BUILD_VERSION;

        animStart = System.currentTimeMillis();
        animationTimer = new Timer(16, e -> repaint());

        MouseAdapter mouse = new SplashMouseHandler();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void startAnimation() {
        if (!animationTimer.isRunning())
            animationTimer.start();
    }

    public void stopAnimation() {
        animationTimer.stop();
    }

    public void fadeOut(int durationMs, Runnable onComplete) {
        cancelFade();
        frozenLoadingAlpha = easedPulse(System.currentTimeMillis() - animStart);

        long start = System.currentTimeMillis();
        fadeTimer = new Timer(16, null);
        fadeTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= durationMs) {
                globalAlpha = 0f;
                finishFade();
                SwingUtilities.invokeLater(onComplete);
                return;
            }
            float t = (float) elapsed / durationMs;
            globalAlpha = 1f - t * t * t;
            repaint();
        });
        fadeTimer.start();
    }

    public void cancelFade() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        if (globalAlpha != 1f) {
            globalAlpha = 1f;
            repaint();
        }
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        cancelFade();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        applyRenderingHints(g2);
        g2.setComposite(AlphaComposite.SrcOver.derive(globalAlpha));

        int w = getWidth();
        int h = getHeight();
        long elapsed = System.currentTimeMillis() - animStart;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        Rectangle frame = new Rectangle(
            Dim.FRAME_PAD_SIDE, Dim.FRAME_PAD_TOP,
            w - 2 * Dim.FRAME_PAD_SIDE, h - Dim.FRAME_PAD_TOP - Dim.FRAME_PAD_BOTTOM);

        hitRegions.clear();

        paintGrid(g2, frame);
        paintCenterContent(g2, w, h);
        paintLoadingText(g2, w, h, elapsed);
        paintDashedFrame(g2, frame);
        paintCorners(g2, frame);
        paintFrameStrips(g2, frame);
        paintTitleBar(g2, w);

        g2.dispose();
    }

    private void finishFade() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        repaint();
    }

    private static void applyRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private void paintGrid(Graphics2D g, Rectangle frame) {
        Shape previousClip = g.getClip();
        g.setClip(frame);

        g.setColor(Palette.GRID_LINE);
        g.setStroke(new BasicStroke(1f));

        int right = frame.x + frame.width;
        int bottom = frame.y + frame.height;
        for (int x = frame.x; x <= right; x += Dim.GRID_CELL) {
            float snapped = x + 0.5f;
            g.draw(new Line2D.Float(snapped, frame.y, snapped, bottom));
        }
        for (int y = frame.y; y <= bottom; y += Dim.GRID_CELL) {
            float snapped = y + 0.5f;
            g.draw(new Line2D.Float(frame.x, snapped, right, snapped));
        }

        paintGridVignette(g, frame);
        g.setClip(previousClip);
    }

    private void paintGridVignette(Graphics2D g, Rectangle frame) {
        float cx = frame.x + frame.width / 2f;
        float cy = frame.y + frame.height / 2f;
        float rx = 0.6f * frame.width;
        float ry = 0.5f * frame.height;
        if (rx <= 0 || ry <= 0)
            return;

        float[] fractions = { 0f, 0.4f, 1f };
        Color[] colors = { Palette.TRANSPARENT, Palette.TRANSPARENT, Palette.GRID_VIGNETTE };

        AffineTransform gradientTransform = new AffineTransform();
        gradientTransform.translate(cx, cy);
        gradientTransform.scale(1.0, ry / rx);
        gradientTransform.translate(-cx, -cy);

        g.setPaint(new RadialGradientPaint(
            new Point2D.Float(cx, cy), rx, new Point2D.Float(cx, cy),
            fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE,
            MultipleGradientPaint.ColorSpaceType.SRGB, gradientTransform));
        g.fill(frame);
    }

    private void paintCenterContent(Graphics2D g, int w, int h) {
        int centerX = w / 2;
        int centerY = h / 2;

        FontMetrics titleMetrics = g.getFontMetrics(titleFont);
        FontMetrics subtitleMetrics = g.getFontMetrics(subtitleFont);
        int titleHeight = titleMetrics.getAscent() + titleMetrics.getDescent();
        int subtitleHeight = subtitleMetrics.getAscent() + subtitleMetrics.getDescent();

        int blockHeight = Dim.LOGO_SIZE + 32 + titleHeight + 4 + subtitleHeight;
        int cursorY = centerY - blockHeight / 2;

        paintLogo(g, centerX - Dim.LOGO_SIZE / 2, cursorY);
        cursorY += Dim.LOGO_SIZE + 32;

        cursorY += titleMetrics.getAscent();
        paintTitleWithGradient(g, centerX, cursorY, titleMetrics);
        cursorY += titleMetrics.getDescent();

        cursorY += 4 + subtitleMetrics.getAscent();
        g.setFont(subtitleFont);
        g.setColor(Palette.ZINC_500);
        int subtitleWidth = subtitleMetrics.stringWidth(splashSubtitle);
        g.drawString(splashSubtitle, centerX - subtitleWidth / 2, cursorY);
    }

    private void paintTitleWithGradient(Graphics2D g, int centerX, int baselineY, FontMetrics metrics) {
        int width = metrics.stringWidth(splashTitle);
        int x = centerX - width / 2;
        int top = baselineY - metrics.getAscent();
        int bottom = baselineY + metrics.getDescent();

        g.setFont(titleFont);
        g.setPaint(new GradientPaint(0, top, Color.WHITE, 0, bottom, Palette.ZINC_400));
        g.drawString(splashTitle, x, baselineY);
    }

    private void paintLogo(Graphics2D g, int x, int y) {
        paintSvg(g, logoShape, x, y, Dim.LOGO_SIZE, Color.WHITE, 1f);
    }

    private void paintLoadingText(Graphics2D g, int w, int h, long elapsed) {
        float alpha = fadeTimer != null ? frozenLoadingAlpha : easedPulse(elapsed);
        FontMetrics metrics = g.getFontMetrics(monoSmallTracked);
        int textWidth = metrics.stringWidth(splashLoading);
        int baseline = h - Dim.LOADING_BOTTOM_OFFSET - metrics.getDescent();

        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.SrcOver.derive(alpha * globalAlpha));
        gg.setFont(monoSmallTracked);
        gg.setColor(Palette.ZINC_400);
        gg.drawString(splashLoading, (w - textWidth) / 2, baseline);
        gg.dispose();
    }

    private void paintDashedFrame(Graphics2D g, Rectangle frame) {
        g.setColor(Palette.DASHED_BORDER);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH_PATTERN, 0f));
        g.draw(new Rectangle2D.Float(frame.x + 0.5f, frame.y + 0.5f, frame.width - 1f, frame.height - 1f));
    }

    private void paintCorners(Graphics2D g, Rectangle frame) {
        g.setColor(Palette.CORNER);
        g.setStroke(new BasicStroke(1f));

        int x1 = frame.x - 1;
        int y1 = frame.y - 1;
        int x2 = frame.x + frame.width + 1;
        int y2 = frame.y + frame.height + 1;

        drawCorner(g, x1, y1, true, true);
        drawCorner(g, x2 - Dim.CORNER_SIZE, y1, true, false);
        drawCorner(g, x1, y2 - Dim.CORNER_SIZE, false, true);
        drawCorner(g, x2 - Dim.CORNER_SIZE, y2 - Dim.CORNER_SIZE, false, false);
    }

    private void drawCorner(Graphics2D g, int x, int y, boolean top, boolean left) {
        float cx = x + 0.5f;
        float cy = y + 0.5f;
        float endX = x + Dim.CORNER_SIZE;
        float endY = y + Dim.CORNER_SIZE;

        float horizontalY = top ? cy : endY - 0.5f;
        g.draw(new Line2D.Float(cx, horizontalY, endX, horizontalY));

        float verticalX = left ? cx : endX - 0.5f;
        g.draw(new Line2D.Float(verticalX, cy, verticalX, endY));
    }

    private void paintFrameStrips(Graphics2D g, Rectangle frame) {
        int innerX = frame.x + Dim.FRAME_INNER_PAD;
        int innerY = frame.y + Dim.FRAME_INNER_PAD;
        int innerW = frame.width - 2 * Dim.FRAME_INNER_PAD;
        int innerBottom = frame.y + frame.height - Dim.FRAME_INNER_PAD;

        paintFrameTop(g, innerX, innerY, innerW);
        paintFrameBottom(g, innerX, innerBottom, innerW);
    }

    private void paintFrameTop(Graphics2D g, int x, int y, int width) {
        int dotY = y + (Dim.TITLE_BAR_TEXT_SIZE - Dim.DOT_SIZE) / 2 + 2;
        drawDot(g, x, dotY, Palette.DOT_1);
        drawDot(g, x + Dim.DOT_SIZE + Dim.DOT_GAP, dotY, Palette.DOT_2);
        drawDot(g, x + 2 * (Dim.DOT_SIZE + Dim.DOT_GAP), dotY, Palette.DOT_3);

        g.setFont(monoSmallTracked);
        g.setColor(Palette.ZINC_600);
        FontMetrics m = g.getFontMetrics(monoSmallTracked);
        int textWidth = m.stringWidth(buildVersion);
        g.drawString(buildVersion, x + width - textWidth, y + m.getAscent());
    }

    private void paintFrameBottom(Graphics2D g, int x, int bottomY, int width) {
        g.setFont(monoSmallTracked);
        FontMetrics m = g.getFontMetrics(monoSmallTracked);
        int helpWidth = m.stringWidth(splashHelp);
        int helpHeight = m.getAscent() + m.getDescent();
        int baseline = bottomY - m.getDescent();

        g.setColor(hoverTarget == HoverTarget.HELP ? Palette.ZINC_400 : Palette.ZINC_600);
        g.drawString(splashHelp, x, baseline);
        registerHit(HoverTarget.HELP, new Rectangle(x, baseline - m.getAscent(), helpWidth, helpHeight));

        int iconX = x + width - Dim.GITHUB_ICON_SIZE;
        int iconY = bottomY - Dim.GITHUB_ICON_SIZE;
        float alpha = hoverTarget == HoverTarget.GITHUB ? 0.7f : 1f;
        paintSvg(g, githubShape, iconX, iconY, Dim.GITHUB_ICON_SIZE, Color.WHITE, alpha);
        registerHit(HoverTarget.GITHUB, new Rectangle(iconX, iconY, Dim.GITHUB_ICON_SIZE, Dim.GITHUB_ICON_SIZE));
    }

    private void drawDot(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRect(x, y, Dim.DOT_SIZE, Dim.DOT_SIZE);
    }

    private void paintTitleBar(Graphics2D g, int w) {
        int logoX = 12;
        int logoY = (Dim.TITLE_BAR_HEIGHT - Dim.TITLE_BAR_LOGO_SIZE) / 2;
        paintSvg(g, logoShape, logoX, logoY, Dim.TITLE_BAR_LOGO_SIZE, Color.WHITE, 1f);

        int titleX = logoX + Dim.TITLE_BAR_LOGO_SIZE + 8;
        g.setFont(titleBarFont);
        FontMetrics m = g.getFontMetrics(titleBarFont);
        int baseline = (Dim.TITLE_BAR_HEIGHT - m.getHeight()) / 2 + m.getAscent();
        g.setColor(Palette.ZINC_400);
        g.drawString(appTitle, titleX, baseline);

        int controlsStart = w - 3 * Dim.WINDOW_CTRL_WIDTH;
        titleBarDragRegion = new Rectangle(0, 0, controlsStart, Dim.TITLE_BAR_HEIGHT);

        paintWindowControl(g, controlsStart, HoverTarget.MINIMIZE, Palette.ZINC_200);
        paintWindowControl(g, controlsStart + Dim.WINDOW_CTRL_WIDTH, HoverTarget.MAXIMIZE, Palette.ZINC_200);
        paintWindowControl(g, controlsStart + 2 * Dim.WINDOW_CTRL_WIDTH, HoverTarget.CLOSE, Palette.RED_400);
    }

    private void paintWindowControl(Graphics2D g, int x, HoverTarget target, Color hoverTint) {
        Rectangle bounds = new Rectangle(x, 0, Dim.WINDOW_CTRL_WIDTH, Dim.TITLE_BAR_HEIGHT);
        registerHit(target, bounds);

        Color tint = hoverTarget == target ? hoverTint : Palette.ZINC_500;
        int iconX = x + (Dim.WINDOW_CTRL_WIDTH - Dim.WINDOW_ICON_SIZE) / 2;
        int iconY = (Dim.TITLE_BAR_HEIGHT - Dim.WINDOW_ICON_SIZE) / 2;
        paintSvg(g, windowIcons.get(target), iconX, iconY, Dim.WINDOW_ICON_SIZE, tint, 1f);
    }

    private void paintSvg(Graphics2D g, SvgShape shape, int x, int y, int size, Color color, float alpha) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.SrcOver.derive(alpha * globalAlpha));
        gg.translate(x, y);
        double scale = size / shape.viewBoxWidth();
        gg.scale(scale, scale);
        gg.setColor(color);
        gg.fill(shape.path());
        gg.dispose();
    }

    private void registerHit(HoverTarget target, Rectangle bounds) {
        hitRegions.add(new HitRegion(target, bounds));
    }

    private HoverTarget hitTest(int x, int y) {
        for (HitRegion region : hitRegions) {
            if (region.bounds.contains(x, y)) return region.target;
        }
        return null;
    }

    private static float easedPulse(long elapsed) {
        double t = (elapsed % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        double cosine = (1 + Math.cos(2 * Math.PI * t)) / 2;
        return (float) (PULSE_MIN + (1.0 - PULSE_MIN) * cosine);
    }

    private static Font withTracking(Font base, float tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attrs);
    }

    private record HitRegion(HoverTarget target, Rectangle bounds) {}

    private final class SplashMouseHandler extends MouseAdapter {

        private boolean dragging;
        private HoverTarget pressedTarget;

        @Override
        public void mousePressed(MouseEvent e) {
            pressedTarget = hitTest(e.getX(), e.getY());
            if (pressedTarget == null && titleBarDragRegion.contains(e.getPoint())) {
                actions.dragStart();
                dragging = true;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragging) actions.dragMove();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                actions.dragEnd();
                dragging = false;
            }

            HoverTarget target = hitTest(e.getX(), e.getY());
            if (target != null && target == pressedTarget) dispatch(target);
            pressedTarget = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && hitTest(e.getX(), e.getY()) == null
                && titleBarDragRegion.contains(e.getPoint())) {
                actions.dragDoubleClick();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            HoverTarget target = hitTest(e.getX(), e.getY());
            if (target != hoverTarget) {
                hoverTarget = target;
                setCursor(Cursor.getPredefinedCursor(target == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
                repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverTarget != null) {
                hoverTarget = null;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        }

        private void dispatch(HoverTarget target) {
            switch (target) {
                case MINIMIZE -> actions.minimize();
                case MAXIMIZE -> actions.maximize();
                case CLOSE -> actions.close();
                case HELP -> actions.openHelp();
                case GITHUB -> actions.openGithub();
            }
        }
    }
}
