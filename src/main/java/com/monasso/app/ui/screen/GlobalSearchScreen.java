package com.monasso.app.ui.screen;

import com.monasso.app.model.GlobalSearchResult;
import com.monasso.app.service.GlobalSearchService;
import com.monasso.app.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class GlobalSearchScreen extends VBox {

    private final GlobalSearchService globalSearchService;
    private final Consumer<GlobalSearchResult> openResultHandler;

    private final ObservableList<GlobalSearchResult> results = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final Label summaryLabel = new Label();
    private final Label detailTitle = new Label("Aucun resultat selectionne");
    private final Label detailMeta = new Label();
    private final Label detailDescription = new Label();
    private final Button openButton = new Button("Ouvrir le module");

    private final TableView<GlobalSearchResult> resultsTable = createResultsTable();

    public GlobalSearchScreen(GlobalSearchService globalSearchService, Consumer<GlobalSearchResult> openResultHandler) {
        this.globalSearchService = globalSearchService;
        this.openResultHandler = openResultHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Recherche globale");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Une seule recherche sur les personnes, evenements, reunions et taches.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        summaryLabel.getStyleClass().add("muted-text");
        detailTitle.getStyleClass().add("event-name");
        detailMeta.getStyleClass().add("muted-text");
        detailDescription.getStyleClass().add("screen-subtitle");
        detailDescription.setWrapText(true);

        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        getChildren().addAll(
                title,
                subtitle,
                createSearchPanel(),
                summaryLabel,
                resultsTable,
                createDetailPanel()
        );

        searchField.requestFocus();
    }

    private VBox createSearchPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher une personne, un evenement, une reunion ou une tache");
        searchField.setOnAction(event -> runSearch());

        Button searchButton = new Button("Rechercher");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setOnAction(event -> runSearch());

        Button clearButton = new Button("Effacer");
        clearButton.getStyleClass().add("ghost-button");
        clearButton.setOnAction(event -> {
            searchField.clear();
            results.clear();
            summaryLabel.setText("");
            clearDetails();
        });

        HBox row = new HBox(10, searchField, searchButton, clearButton);
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Resultat selectionne");
        section.getStyleClass().add("section-label");

        openButton.getStyleClass().add("accent-button");
        openButton.setDisable(true);
        openButton.setOnAction(event -> openSelectedResult());

        panel.getChildren().addAll(section, detailTitle, detailMeta, detailDescription, openButton);
        return panel;
    }

    private TableView<GlobalSearchResult> createResultsTable() {
        TableView<GlobalSearchResult> table = new TableView<>(results);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<GlobalSearchResult, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type().label()));
        typeColumn.setPrefWidth(120);

        TableColumn<GlobalSearchResult, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(240);

        TableColumn<GlobalSearchResult, String> subtitleColumn = new TableColumn<>("Resume");
        subtitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().subtitle())));
        subtitleColumn.setPrefWidth(260);

        TableColumn<GlobalSearchResult, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().details())));
        detailsColumn.setPrefWidth(280);

        table.getColumns().add(typeColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(subtitleColumn);
        table.getColumns().add(detailsColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showDetails(newValue));
        table.setRowFactory(tv -> {
            TableRow<GlobalSearchResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openResult(row.getItem());
                }
            });
            return row;
        });
        return table;
    }

    private void runSearch() {
        try {
            results.setAll(globalSearchService.search(searchField.getText()));
            summaryLabel.setText("Resultats: " + results.size());
            if (results.isEmpty()) {
                clearDetails();
            }
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Recherche globale", e.getMessage());
        }
    }

    private void showDetails(GlobalSearchResult result) {
        if (result == null) {
            clearDetails();
            return;
        }
        detailTitle.setText("[" + result.type().label() + "] " + result.title());
        detailMeta.setText(defaultValue(result.subtitle()));
        detailDescription.setText(defaultValue(result.details()));
        openButton.setDisable(false);
    }

    private void clearDetails() {
        detailTitle.setText("Aucun resultat selectionne");
        detailMeta.setText("");
        detailDescription.setText("");
        openButton.setDisable(true);
    }

    private void openSelectedResult() {
        GlobalSearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Recherche globale", "Selectionnez un resultat.");
            return;
        }
        openResult(selected);
    }

    private void openResult(GlobalSearchResult result) {
        if (result == null || openResultHandler == null) {
            return;
        }
        openResultHandler.accept(result);
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
