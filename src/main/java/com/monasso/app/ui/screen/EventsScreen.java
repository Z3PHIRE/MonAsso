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

    public EventsScreen(EventService eventService) {
        this.eventService = eventService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Evenements");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Planifiez les evenements associatifs et suivez leur calendrier.");
        subtitle.getStyleClass().add("screen-subtitle");

        TableView<Event> tableView = createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox formPanel = createFormPanel(tableView);

        getChildren().addAll(title, subtitle, formPanel, tableView);
        refreshEvents(tableView);
    }

    private TableView<Event> createTable() {
        TableView<Event> tableView = new TableView<>(events);

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

        tableView.getColumns().addAll(idColumn, nameColumn, dateColumn, locationColumn, descriptionColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tableView;
    }

    private VBox createFormPanel(TableView<Event> tableView) {
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
        addButton.setOnAction(event -> addEvent(tableView));

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshEvents(tableView));

        HBox actions = new HBox(10, addButton, refreshButton);
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private void addEvent(TableView<Event> tableView) {
        try {
            eventService.addEvent(nameField.getText(), datePicker.getValue(), locationField.getText(), descriptionArea.getText());
            clearForm();
            refreshEvents(tableView);
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement ajoute avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshEvents(TableView<Event> tableView) {
        events.setAll(eventService.getAllEvents());
        tableView.refresh();
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
