package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** Toggle switch: 44×24px rail + 20px knob, matches web Switch component. */
public final class ToggleSwitch extends Pane {

    private static final double WIDTH  = 44;
    private static final double HEIGHT = 24;
    private static final double KNOB_R = 10;
    private static final double PAD    = 2;

    private static final Paint RAIL_ON = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, VoxelColors.SWITCH_RAIL_ON_FROM),
            new Stop(1, VoxelColors.SWITCH_RAIL_ON_TO));
    private static final Color KNOB_ON  = Color.WHITE;

    private final BooleanProperty value = new SimpleBooleanProperty(false);
    private final Timeline knobTransition = new Timeline();
    private final Rectangle rail;
    private final Circle knob;

    public ToggleSwitch() {
        rail = new Rectangle(WIDTH, HEIGHT, VoxelColors.SWITCH_RAIL_OFF);
        rail.setArcWidth(HEIGHT);
        rail.setArcHeight(HEIGHT);

        knob = new Circle(PAD + KNOB_R, HEIGHT / 2, KNOB_R, VoxelColors.SWITCH_KNOB_OFF);

        getChildren().addAll(rail, knob);
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        value.addListener((obs, o, on) -> animateTo(on));
        setOnMouseClicked(e -> { if (!isDisabled()) value.set(!value.get()); });
        setCursor(Cursor.HAND);
        disabledProperty().addListener((obs, wasDisabled, isDisabled) -> setCursor(isDisabled ? Cursor.DEFAULT : Cursor.HAND));
    }

    private void animateTo(boolean on) {
        double targetX = on ? WIDTH - PAD - KNOB_R : PAD + KNOB_R;
        rail.setFill(on ? RAIL_ON : VoxelColors.SWITCH_RAIL_OFF);
        knob.setFill(on ? KNOB_ON : VoxelColors.SWITCH_KNOB_OFF);
        knobTransition.stop();
        knobTransition.getKeyFrames().setAll(new KeyFrame(Duration.millis(200),
                new KeyValue(knob.centerXProperty(), targetX, javafx.animation.Interpolator.EASE_BOTH)));
        knobTransition.playFromStart();
    }

    public BooleanProperty valueProperty() { return value; }
    public boolean isOn() { return value.get(); }
    public void setValue(boolean v) { value.set(v); }
}
