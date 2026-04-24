package com.monasso.app.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public final class PictogramFactory {

    private PictogramFactory() {
    }

    public static StackPane createBadge(String label, String rootStyleClass, String textStyleClass) {
        StackPane container = new StackPane();
        container.getStyleClass().add(rootStyleClass);
        container.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(label);
        iconLabel.getStyleClass().add(textStyleClass);

        container.getChildren().add(iconLabel);
        return container;
    }
}
