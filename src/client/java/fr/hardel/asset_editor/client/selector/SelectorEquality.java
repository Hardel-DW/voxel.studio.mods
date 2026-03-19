package fr.hardel.asset_editor.client.selector;

import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface SelectorEquality<T> {

    boolean same(T left, T right);

    static <T> SelectorEquality<T> equalsEquality() {
        return Objects::equals;
    }

    static <T> SelectorEquality<T> identityEquality() {
        return (left, right) -> left == right;
    }

    static <T> SelectorEquality<List<T>> listEquality() {
        return Objects::equals;
    }
}
