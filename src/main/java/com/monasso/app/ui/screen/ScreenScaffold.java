package com.monasso.app.ui.screen;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ScreenScaffold extends VBox {

    public ScreenScaffold(String title, String subtitle, Node... contentNodes) {
        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("screen-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("screen-subtitle");
        subtitleLabel.setWrapText(true);

        getChildren().add(titleLabel);
        getChildren().add(subtitleLabel);
        getChildren().addAll(contentNodes);
    }
}
