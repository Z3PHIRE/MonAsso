package com.monasso.app.ui.screen;

import com.monasso.app.service.ExportService;
import com.monasso.app.service.SettingsService;
import com.monasso.app.util.AlertUtils;
import com.monasso.app.util.DesktopUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

        Label subtitle = new Label("Exports CSV, XLSX et PDF vers un dossier local configurable.");
        subtitle.getStyleClass().add("screen-subtitle");

        getChildren().addAll(
                title,
                subtitle,
                createLocationPanel(),
                createExportsTabs(),
                createOutputPanel()
        );
        refreshLocationField();
    }

    private TabPane createExportsTabs() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("light-tabs");

        Tab csvTab = new Tab("CSV", createCsvPanel());
        csvTab.setClosable(false);
        Tab xlsxTab = new Tab("Excel", createXlsxPanel());
        xlsxTab.setClosable(false);
        Tab pdfTab = new Tab("PDF", createPdfPanel());
        pdfTab.setClosable(false);

        tabPane.getTabs().addAll(csvTab, xlsxTab, pdfTab);
        return tabPane;
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
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(label, exportPathField, actions);
        return panel;
    }

    private VBox createCsvPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Exports CSV");
        panelTitle.getStyleClass().add("section-label");

        Button membersButton = new Button("Membres CSV");
        membersButton.getStyleClass().add("primary-button");
        membersButton.setOnAction(event -> runExport(() -> exportService.exportMembersCsv(exportDirectory), "Membres CSV exportes"));

        Button eventsButton = new Button("Evenements CSV");
        eventsButton.getStyleClass().add("primary-button");
        eventsButton.setOnAction(event -> runExport(() -> exportService.exportEventsCsv(exportDirectory), "Evenements CSV exportes"));

        Button contributionsButton = new Button("Cotisations CSV");
        contributionsButton.getStyleClass().add("primary-button");
        contributionsButton.setOnAction(event -> runExport(() -> exportService.exportContributionsCsv(exportDirectory), "Cotisations CSV exportees"));

        Button allButton = new Button("Tout CSV");
        allButton.getStyleClass().add("accent-button");
        allButton.setOnAction(event -> exportAllCsv());

        HBox actions = new HBox(10, membersButton, eventsButton, contributionsButton, allButton);
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(panelTitle, actions);
        return panel;
    }

    private VBox createXlsxPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Exports Excel (XLSX)");
        panelTitle.getStyleClass().add("section-label");

        Button membersButton = new Button("Membres XLSX");
        membersButton.getStyleClass().add("primary-button");
        membersButton.setOnAction(event -> runExport(() -> exportService.exportMembersXlsx(exportDirectory), "Membres XLSX exportes"));

        Button eventsButton = new Button("Evenements XLSX");
        eventsButton.getStyleClass().add("primary-button");
        eventsButton.setOnAction(event -> runExport(() -> exportService.exportEventsXlsx(exportDirectory), "Evenements XLSX exportes"));

        Button contributionsButton = new Button("Cotisations XLSX");
        contributionsButton.getStyleClass().add("primary-button");
        contributionsButton.setOnAction(event -> runExport(() -> exportService.exportContributionsXlsx(exportDirectory), "Cotisations XLSX exportees"));

        Button globalButton = new Button("Global XLSX (3 feuilles)");
        globalButton.getStyleClass().add("accent-button");
        globalButton.setOnAction(event -> runExport(() -> exportService.exportGlobalXlsx(exportDirectory), "Export global XLSX termine"));

        HBox actions = new HBox(10, membersButton, eventsButton, contributionsButton, globalButton);
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(panelTitle, actions);
        return panel;
    }

    private VBox createPdfPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Rapports PDF");
        panelTitle.getStyleClass().add("section-label");

        Button membersButton = new Button("Rapport membres");
        membersButton.getStyleClass().add("primary-button");
        membersButton.setOnAction(event -> runExport(() -> exportService.exportMembersPdf(exportDirectory), "Rapport membres genere"));

        Button eventsButton = new Button("Rapport evenements");
        eventsButton.getStyleClass().add("primary-button");
        eventsButton.setOnAction(event -> runExport(() -> exportService.exportEventsPdf(exportDirectory), "Rapport evenements genere"));

        Button contributionsButton = new Button("Rapport cotisations");
        contributionsButton.getStyleClass().add("primary-button");
        contributionsButton.setOnAction(event -> runExport(() -> exportService.exportContributionsPdf(exportDirectory), "Rapport cotisations genere"));

        HBox actions = new HBox(10, membersButton, eventsButton, contributionsButton);
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(panelTitle, actions);
        return panel;
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
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir le dossier d'export");
            if (exportDirectory != null && exportDirectory.toFile().exists()) {
                chooser.setInitialDirectory(exportDirectory.toFile());
            }
            var selected = chooser.showDialog(getScene().getWindow());
            if (selected == null) {
                return;
            }
            exportDirectory = selected.toPath().toAbsolutePath().normalize();
            settingsService.setExportDirectory(exportDirectory);
            refreshLocationField();
            AlertUtils.info(getScene().getWindow(), "Exports", "Dossier d'export mis a jour.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Exports", e.getMessage());
        }
    }

    private void openDirectory() {
        try {
            DesktopUtils.openDirectory(exportDirectory);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Exports", e.getMessage());
        }
    }

    private void exportAllCsv() {
        try {
            Path members = exportService.exportMembersCsv(exportDirectory);
            Path events = exportService.exportEventsCsv(exportDirectory);
            Path contributions = exportService.exportContributionsCsv(exportDirectory);
            onSuccess("Exports CSV termines:\n" + members + "\n" + events + "\n" + contributions);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Exports", e.getMessage());
            statusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private void runExport(ExportOperation exportOperation, String successTitle) {
        try {
            Path target = exportOperation.execute();
            onSuccess(successTitle + ":\n" + target);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Exports", e.getMessage());
            statusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private void onSuccess(String message) {
        statusLabel.setText(message);
        AlertUtils.info(getScene().getWindow(), "Exports", "Operation terminee.");
    }

    private void refreshLocationField() {
        exportPathField.setText(exportDirectory.toString());
    }

    @FunctionalInterface
    private interface ExportOperation {
        Path execute();
    }
}
