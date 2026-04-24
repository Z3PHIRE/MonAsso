package com.monasso.app.ui.screen;

import com.monasso.app.model.Event;
import com.monasso.app.service.EventService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class EventsScreen extends VBox {

    private final EventService eventService;
    private final ObservableList<Event> events = FXCollections.observableArrayList();

    private final TextField nameField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField locationField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final Label tableSummary = new Label();

    private final TableView<Event> tableView = createTable();

    public EventsScreen(EventService eventService) {
        this.eventService = eventService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Evenements");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Planifiez les evenements associatifs, puis suivez les dates a venir.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox formPanel = createFormPanel();

        tableSummary.getStyleClass().add("muted-text");
        getChildren().addAll(title, subtitle, formPanel, tableSummary, tableView);

        refreshEvents();
    }

    private TableView<Event> createTable() {
        TableView<Event> table = new TableView<>(events);
        table.getStyleClass().add("app-table");

        TableColumn<Event, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(80);

        TableColumn<Event, String> nameColumn = new TableColumn<>("Nom");
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().name()));
        nameColumn.setPrefWidth(220);

        TableColumn<Event, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().eventDate().toString()));
        dateColumn.setPrefWidth(140);

        TableColumn<Event, String> locationColumn = new TableColumn<>("Lieu");
        locationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().location())));
        locationColumn.setPrefWidth(180);

        TableColumn<Event, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().description())));
        descriptionColumn.setPrefWidth(320);

        table.getColumns().addAll(idColumn, nameColumn, dateColumn, locationColumn, descriptionColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label formTitle = new Label("Ajouter un evenement");
        formTitle.getStyleClass().add("section-label");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");

        nameField.setPromptText("Nom de l'evenement");
        locationField.setPromptText("Lieu");
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(2);

        grid.add(new Label("Nom"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Date"), 2, 0);
        grid.add(datePicker, 3, 0);
        grid.add(new Label("Lieu"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Description"), 2, 1);
        grid.add(descriptionArea, 3, 1);

        Button addButton = new Button("Ajouter");
        addButton.getStyleClass().add("accent-button");
        addButton.setOnAction(event -> addEvent());

        Button deleteButton = new Button("Supprimer selection");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteSelectedEvent());

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshEvents());

        HBox actions = new HBox(10, addButton, deleteButton, refreshButton);
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private void addEvent() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Le nom de l'evenement est obligatoire.");
            return;
        }
        if (datePicker.getValue() == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "La date de l'evenement est obligatoire.");
            return;
        }
        try {
            eventService.addEvent(nameField.getText(), datePicker.getValue(), locationField.getText(), descriptionArea.getText());
            clearForm();
            refreshEvents();
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement ajoute avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void deleteSelectedEvent() {
        Event selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un evenement a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Supprimer l'evenement \"" + selected.name() + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventService.deleteEvent(selected.id());
            refreshEvents();
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement supprime.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshEvents() {
        events.setAll(eventService.getAllEvents());
        tableView.refresh();
        long upcoming = eventService.getUpcomingEvents(1000).size();
        tableSummary.setText("Total : " + events.size() + " | Prochains evenements : " + upcoming);
    }

    private void clearForm() {
        nameField.clear();
        datePicker.setValue(LocalDate.now());
        locationField.clear();
        descriptionArea.clear();
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
