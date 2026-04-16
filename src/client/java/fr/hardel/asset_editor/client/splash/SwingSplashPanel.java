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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

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

    private static final int TITLE_BAR_HEIGHT = 32;
    private static final int FRAME_PAD_TOP = 48;
    private static final int FRAME_PAD_SIDE = 24;
    private static final int FRAME_PAD_BOTTOM = 24;
    private static final int FRAME_INNER_PAD = 16;
    private static final int GRID_CELL = 64;
    private static final int CORNER_SIZE = 16;
    private static final int WINDOW_CTRL_WIDTH = 36;
    private static final int LOADING_BOTTOM_OFFSET = 80;
    private static final int DOT_SIZE = 4;
    private static final int DOT_GAP = 4;
    private static final int LOGO_SIZE = 96;
    private static final int TITLE_BAR_LOGO_SIZE = 16;
    private static final int WIN_ICON_SIZE = 12;
    private static final int GITHUB_ICON_SIZE = 16;
    private static final int TITLE_SIZE = 36;
    private static final int TITLE_BAR_TEXT_SIZE = 12;
    private static final int SMALL_TEXT_SIZE = 10;
    private static final int SUBTITLE_SIZE = 12;

    private static final Color GRID_LINE = new Color(0x09090B);
    private static final Color GRID_VIGNETTE = new Color(0, 0, 0, 153);
    private static final Color DASHED_BORDER = new Color(0x151418);
    private static final Color CORNER_COLOR = new Color(0x52525B);
    private static final Color DOT_1 = new Color(0x71717A);
    private static final Color DOT_2 = new Color(0x52525B);
    private static final Color DOT_3 = new Color(0x3F3F46);
    private static final Color ZINC_400 = new Color(0xA1A1AA);
    private static final Color ZINC_500 = new Color(0x71717A);
    private static final Color ZINC_600 = new Color(0x52525B);
    private static final Color ZINC_200 = new Color(0xE4E4E7);
    private static final Color RED_400 = new Color(0xF87171);

    private static final float[] DASH_PATTERN = { 6f, 4f };

    private final SvgShape logoShape;
    private final SvgShape githubShape;
    private final SvgShape minShape;
    private final SvgShape maxShape;
    private final SvgShape closeShape;
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

    private Rectangle titleBarDragRegion = new Rectangle();
    private final Rectangle[] windowControls = { new Rectangle(), new Rectangle(), new Rectangle() };
    private Rectangle helpRegion = new Rectangle();
    private Rectangle githubRegion = new Rectangle();

    private HoverTarget hoverTarget = null;

    private float globalAlpha = 1f;
    private Timer fadeTimer;
    private float frozenLoadingAlpha = 1f;

    private enum HoverTarget {
        MIN, MAX, CLOSE, HELP, GITHUB
    }

    public SwingSplashPanel(Actions actions) {
        this.actions = actions;

        setOpaque(false);
        setBackground(Color.BLACK);
        setLayout(null);

        logoShape = SplashAssets.logo();
        githubShape = SplashAssets.github();
        minShape = SplashAssets.minimize();
        maxShape = SplashAssets.maximize();
        closeShape = SplashAssets.close();

        titleFont = SplashAssets.rubikExtraBold().deriveFont((float) TITLE_SIZE);
        titleBarFont = SplashAssets.rubikMedium().deriveFont((float) TITLE_BAR_TEXT_SIZE);
        subtitleFont = withTracking(SplashAssets.rubikMedium().deriveFont((float) SUBTITLE_SIZE), 0.3f);
        Font monoSmall = new Font(Font.MONOSPACED, Font.PLAIN, SMALL_TEXT_SIZE);
        monoSmallTracked = withTracking(monoSmall, 0.1f);

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
        long pulseElapsed = System.currentTimeMillis() - animStart;
        frozenLoadingAlpha = easedPulse(pulseElapsed);
        long start = System.currentTimeMillis();
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            long fadeElapsed = System.currentTimeMillis() - start;
            if (fadeElapsed >= durationMs) {
                globalAlpha = 0f;
                timer.stop();
                if (fadeTimer == timer)
                    fadeTimer = null;

                repaint();
                SwingUtilities.invokeLater(onComplete);
                return;
            }
            float t = (float) fadeElapsed / durationMs;
            globalAlpha = 1f - t * t * t;
            repaint();
        });
        fadeTimer = timer;
        timer.start();
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2.setComposite(AlphaComposite.SrcOver.derive(globalAlpha));

        int w = getWidth();
        int h = getHeight();
        long elapsed = System.currentTimeMillis() - animStart;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        Rectangle frame = new Rectangle(FRAME_PAD_SIDE, FRAME_PAD_TOP,
            w - 2 * FRAME_PAD_SIDE, h - FRAME_PAD_TOP - FRAME_PAD_BOTTOM);

        paintGrid(g2, frame);
        paintCenterContent(g2, w, h);
        paintLoadingText(g2, w, h, elapsed);
        paintDashedFrame(g2, frame);
        paintCorners(g2, frame);
        paintFrameStrips(g2, frame);
        paintTitleBar(g2, w);

        g2.dispose();
    }

    private void paintGrid(Graphics2D g, Rectangle frame) {
        Shape previousClip = g.getClip();
        g.setClip(frame);

        g.setColor(GRID_LINE);
        g.setStroke(new BasicStroke(1f));

        for (int x = frame.x; x <= frame.x + frame.width; x += GRID_CELL) {
            float snapped = (float) (double) x + 0.5f;
            g.draw(new java.awt.geom.Line2D.Float(snapped, frame.y, snapped, frame.y + frame.height));
        }
        for (int y = frame.y; y <= frame.y + frame.height; y += GRID_CELL) {
            float snapped = (float) (double) y + 0.5f;
            g.draw(new java.awt.geom.Line2D.Float(frame.x, snapped, frame.x + frame.width, snapped));
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
        Color[] colors = { new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), GRID_VIGNETTE };

        AffineTransform gradientTransform = new AffineTransform();
        gradientTransform.translate(cx, cy);
        gradientTransform.scale(1.0, ry / rx);
        gradientTransform.translate(-cx, -cy);

        RadialGradientPaint paint = new RadialGradientPaint(
            new Point2D.Float(cx, cy), rx, new Point2D.Float(cx, cy),
            fractions, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE,
            MultipleGradientPaint.ColorSpaceType.SRGB, gradientTransform);

        g.setPaint(paint);
        g.fill(frame);
    }

    private void paintCenterContent(Graphics2D g, int w, int h) {
        int centerX = w / 2;
        int centerY = h / 2;

        FontMetrics titleMetrics = g.getFontMetrics(titleFont);
        FontMetrics subtitleMetrics = g.getFontMetrics(subtitleFont);
        int titleHeight = titleMetrics.getAscent() + titleMetrics.getDescent();
        int subtitleHeight = subtitleMetrics.getAscent() + subtitleMetrics.getDescent();

        int blockHeight = LOGO_SIZE + 32 + titleHeight + 4 + subtitleHeight;
        int cursorY = centerY - blockHeight / 2;

        paintLogo(g, centerX - LOGO_SIZE / 2, cursorY);
        cursorY += LOGO_SIZE + 32;

        cursorY += titleMetrics.getAscent();
        paintTitleWithGradient(g, centerX, cursorY, titleMetrics);
        cursorY += titleMetrics.getDescent();

        cursorY += 4;
        cursorY += subtitleMetrics.getAscent();
        g.setFont(subtitleFont);
        g.setColor(ZINC_500);
        int subtitleWidth = subtitleMetrics.stringWidth(splashSubtitle);
        g.drawString(splashSubtitle, centerX - subtitleWidth / 2, cursorY);
    }

    private void paintTitleWithGradient(Graphics2D g, int centerX, int baselineY, FontMetrics metrics) {
        int width = metrics.stringWidth(splashTitle);
        int x = centerX - width / 2;
        int top = baselineY - metrics.getAscent();
        int bottom = baselineY + metrics.getDescent();

        g.setFont(titleFont);
        g.setPaint(new GradientPaint(0, top, Color.WHITE, 0, bottom, ZINC_400));
        g.drawString(splashTitle, x, baselineY);
    }

    private void paintLogo(Graphics2D g, int x, int y) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(x, y);
        double scale = SwingSplashPanel.LOGO_SIZE / logoShape.viewBoxWidth();
        gg.scale(scale, scale);
        gg.setColor(Color.WHITE);
        gg.fill(logoShape.path());
        gg.dispose();
    }

    private void paintLoadingText(Graphics2D g, int w, int h, long elapsed) {
        float alpha = fadeTimer != null ? frozenLoadingAlpha : easedPulse(elapsed);
        FontMetrics metrics = g.getFontMetrics(monoSmallTracked);
        int textWidth = metrics.stringWidth(splashLoading);
        int baseline = h - LOADING_BOTTOM_OFFSET - metrics.getDescent();

        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.SrcOver.derive(alpha * globalAlpha));
        gg.setFont(monoSmallTracked);
        gg.setColor(ZINC_400);
        gg.drawString(splashLoading, (w - textWidth) / 2, baseline);
        gg.dispose();
    }

    private void paintDashedFrame(Graphics2D g, Rectangle frame) {
        g.setColor(DASHED_BORDER);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASH_PATTERN, 0f));
        g.draw(new Rectangle2D.Float(frame.x + 0.5f, frame.y + 0.5f, frame.width - 1f, frame.height - 1f));
    }

    private void paintCorners(Graphics2D g, Rectangle frame) {
        g.setColor(CORNER_COLOR);
        g.setStroke(new BasicStroke(1f));

        int x1 = frame.x - 1;
        int y1 = frame.y - 1;
        int x2 = frame.x + frame.width + 1;
        int y2 = frame.y + frame.height + 1;

        drawCorner(g, x1, y1, true, true);
        drawCorner(g, x2 - CORNER_SIZE, y1, true, false);
        drawCorner(g, x1, y2 - CORNER_SIZE, false, true);
        drawCorner(g, x2 - CORNER_SIZE, y2 - CORNER_SIZE, false, false);
    }

    private void drawCorner(Graphics2D g, int x, int y, boolean top, boolean left) {
        float cx = x + 0.5f;
        float cy = y + 0.5f;
        float endX = x + CORNER_SIZE;
        float endY = y + CORNER_SIZE;

        if (top)
            g.draw(new java.awt.geom.Line2D.Float(cx, cy, endX, cy));
        else
            g.draw(new java.awt.geom.Line2D.Float(cx, endY - 0.5f, endX, endY - 0.5f));

        if (left)
            g.draw(new java.awt.geom.Line2D.Float(cx, cy, cx, endY));
        else
            g.draw(new java.awt.geom.Line2D.Float(endX - 0.5f, cy, endX - 0.5f, endY));
    }

    private void paintFrameStrips(Graphics2D g, Rectangle frame) {
        int innerX = frame.x + FRAME_INNER_PAD;
        int innerY = frame.y + FRAME_INNER_PAD;
        int innerW = frame.width - 2 * FRAME_INNER_PAD;
        int innerBottom = frame.y + frame.height - FRAME_INNER_PAD;

        paintFrameTop(g, innerX, innerY, innerW);
        paintFrameBottom(g, innerX, innerBottom, innerW);
    }

    private void paintFrameTop(Graphics2D g, int x, int y, int width) {
        int dotY = y + (TITLE_BAR_TEXT_SIZE - DOT_SIZE) / 2 + 2;
        drawDot(g, x, dotY, DOT_1);
        drawDot(g, x + DOT_SIZE + DOT_GAP, dotY, DOT_2);
        drawDot(g, x + 2 * (DOT_SIZE + DOT_GAP), dotY, DOT_3);

        g.setFont(monoSmallTracked);
        g.setColor(ZINC_600);
        FontMetrics m = g.getFontMetrics(monoSmallTracked);
        int textWidth = m.stringWidth(buildVersion);
        int baseline = y + m.getAscent();
        g.drawString(buildVersion, x + width - textWidth, baseline);
    }

    private void paintFrameBottom(Graphics2D g, int x, int bottomY, int width) {
        g.setFont(monoSmallTracked);
        FontMetrics m = g.getFontMetrics(monoSmallTracked);
        int helpWidth = m.stringWidth(splashHelp);
        int helpHeight = m.getAscent() + m.getDescent();
        int baseline = bottomY - m.getDescent();

        g.setColor(hoverTarget == HoverTarget.HELP ? ZINC_400 : ZINC_600);
        g.drawString(splashHelp, x, baseline);
        helpRegion = new Rectangle(x, baseline - m.getAscent(), helpWidth, helpHeight);

        int iconX = x + width - GITHUB_ICON_SIZE;
        int iconY = bottomY - GITHUB_ICON_SIZE;
        float alpha = hoverTarget == HoverTarget.GITHUB ? 0.7f : 1f;
        paintSvg(g, githubShape, iconX, iconY, GITHUB_ICON_SIZE, Color.WHITE, alpha);
        githubRegion = new Rectangle(iconX, iconY, GITHUB_ICON_SIZE, GITHUB_ICON_SIZE);
    }

    private void drawDot(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRect(x, y, DOT_SIZE, DOT_SIZE);
    }

    private void paintTitleBar(Graphics2D g, int w) {
        int logoX = 12;
        int logoY = (TITLE_BAR_HEIGHT - TITLE_BAR_LOGO_SIZE) / 2;
        paintSvg(g, logoShape, logoX, logoY, TITLE_BAR_LOGO_SIZE, Color.WHITE, 1f);

        int titleX = logoX + TITLE_BAR_LOGO_SIZE + 8;
        g.setFont(titleBarFont);
        FontMetrics m = g.getFontMetrics(titleBarFont);
        int baseline = (TITLE_BAR_HEIGHT - m.getHeight()) / 2 + m.getAscent();
        g.setColor(ZINC_400);
        g.drawString(appTitle, titleX, baseline);

        int controlsStart = w - 3 * WINDOW_CTRL_WIDTH;
        titleBarDragRegion = new Rectangle(0, 0, controlsStart, TITLE_BAR_HEIGHT);

        paintWindowControl(g, controlsStart, minShape, ZINC_200, HoverTarget.MIN, 0);
        paintWindowControl(g, controlsStart + WINDOW_CTRL_WIDTH, maxShape, ZINC_200, HoverTarget.MAX, 1);
        paintWindowControl(g, controlsStart + 2 * WINDOW_CTRL_WIDTH, closeShape, RED_400, HoverTarget.CLOSE, 2);
    }

    private void paintWindowControl(Graphics2D g, int x, SvgShape shape,
                                    Color hoverTint, HoverTarget target, int index) {
        windowControls[index] = new Rectangle(x, 0, WINDOW_CTRL_WIDTH, TITLE_BAR_HEIGHT);
        Color tint = hoverTarget == target ? hoverTint : ZINC_500;
        int iconX = x + (WINDOW_CTRL_WIDTH - WIN_ICON_SIZE) / 2;
        int iconY = (TITLE_BAR_HEIGHT - WIN_ICON_SIZE) / 2;
        paintSvg(g, shape, iconX, iconY, WIN_ICON_SIZE, tint, 1f);
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

    private static float easedPulse(long elapsed) {
        double t = (elapsed % (long) 3000) / (double) (long) 3000;
        double cosine = (1 + Math.cos(2 * Math.PI * t)) / 2;
        return (float) ((float) 0.3 + ((float) 1.0 - (float) 0.3) * cosine);
    }

    private static Font withTracking(Font base, float tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attrs);
    }

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
            if (dragging)
                actions.dragMove();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                actions.dragEnd();
                dragging = false;
            }

            HoverTarget target = hitTest(e.getX(), e.getY());
            if (target != null && target == pressedTarget) {
                switch (target) {
                    case MIN -> actions.minimize();
                    case MAX -> actions.maximize();
                    case CLOSE -> actions.close();
                    case HELP -> actions.openHelp();
                    case GITHUB -> actions.openGithub();
                }
            }
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

        private HoverTarget hitTest(int x, int y) {
            for (int i = 0; i < windowControls.length; i++) {
                if (windowControls[i].contains(x, y)) {
                    return switch (i) {
                        case 0 -> HoverTarget.MIN;
                        case 1 -> HoverTarget.MAX;
                        default -> HoverTarget.CLOSE;
                    };
                }
            }
            if (helpRegion.contains(x, y))
                return HoverTarget.HELP;
            if (githubRegion.contains(x, y))
                return HoverTarget.GITHUB;
            return null;
        }
    }
}
