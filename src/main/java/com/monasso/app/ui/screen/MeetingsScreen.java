package com.monasso.app.ui.screen;

import com.monasso.app.model.Event;
import com.monasso.app.service.EventService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class MeetingsScreen extends VBox {

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm");

    private final EventService eventService;
    private final ObservableList<Event> meetings = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final CheckBox upcomingOnlyCheck = new CheckBox("Afficher uniquement les reunions a venir");
    private final Label tableSummary = new Label();

    private final TextField titleField = new TextField();
    private final TextField dateField = new TextField(LocalDate.now().toString());
    private final TextField timeField = new TextField("18:30");
    private final TextField locationField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    private final TableView<Event> meetingsTable = createMeetingsTable();
    private long editingMeetingId = -1L;

    public MeetingsScreen(EventService eventService) {
        this.eventService = eventService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Reunions");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Vue simplifiee des reunions pour preparer et suivre les points d'organisation.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        VBox.setVgrow(meetingsTable, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                tableSummary,
                meetingsTable,
                createFormPanel()
        );

        refreshMeetings();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Filtrer par titre, lieu ou description");
        searchField.setOnAction(event -> refreshMeetings());
        upcomingOnlyCheck.setSelected(true);
        upcomingOnlyCheck.setOnAction(event -> refreshMeetings());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshMeetings());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser filtres");
        resetItem.setOnAction(event -> {
            searchField.clear();
            upcomingOnlyCheck.setSelected(true);
            refreshMeetings();
        });
        moreButton.getItems().add(resetItem);

        HBox row = new HBox(10, new Label("Recherche"), searchField, upcomingOnlyCheck, applyButton, moreButton);
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Creer ou modifier une reunion");
        section.getStyleClass().add("section-label");

        titleField.setPromptText("Sujet de reunion");
        dateField.setPromptText("Date YYYY-MM-DD");
        timeField.setPromptText("Heure HH:mm");
        locationField.setPromptText("Lieu");
        descriptionArea.setPromptText("Ordre du jour / notes");
        descriptionArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Sujet *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date *"), 2, 0);
        grid.add(dateField, 3, 0);
        grid.add(new Label("Heure *"), 0, 1);
        grid.add(timeField, 1, 1);
        grid.add(new Label("Lieu"), 2, 1);
        grid.add(locationField, 3, 1);
        grid.add(new Label("Description"), 0, 2);
        grid.add(descriptionArea, 1, 2, 3, 1);

        Button createButton = new Button("Creer reunion");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createMeeting());

        Button updateButton = new Button("Modifier reunion");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateMeeting());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Nouveau formulaire");
        clearItem.setOnAction(event -> clearForm());
        MenuItem deleteItem = new MenuItem("Supprimer la selection");
        deleteItem.setOnAction(event -> deleteSelectedMeeting());
        moreButton.getItems().addAll(clearItem, deleteItem);

        HBox actions = new HBox(10, createButton, updateButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        return panel;
    }

    private TableView<Event> createMeetingsTable() {
        TableView<Event> table = new TableView<>(meetings);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Event, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(70);

        TableColumn<Event, String> titleColumn = new TableColumn<>("Reunion");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cleanMeetingTitle(cell.getValue().title())));
        titleColumn.setPrefWidth(220);

        TableColumn<Event, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().eventDate().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<Event, String> timeColumn = new TableColumn<>("Heure");
        timeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(TIME_DISPLAY.format(cell.getValue().eventTime())));
        timeColumn.setPrefWidth(90);

        TableColumn<Event, String> locationColumn = new TableColumn<>("Lieu");
        locationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().location())));
        locationColumn.setPrefWidth(190);

        TableColumn<Event, String> participantsColumn = new TableColumn<>("Participants");
        participantsColumn.setCellValueFactory(cell -> {
            Event event = cell.getValue();
            String label = event.capacity() == null
                    ? String.valueOf(event.participantCount())
                    : event.participantCount() + "/" + event.capacity();
            return new javafx.beans.property.SimpleStringProperty(label);
        });
        participantsColumn.setPrefWidth(120);

        table.getColumns().addAll(idColumn, titleColumn, dateColumn, timeColumn, locationColumn, participantsColumn);

        table.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadMeetingIntoForm(row.getItem());
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadMeetingIntoForm(newValue);
            }
        });
        return table;
    }

    private void refreshMeetings() {
        String query = normalize(searchField.getText());
        List<Event> events = eventService.getEvents("", upcomingOnlyCheck.isSelected());
        meetings.setAll(
                events.stream()
                        .filter(this::isMeeting)
                        .filter(event -> query.isBlank() || normalize(indexableMeetingText(event)).contains(query))
                        .toList()
        );
        meetingsTable.refresh();
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d reunions", meetings.size()));
    }

    private void createMeeting() {
        try {
            eventService.addEvent(
                    toMeetingTitle(titleField.getText()),
                    parseDate(dateField.getText()),
                    parseTime(timeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    null
            );
            AlertUtils.info(getScene().getWindow(), "Reunions", "Reunion creee.");
            clearForm();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void updateMeeting() {
        if (editingMeetingId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez une reunion a modifier.");
            return;
        }
        try {
            Event existing = eventService.getEvent(editingMeetingId);
            eventService.updateEvent(
                    editingMeetingId,
                    toMeetingTitle(titleField.getText()),
                    parseDate(dateField.getText()),
                    parseTime(timeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    existing.capacity()
            );
            AlertUtils.info(getScene().getWindow(), "Reunions", "Reunion modifiee.");
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void deleteSelectedMeeting() {
        Event selected = meetingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez une reunion a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Reunions",
                "Supprimer la reunion \"" + cleanMeetingTitle(selected.title()) + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventService.deleteEvent(selected.id());
            AlertUtils.info(getScene().getWindow(), "Reunions", "Reunion supprimee.");
            clearForm();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void loadMeetingIntoForm(Event event) {
        editingMeetingId = event.id();
        titleField.setText(cleanMeetingTitle(event.title()));
        dateField.setText(event.eventDate().toString());
        timeField.setText(TIME_DISPLAY.format(event.eventTime()));
        locationField.setText(defaultValue(event.location()));
        descriptionArea.setText(defaultValue(event.description()));
    }

    private void clearForm() {
        editingMeetingId = -1L;
        titleField.clear();
        dateField.setText(LocalDate.now().toString());
        timeField.setText("18:30");
        locationField.clear();
        descriptionArea.clear();
        meetingsTable.getSelectionModel().clearSelection();
    }

    private boolean isMeeting(Event event) {
        return normalize(event.title()).contains("reunion");
    }

    private String toMeetingTitle(String rawValue) {
        String safeTitle = rawValue == null ? "" : rawValue.trim();
        if (safeTitle.isEmpty()) {
            throw new IllegalArgumentException("Le sujet de reunion est obligatoire.");
        }
        if (normalize(safeTitle).startsWith("reunion")) {
            return safeTitle;
        }
        return "Reunion - " + safeTitle;
    }

    private String cleanMeetingTitle(String title) {
        String safe = defaultValue(title).trim();
        if (normalize(safe).startsWith("reunion - ")) {
            return safe.substring(safe.indexOf('-') + 1).trim();
        }
        return safe;
    }

    private String indexableMeetingText(Event event) {
        return String.join(" ",
                defaultValue(event.title()),
                defaultValue(event.location()),
                defaultValue(event.description())
        );
    }

    private String normalize(String value) {
        String safe = value == null ? "" : value;
        return Normalizer.normalize(safe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private LocalDate parseDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("La date est obligatoire (format YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide. Format attendu: YYYY-MM-DD.");
        }
    }

    private LocalTime parseTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("L'heure est obligatoire (format HH:mm).");
        }
        try {
            return LocalTime.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Heure invalide. Format attendu: HH:mm.");
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
