package fr.hardel.asset_editor.client.splash;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

final class SvgPath {

    static Path2D parse(String d) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        Tokenizer tokens = new Tokenizer(d);

        double cx = 0, cy = 0;
        double sx = 0, sy = 0;
        double prevCtrlX = 0, prevCtrlY = 0;
        char prevCmd = 0;
        char current = 0;

        while (tokens.hasMore()) {
            char cmd = tokens.peekIsCommand() ? tokens.readCommand() : current;
            if (cmd == 0) throw new IllegalStateException("SVG path missing command: " + d);
            boolean rel = Character.isLowerCase(cmd);
            char up = Character.toUpperCase(cmd);
            current = (up == 'M') ? (rel ? 'l' : 'L') : cmd;

            switch (up) {
                case 'M' -> {
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel && prevCmd != 0) { x += cx; y += cy; }
                    path.moveTo(x, y);
                    cx = x; cy = y;
                    sx = x; sy = y;
                }
                case 'L' -> {
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) { x += cx; y += cy; }
                    path.lineTo(x, y);
                    cx = x; cy = y;
                }
                case 'H' -> {
                    double x = tokens.readNumber();
                    if (rel) x += cx;
                    path.lineTo(x, cy);
                    cx = x;
                }
                case 'V' -> {
                    double y = tokens.readNumber();
                    if (rel) y += cy;
                    path.lineTo(cx, y);
                    cy = y;
                }
                case 'C' -> {
                    double x1 = tokens.readNumber();
                    double y1 = tokens.readNumber();
                    double x2 = tokens.readNumber();
                    double y2 = tokens.readNumber();
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) {
                        x1 += cx; y1 += cy;
                        x2 += cx; y2 += cy;
                        x += cx; y += cy;
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    prevCtrlX = x2; prevCtrlY = y2;
                    cx = x; cy = y;
                }
                case 'S' -> {
                    double x1, y1;
                    if (Character.toUpperCase(prevCmd) == 'C' || Character.toUpperCase(prevCmd) == 'S') {
                        x1 = 2 * cx - prevCtrlX;
                        y1 = 2 * cy - prevCtrlY;
                    } else {
                        x1 = cx; y1 = cy;
                    }
                    double x2 = tokens.readNumber();
                    double y2 = tokens.readNumber();
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) {
                        x2 += cx; y2 += cy;
                        x += cx; y += cy;
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    prevCtrlX = x2; prevCtrlY = y2;
                    cx = x; cy = y;
                }
                case 'Q' -> {
                    double x1 = tokens.readNumber();
                    double y1 = tokens.readNumber();
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) {
                        x1 += cx; y1 += cy;
                        x += cx; y += cy;
                    }
                    path.quadTo(x1, y1, x, y);
                    prevCtrlX = x1; prevCtrlY = y1;
                    cx = x; cy = y;
                }
                case 'T' -> {
                    double x1, y1;
                    if (Character.toUpperCase(prevCmd) == 'Q' || Character.toUpperCase(prevCmd) == 'T') {
                        x1 = 2 * cx - prevCtrlX;
                        y1 = 2 * cy - prevCtrlY;
                    } else {
                        x1 = cx; y1 = cy;
                    }
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) { x += cx; y += cy; }
                    path.quadTo(x1, y1, x, y);
                    prevCtrlX = x1; prevCtrlY = y1;
                    cx = x; cy = y;
                }
                case 'A' -> {
                    double rx = tokens.readNumber();
                    double ry = tokens.readNumber();
                    double rot = tokens.readNumber();
                    boolean largeArc = tokens.readFlag();
                    boolean sweep = tokens.readFlag();
                    double x = tokens.readNumber();
                    double y = tokens.readNumber();
                    if (rel) { x += cx; y += cy; }
                    appendArc(path, cx, cy, rx, ry, rot, largeArc, sweep, x, y);
                    cx = x; cy = y;
                }
                case 'Z' -> {
                    path.closePath();
                    cx = sx; cy = sy;
                }
                default -> throw new IllegalStateException("Unsupported SVG command: " + cmd);
            }

            prevCmd = cmd;
        }

        return path;
    }

    private static void appendArc(Path2D path, double x1, double y1, double rx, double ry,
                                  double rotDeg, boolean largeArc, boolean sweep, double x2, double y2) {
        if (rx == 0 || ry == 0) {
            path.lineTo(x2, y2);
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotDeg);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx = (x1 - x2) / 2.0;
        double dy = (y1 - y2) / 2.0;
        double x1p = cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;

        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) {
            double s = Math.sqrt(lambda);
            rx *= s;
            ry *= s;
        }

        double sign = (largeArc == sweep) ? -1 : 1;
        double numer = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double denom = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double coef = sign * Math.sqrt(Math.max(0, numer / denom));
        double cxp = coef * (rx * y1p) / ry;
        double cyp = coef * -(ry * x1p) / rx;

        double arcCenterX = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2.0;
        double arcCenterY = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2.0;

        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;

        double theta = angle(1, 0, ux, uy);
        double delta = angle(ux, uy, vx, vy);

        if (!sweep && delta > 0) delta -= 2 * Math.PI;
        else if (sweep && delta < 0) delta += 2 * Math.PI;

        Arc2D arc = new Arc2D.Double(
            arcCenterX - rx, arcCenterY - ry, 2 * rx, 2 * ry,
            -Math.toDegrees(theta), -Math.toDegrees(delta), Arc2D.OPEN);

        if (rotDeg != 0) {
            AffineTransform t = AffineTransform.getRotateInstance(phi, arcCenterX, arcCenterY);
            path.append(t.createTransformedShape(arc).getPathIterator(null, 0.001), true);
        } else {
            path.append(arc.getPathIterator(null, 0.001), true);
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double ang = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        if (ux * vy - uy * vx < 0) ang = -ang;
        return ang;
    }

    private static final class Tokenizer {
        private final String src;
        private int i;

        Tokenizer(String src) {
            this.src = src;
            this.i = 0;
            skipSeparators();
        }

        boolean hasMore() {
            return i < src.length();
        }

        boolean peekIsCommand() {
            if (i >= src.length()) return false;
            char c = src.charAt(i);
            return Character.isLetter(c);
        }

        char readCommand() {
            char c = src.charAt(i++);
            skipSeparators();
            return c;
        }

        double readNumber() {
            skipSeparators();
            int start = i;
            if (i < src.length() && (src.charAt(i) == '+' || src.charAt(i) == '-')) i++;
            while (i < src.length() && Character.isDigit(src.charAt(i))) i++;
            if (i < src.length() && src.charAt(i) == '.') {
                do i++;
                while (i < src.length() && Character.isDigit(src.charAt(i)));
            }
            if (i < src.length() && (src.charAt(i) == 'e' || src.charAt(i) == 'E')) {
                i++;
                if (i < src.length() && (src.charAt(i) == '+' || src.charAt(i) == '-')) i++;
                while (i < src.length() && Character.isDigit(src.charAt(i))) i++;
            }
            if (start == i) throw new IllegalStateException("SVG path: expected number at " + start);
            return Double.parseDouble(src.substring(start, i));
        }

        boolean readFlag() {
            skipSeparators();
            char c = src.charAt(i++);
            return c == '1';
        }

        private void skipSeparators() {
            while (i < src.length()) {
                char c = src.charAt(i);
                if (Character.isWhitespace(c) || c == ',') i++;
                else break;
            }
        }
    }

    private SvgPath() {}
}
