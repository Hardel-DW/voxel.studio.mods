package fr.hardel.asset_editor.client.javafx.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import net.minecraft.resources.Identifier;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * Renders an SVG resource file as a fixed-size JavaFX node.
 * Supports simple flat SVGs (single color, path-only).
 */
public final class SvgIcon extends Pane {

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

            NodeList paths = doc.getElementsByTagName("path");
            for (int i = 0; i < paths.getLength(); i++) {
                Element el = (Element) paths.item(i);
                String d = el.getAttribute("d");
                if (d.isEmpty()) continue;

                SVGPath path = new SVGPath();
                path.setContent(d);
                path.setFill(fill);
                if ("evenodd".equals(el.getAttribute("fill-rule"))) {
                    path.setFillRule(FillRule.EVEN_ODD);
                }
                path.getTransforms().add(new Scale(scale, scale, 0, 0));
                getChildren().add(path);
            }
        } catch (Exception ignored) {}
    }

    private static double[] parseViewBox(String attr) {
        if (attr == null || attr.isBlank()) return new double[]{0, 0, 16, 16};
        String[] parts = attr.trim().split("[\\s,]+");
        return new double[]{
            Double.parseDouble(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        };
    }
}
