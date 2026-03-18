package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.application.Platform;
import javafx.beans.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class FxSelectionBindings {

    private final List<Subscription> subscriptions = new ArrayList<>();

    public <S, R> Subscription observe(StoreSelection<S, R> selection, Consumer<? super R> consumer) {
        return observe(selection, consumer, true);
    }

    public <S, R> Subscription observe(StoreSelection<S, R> selection, Consumer<? super R> consumer, boolean emitCurrent) {
        Subscription subscription = selection.subscribe(value -> runOnFx(() -> consumer.accept(value)), emitCurrent);
        subscriptions.add(subscription);
        return subscription;
    }

    public <S, T> Subscription bind(Property<T> property, StoreSelection<S, ? extends T> selection) {
        return observe(selection, value -> {
            if (!Objects.equals(property.getValue(), value))
                property.setValue(value);
        });
    }

    public void dispose() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    private static void runOnFx(Runnable action) {
        if (!VoxelStudioWindow.isUiThreadAvailable() || Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }
}
