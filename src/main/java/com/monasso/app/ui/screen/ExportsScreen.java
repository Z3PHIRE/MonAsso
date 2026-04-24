package com.monasso.app.ui.screen;

import com.monasso.app.service.ExportService;
import com.monasso.app.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.nio.file.Path;

public class ExportsScreen extends VBox {

    private final ExportService exportService;
    private final Label statusLabel = new Label("Selectionnez un type d'export.");

    public ExportsScreen(ExportService exportService) {
        this.exportService = exportService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Exports");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Export CSV local des membres, evenements et cotisations.");
        subtitle.getStyleClass().add("screen-subtitle");

        Button exportMembersButton = new Button("Exporter Membres");
        exportMembersButton.getStyleClass().add("primary-button");
        exportMembersButton.setOnAction(event -> exportMembers());

        Button exportEventsButton = new Button("Exporter Evenements");
        exportEventsButton.getStyleClass().add("primary-button");
        exportEventsButton.setOnAction(event -> exportEvents());

        Button exportContributionsButton = new Button("Exporter Cotisations");
        exportContributionsButton.getStyleClass().add("primary-button");
        exportContributionsButton.setOnAction(event -> exportContributions());

        Button exportAllButton = new Button("Exporter Tout");
        exportAllButton.getStyleClass().add("accent-button");
        exportAllButton.setOnAction(event -> exportAll());

        HBox buttonRow = new HBox(10, exportMembersButton, exportEventsButton, exportContributionsButton, exportAllButton);

        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        Label panelTitle = new Label("Sortie");
        panelTitle.getStyleClass().add("section-label");
        statusLabel.getStyleClass().add("screen-subtitle");
        statusLabel.setWrapText(true);
        panel.getChildren().addAll(panelTitle, statusLabel);

        getChildren().addAll(title, subtitle, buttonRow, panel);
    }

    private void exportMembers() {
        Path directory = chooseDirectory();
        if (directory == null) {
            return;
        }
        Path result = exportService.exportMembers(directory);
        onExportSuccess("Membres exportes: " + result);
    }

    private void exportEvents() {
        Path directory = chooseDirectory();
        if (directory == null) {
            return;
        }
        Path result = exportService.exportEvents(directory);
        onExportSuccess("Evenements exportes: " + result);
    }

    private void exportContributions() {
        Path directory = chooseDirectory();
        if (directory == null) {
            return;
        }
        Path result = exportService.exportContributions(directory);
        onExportSuccess("Cotisations exportees: " + result);
    }

    private void exportAll() {
        Path directory = chooseDirectory();
        if (directory == null) {
            return;
        }
        Path members = exportService.exportMembers(directory);
        Path events = exportService.exportEvents(directory);
        Path contributions = exportService.exportContributions(directory);
        onExportSuccess("Exports termines:\n" + members + "\n" + events + "\n" + contributions);
    }

    private Path chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le dossier d'export");
        var selected = chooser.showDialog(getScene().getWindow());
        return selected == null ? null : selected.toPath();
    }

    private void onExportSuccess(String message) {
        statusLabel.setText(message);
        AlertUtils.info(getScene().getWindow(), "Exports", "Operation terminee.");
    }
}
