package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.OptionalDouble;

public record NumericRange(
    boolean leftExclusive,
    OptionalDouble min,
    boolean rightExclusive,
    OptionalDouble max
) {

    public static final NumericRange UNBOUNDED =
        new NumericRange(false, OptionalDouble.empty(), false, OptionalDouble.empty());

    public boolean contains(double value) {
        if (min.isPresent()) {
            double m = min.getAsDouble();
            if (leftExclusive ? value <= m : value < m) return false;
        }
        if (max.isPresent()) {
            double m = max.getAsDouble();
            if (rightExclusive ? value >= m : value > m) return false;
        }
        return true;
    }

    public boolean isBounded() {
        return min.isPresent() || max.isPresent();
    }
}
