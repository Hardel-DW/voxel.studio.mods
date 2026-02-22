package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import net.minecraft.resources.Identifier;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * Renders an SVG resource file as a fixed-size JavaFX node.
 * Supports: path, rect, line, polyline — with fill and/or stroke.
 */
public final class SvgIcon extends Pane {

    public SvgIcon(String webPath, double size, Paint fill) {
        this(Identifier.fromNamespaceAndPath("asset_editor", normalize(webPath)), size, fill);
    }

    public SvgIcon(Identifier location, double size, Paint fill) {
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);
        setClip(new Rectangle(size, size));

        try (InputStream is = ResourceLoader.open(location)) {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            var svg = doc.getDocumentElement();
            double[] vb = parseViewBox(svg.getAttribute("viewBox"));
            double scale = size / Math.max(vb[2], vb[3]);
            SvgStyle inherited = SvgStyle.from(svg);

            NodeList children = svg.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element el)
                    addShape(el, fill, scale, inherited);
            }
        } catch (Exception ignored) {}
    }

    public void setIconFill(Paint fill) {
        for (var child : getChildren()) {
            if (child instanceof SVGPath p) {
                if (!Color.TRANSPARENT.equals(p.getFill())) p.setFill(fill);
                if (p.getStroke() != null && !Color.TRANSPARENT.equals(p.getStroke())) p.setStroke(fill);
            }
        }
    }

    private void addShape(Element el, Paint fill, double scale, SvgStyle inherited) {
        String content = switch (el.getTagName()) {
            case "path" -> el.getAttribute("d");
            case "rect" -> {
                double x = attr(el, "x", 0), y = attr(el, "y", 0);
                double w = attr(el, "width", 0), h = attr(el, "height", 0);
                yield "M%s,%sh%sv%sh%sz".formatted(x, y, w, h, -w);
            }
            case "line" -> "M%s,%sL%s,%s".formatted(
                    el.getAttribute("x1"), el.getAttribute("y1"),
                    el.getAttribute("x2"), el.getAttribute("y2"));
            case "polyline" -> {
                String[] pts = el.getAttribute("points").trim().split("[\\s,]+");
                var sb = new StringBuilder("M").append(pts[0]).append(",").append(pts[1]);
                for (int i = 2; i + 1 < pts.length; i += 2)
                    sb.append("L").append(pts[i]).append(",").append(pts[i + 1]);
                yield sb.toString();
            }
            default -> null;
        };
        if (content == null || content.isEmpty()) return;

        SVGPath path = new SVGPath();
        path.setContent(content);

        SvgStyle style = SvgStyle.resolve(el, inherited);

        path.setFill("none".equals(style.fill()) ? Color.TRANSPARENT : fill);

        if (!style.stroke().isEmpty() && !"none".equals(style.stroke())) {
            path.setStroke(fill);
            path.setStrokeWidth(style.strokeWidth());
            if ("round".equals(style.lineCap())) path.setStrokeLineCap(StrokeLineCap.ROUND);
            if ("square".equals(style.lineCap())) path.setStrokeLineCap(StrokeLineCap.SQUARE);
            if ("round".equals(style.lineJoin())) path.setStrokeLineJoin(StrokeLineJoin.ROUND);
            if ("bevel".equals(style.lineJoin())) path.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        }

        if ("evenodd".equals(el.getAttribute("fill-rule")))
            path.setFillRule(FillRule.EVEN_ODD);

        path.getTransforms().add(new Scale(scale, scale, 0, 0));
        getChildren().add(path);
    }

    private record SvgStyle(String fill, String stroke, double strokeWidth, String lineCap, String lineJoin) {
        static SvgStyle from(Element el) {
            return new SvgStyle(
                    el.getAttribute("fill"),
                    el.getAttribute("stroke"),
                    parseDouble(el.getAttribute("stroke-width"), 2.0),
                    el.getAttribute("stroke-linecap"),
                    el.getAttribute("stroke-linejoin"));
        }

        static SvgStyle resolve(Element el, SvgStyle parent) {
            return new SvgStyle(
                    el.getAttribute("fill").isEmpty() ? parent.fill() : el.getAttribute("fill"),
                    el.getAttribute("stroke").isEmpty() ? parent.stroke() : el.getAttribute("stroke"),
                    el.getAttribute("stroke-width").isEmpty() ? parent.strokeWidth() : parseDouble(el.getAttribute("stroke-width"), parent.strokeWidth()),
                    el.getAttribute("stroke-linecap").isEmpty() ? parent.lineCap() : el.getAttribute("stroke-linecap"),
                    el.getAttribute("stroke-linejoin").isEmpty() ? parent.lineJoin() : el.getAttribute("stroke-linejoin"));
        }

        private static double parseDouble(String s, double def) {
            if (s == null || s.isEmpty()) return def;
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
        }
    }

    private static double attr(Element el, String name, double def) {
        String v = el.getAttribute(name);
        if (v == null || v.isEmpty()) return def;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return def; }
    }

    private static double[] parseViewBox(String attr) {
        if (attr == null || attr.isBlank()) return new double[]{0, 0, 16, 16};
        String[] parts = attr.trim().split("[\\s,]+");
        return new double[]{
                Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]), Double.parseDouble(parts[3])
        };
    }

    private static String normalize(String webPath) {
        if (webPath == null || webPath.isBlank()) {
            throw new IllegalArgumentException("webPath cannot be empty");
        }
        String path = webPath.trim().replace('\\', '/');
        if (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("images/")) return "textures/" + path.substring("images/".length());
        return path;
    }
}



