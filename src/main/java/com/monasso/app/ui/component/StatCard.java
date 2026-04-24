package com.monasso.app.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StatCard extends VBox {

    private final Label valueLabel;
    private final Label helperLabel;

    public StatCard(String title, String iconLabel, String initialValue, String helperText) {
        getStyleClass().add("stat-card");
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        var icon = PictogramFactory.createBadge(iconLabel, "metric-icon-badge", "metric-icon-text");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        headerRow.getChildren().addAll(icon, titleLabel);

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-value");

        helperLabel = new Label(helperText);
        helperLabel.getStyleClass().add("stat-helper");
        helperLabel.setWrapText(true);

        getChildren().addAll(headerRow, valueLabel, helperLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setHelperText(String text) {
        helperLabel.setText(text);
    }
}
