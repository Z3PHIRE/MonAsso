package com.monasso.app.ui.screen;

import com.monasso.app.service.ExportService;
import com.monasso.app.service.SettingsService;
import com.monasso.app.util.AlertUtils;
import com.monasso.app.util.DesktopUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.nio.file.Path;

public class ExportsScreen extends VBox {

    private final ExportService exportService;
    private final SettingsService settingsService;

    private Path exportDirectory;
    private final TextField exportPathField = new TextField();
    private final Label statusLabel = new Label("Selectionnez un type d'export.");

    public ExportsScreen(ExportService exportService, SettingsService settingsService) {
        this.exportService = exportService;
        this.settingsService = settingsService;
        this.exportDirectory = settingsService.getExportDirectory();

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Exports");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Export CSV local des membres, evenements et cotisations.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox locationPanel = createLocationPanel();
        HBox buttonRow = createExportActions();
        VBox outputPanel = createOutputPanel();

        getChildren().addAll(title, subtitle, locationPanel, buttonRow, outputPanel);
        refreshLocationField();
    }

    private VBox createLocationPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label label = new Label("Dossier d'export");
        label.getStyleClass().add("section-label");

        exportPathField.setEditable(false);
        exportPathField.setFocusTraversable(false);

        Button chooseButton = new Button("Choisir dossier");
        chooseButton.getStyleClass().add("ghost-button");
        chooseButton.setOnAction(event -> chooseDirectory());

        Button openButton = new Button("Ouvrir dossier");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(event -> openDirectory());

        HBox actions = new HBox(10, chooseButton, openButton);
        panel.getChildren().addAll(label, exportPathField, actions);
        return panel;
    }

    private HBox createExportActions() {
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

        HBox row = new HBox(10, exportMembersButton, exportEventsButton, exportContributionsButton, exportAllButton);
        row.getStyleClass().add("action-row");
        return row;
    }

    private VBox createOutputPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Sortie");
        panelTitle.getStyleClass().add("section-label");
        statusLabel.getStyleClass().add("screen-subtitle");
        statusLabel.setWrapText(true);
        panel.getChildren().addAll(panelTitle, statusLabel);
        return panel;
    }

    private void chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le dossier d'export");
        chooser.setInitialDirectory(exportDirectory.toFile());
        var selected = chooser.showDialog(getScene().getWindow());
        if (selected == null) {
            return;
        }
        exportDirectory = selected.toPath().toAbsolutePath().normalize();
        settingsService.setExportDirectory(exportDirectory);
        refreshLocationField();
        AlertUtils.info(getScene().getWindow(), "Exports", "Dossier d'export mis a jour.");
    }

    private void openDirectory() {
        try {
            DesktopUtils.openDirectory(exportDirectory);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Exports", e.getMessage());
        }
    }

    private void exportMembers() {
        Path result = exportService.exportMembers(exportDirectory);
        onExportSuccess("Membres exportes: " + result);
    }

    private void exportEvents() {
        Path result = exportService.exportEvents(exportDirectory);
        onExportSuccess("Evenements exportes: " + result);
    }

    private void exportContributions() {
        Path result = exportService.exportContributions(exportDirectory);
        onExportSuccess("Cotisations exportees: " + result);
    }

    private void exportAll() {
        Path members = exportService.exportMembers(exportDirectory);
        Path events = exportService.exportEvents(exportDirectory);
        Path contributions = exportService.exportContributions(exportDirectory);
        onExportSuccess("Exports termines:\n" + members + "\n" + events + "\n" + contributions);
    }

    private void onExportSuccess(String message) {
        statusLabel.setText(message);
        AlertUtils.info(getScene().getWindow(), "Exports", "Operation terminee.");
    }

    private void refreshLocationField() {
        exportPathField.setText(exportDirectory.toString());
    }
}
