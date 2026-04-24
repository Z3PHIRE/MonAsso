package com.monasso.app.ui.screen;

import com.monasso.app.model.AppSetting;
import com.monasso.app.service.SettingsService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingsScreen extends VBox {

    private final SettingsService settingsService;
    private final ObservableList<AppSetting> settings = FXCollections.observableArrayList();

    private final TextField keyField = new TextField();
    private final TextField valueField = new TextField();

    public SettingsScreen(SettingsService settingsService) {
        this.settingsService = settingsService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Parametres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Gestion locale des parametres applicatifs sauvegardes en SQLite.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox formPanel = createFormPanel();
        TableView<AppSetting> tableView = createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(title, subtitle, formPanel, tableView);
        refreshSettings(tableView);
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label formTitle = new Label("Ajouter / modifier un parametre");
        formTitle.getStyleClass().add("section-label");

        keyField.setPromptText("Cle (ex: association.city)");
        valueField.setPromptText("Valeur");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Cle"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Valeur"), 2, 0);
        grid.add(valueField, 3, 0);

        Button saveButton = new Button("Enregistrer");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(event -> saveSetting());

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshSettings(getTableFromParent()));

        HBox actions = new HBox(10, saveButton, refreshButton);
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private TableView<AppSetting> createTable() {
        TableView<AppSetting> tableView = new TableView<>(settings);

        TableColumn<AppSetting, String> keyColumn = new TableColumn<>("Cle");
        keyColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().key()));
        keyColumn.setPrefWidth(280);

        TableColumn<AppSetting, String> valueColumn = new TableColumn<>("Valeur");
        valueColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().value()));
        valueColumn.setPrefWidth(320);

        TableColumn<AppSetting, String> updatedAtColumn = new TableColumn<>("Mis a jour");
        updatedAtColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().updatedAt()));
        updatedAtColumn.setPrefWidth(220);

        tableView.getColumns().addAll(keyColumn, valueColumn, updatedAtColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tableView;
    }

    private void saveSetting() {
        try {
            settingsService.saveSetting(keyField.getText(), valueField.getText());
            keyField.clear();
            valueField.clear();
            refreshSettings(getTableFromParent());
            AlertUtils.info(getScene().getWindow(), "Parametres", "Parametre enregistre.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void refreshSettings(TableView<AppSetting> tableView) {
        settings.setAll(settingsService.getAllSettings());
        if (tableView != null) {
            tableView.refresh();
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<AppSetting> getTableFromParent() {
        for (var child : getChildren()) {
            if (child instanceof TableView<?> table) {
                return (TableView<AppSetting>) table;
            }
        }
        return null;
    }
}
