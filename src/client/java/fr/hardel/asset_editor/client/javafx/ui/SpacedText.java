package fr.hardel.asset_editor.client.javafx.ui;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Simulates CSS letter-spacing by rendering each character as a separate Text node
 * with HBox spacing, since JavaFX has no native letter-spacing support.
 */
public final class SpacedText extends HBox {

    public SpacedText(String content, Font font, Paint fill, double letterSpacingEm) {
        setSpacing(letterSpacingEm * font.getSize());
        for (char c : content.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setFont(font);
            t.setFill(fill);
            getChildren().add(t);
        }
    }
}
