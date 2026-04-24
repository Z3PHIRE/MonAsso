package com.monasso.app.ui.screen;

import com.monasso.app.service.SettingsService;
import com.monasso.app.util.AlertUtils;
import com.monasso.app.util.DesktopUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.nio.file.Path;

public class SettingsScreen extends VBox {

    private final SettingsService settingsService;

    private final TextField databasePathField = new TextField();
    private final TextField exportPathField = new TextField();
    private final TextField backupPathField = new TextField();
    private final Label statusLabel = new Label();

    public SettingsScreen(SettingsService settingsService) {
        this.settingsService = settingsService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Parametres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Configurez les chemins locaux utilises par l'application.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        VBox pathsPanel = createPathsPanel();
        VBox actionsPanel = createActionsPanel();

        getChildren().addAll(title, subtitle, pathsPanel, actionsPanel);
        reloadFromSettings();
    }

    private VBox createPathsPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Chemins applicatifs");
        panelTitle.getStyleClass().add("section-label");

        databasePathField.setEditable(false);
        databasePathField.setFocusTraversable(false);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(12);

        addPathRow(grid, 0, "Base SQLite", databasePathField, false);
        addPathRow(grid, 1, "Exports", exportPathField, true);
        addPathRow(grid, 2, "Sauvegardes", backupPathField, true);

        statusLabel.getStyleClass().add("muted-text");
        statusLabel.setWrapText(true);

        panel.getChildren().addAll(panelTitle, grid, statusLabel);
        return panel;
    }

    private VBox createActionsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Actions");
        panelTitle.getStyleClass().add("section-label");

        Button saveButton = new Button("Enregistrer les chemins");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(event -> savePathSettings());

        Button reloadButton = new Button("Recharger");
        reloadButton.getStyleClass().add("ghost-button");
        reloadButton.setOnAction(event -> reloadFromSettings());

        HBox row = new HBox(10, saveButton, reloadButton);
        row.getStyleClass().add("action-row");

        panel.getChildren().addAll(panelTitle, row);
        return panel;
    }

    private void addPathRow(GridPane grid, int rowIndex, String label, TextField field, boolean editable) {
        Label rowLabel = new Label(label);
        rowLabel.getStyleClass().add("field-label");

        field.setEditable(editable);
        HBox.setHgrow(field, Priority.ALWAYS);

        Button chooseButton = new Button("Choisir");
        chooseButton.getStyleClass().add("ghost-button");
        chooseButton.setDisable(!editable);
        chooseButton.setOnAction(event -> chooseFolderForField(field));

        Button openButton = new Button("Ouvrir");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(event -> openFolder(field));

        HBox buttonBox = new HBox(8, chooseButton, openButton);
        buttonBox.getStyleClass().add("action-row");

        grid.add(rowLabel, 0, rowIndex);
        grid.add(field, 1, rowIndex);
        grid.add(buttonBox, 2, rowIndex);
    }

    private void chooseFolderForField(TextField field) {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir un dossier");
            Path current = pathFromField(field);
            if (current != null && current.toFile().exists()) {
                chooser.setInitialDirectory(current.toFile());
            }
            var selected = chooser.showDialog(getScene().getWindow());
            if (selected != null) {
                field.setText(selected.toPath().toAbsolutePath().normalize().toString());
            }
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void openFolder(TextField field) {
        try {
            Path path = pathFromField(field);
            if (path == null) {
                AlertUtils.warning(getScene().getWindow(), "Parametres", "Aucun dossier defini.");
                return;
            }
            Path targetDirectory = path.toFile().isDirectory() ? path : path.getParent();
            if (targetDirectory == null) {
                AlertUtils.warning(getScene().getWindow(), "Parametres", "Impossible de determiner le dossier a ouvrir.");
                return;
            }
            DesktopUtils.openDirectory(targetDirectory);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void savePathSettings() {
        try {
            settingsService.setExportDirectory(pathFromField(exportPathField));
            settingsService.setBackupDirectory(pathFromField(backupPathField));
            reloadFromSettings();
            statusLabel.setText("Chemins enregistres avec succes.");
            AlertUtils.info(getScene().getWindow(), "Parametres", "Configuration enregistree.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void reloadFromSettings() {
        databasePathField.setText(settingsService.getDatabasePath().toString());
        exportPathField.setText(settingsService.getExportDirectory().toString());
        backupPathField.setText(settingsService.getBackupDirectory().toString());
        statusLabel.setText("Configuration chargee.");
    }

    private Path pathFromField(TextField field) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim());
    }
}
