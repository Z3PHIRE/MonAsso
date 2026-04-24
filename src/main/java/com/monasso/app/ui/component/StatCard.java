package com.monasso.app.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatCard extends VBox {

    private final Label valueLabel;

    public StatCard(String title, String initialValue) {
        getStyleClass().add("stat-card");
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-value");

        getChildren().addAll(titleLabel, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
