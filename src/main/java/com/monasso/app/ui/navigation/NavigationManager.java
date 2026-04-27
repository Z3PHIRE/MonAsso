package com.monasso.app.ui.navigation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NavigationManager {

    private final StackPane contentPane;
    private final Map<ScreenId, Supplier<Node>> factories = new EnumMap<>(ScreenId.class);
    private final Map<ScreenId, Node> cache = new EnumMap<>(ScreenId.class);
    private final ObjectProperty<ScreenId> currentScreen = new SimpleObjectProperty<>();

    public NavigationManager(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    public void register(ScreenId screenId, Supplier<Node> supplier) {
        factories.put(screenId, supplier);
    }

    public void navigate(ScreenId screenId) {
        if (screenId == null) {
            return;
        }
        if (screenId == currentScreen.get() && !contentPane.getChildren().isEmpty()) {
            return;
        }
        Supplier<Node> supplier = factories.get(screenId);
        if (supplier == null) {
            throw new IllegalArgumentException("Ecran non enregistre: " + screenId);
        }
        Node view = cache.computeIfAbsent(screenId, key -> supplier.get());
        contentPane.getChildren().setAll(view);
        currentScreen.set(screenId);
    }

    public ReadOnlyObjectProperty<ScreenId> currentScreenProperty() {
        return currentScreen;
    }

    public void clearCache() {
        cache.clear();
    }

    public void clearCache(ScreenId... screens) {
        if (screens == null || screens.length == 0) {
            clearCache();
            return;
        }
        List<ScreenId> toClear = java.util.Arrays.stream(screens).filter(java.util.Objects::nonNull).toList();
        if (toClear.isEmpty()) {
            return;
        }
        for (ScreenId screenId : toClear) {
            cache.remove(screenId);
        }
    }

    public ScreenId currentScreen() {
        return currentScreen.get();
    }
}
