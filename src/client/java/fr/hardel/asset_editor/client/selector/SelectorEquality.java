package fr.hardel.asset_editor.client.selector;

import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface SelectorEquality<T> {

    SelectorEquality<Object> EQUALS = Objects::equals;
    SelectorEquality<Object> IDENTITY = (left, right) -> left == right;
    SelectorEquality<List<?>> LIST_EQUALS = Objects::equals;

    boolean same(T left, T right);

    @SuppressWarnings("unchecked")
    static <T> SelectorEquality<T> equalsEquality() {
        return (SelectorEquality<T>) EQUALS;
    }

    @SuppressWarnings("unchecked")
    static <T> SelectorEquality<T> identityEquality() {
        return (SelectorEquality<T>) IDENTITY;
    }

    @SuppressWarnings("unchecked")
    static <T> SelectorEquality<List<T>> listEquality() {
        return (SelectorEquality<List<T>>) (SelectorEquality<?>) LIST_EQUALS;
    }
}
