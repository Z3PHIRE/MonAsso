package com.monasso.app.ui.screen;

import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.service.EventService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class EventsScreen extends VBox {

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm");

    private final EventService eventService;
    private final MemberService memberService;

    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final ObservableList<Member> participants = FXCollections.observableArrayList();
    private final ObservableList<Member> availableMembers = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final CheckBox upcomingOnlyCheck = new CheckBox("Afficher uniquement les evenements a venir");
    private final Label tableSummary = new Label();

    private final TextField titleField = new TextField();
    private final TextField dateField = new TextField(LocalDate.now().toString());
    private final TextField timeField = new TextField("19:00");
    private final TextField locationField = new TextField();
    private final TextField capacityField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    private final Label detailTitle = new Label("Aucun evenement selectionne");
    private final Label detailMeta = new Label();
    private final TextField participantSearchField = new TextField();
    private final ComboBox<Member> availableMembersCombo = new ComboBox<>();
    private final TableView<Member> participantsTable = createParticipantsTable();

    private final TableView<Event> eventsTable = createEventsTable();
    private long editingEventId = -1L;

    public EventsScreen(EventService eventService, MemberService memberService) {
        this.eventService = eventService;
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Evenements");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Creation, edition, suppression et gestion des participants.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        VBox.setVgrow(participantsTable, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                createEventFormPanel(),
                tableSummary,
                eventsTable,
                createEventDetailPanel()
        );

        refreshEvents();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher un evenement (titre, lieu, description)");
        searchField.setOnAction(event -> refreshEvents());
        upcomingOnlyCheck.setOnAction(event -> refreshEvents());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshEvents());

        Button resetButton = new Button("Reinitialiser");
        resetButton.getStyleClass().add("ghost-button");
        resetButton.setOnAction(event -> {
            searchField.clear();
            upcomingOnlyCheck.setSelected(false);
            refreshEvents();
        });

        HBox row = new HBox(10, new Label("Recherche"), searchField, upcomingOnlyCheck, applyButton, resetButton);
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createEventFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Fiche evenement");
        section.getStyleClass().add("section-label");

        titleField.setPromptText("Titre");
        dateField.setPromptText("Date YYYY-MM-DD");
        timeField.setPromptText("Heure HH:mm");
        locationField.setPromptText("Lieu");
        capacityField.setPromptText("Capacite optionnelle");
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Titre *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date *"), 2, 0);
        grid.add(dateField, 3, 0);
        grid.add(new Label("Heure *"), 0, 1);
        grid.add(timeField, 1, 1);
        grid.add(new Label("Lieu"), 2, 1);
        grid.add(locationField, 3, 1);
        grid.add(new Label("Capacite"), 0, 2);
        grid.add(capacityField, 1, 2);
        grid.add(new Label("Description"), 2, 2);
        grid.add(descriptionArea, 3, 2);

        Button createButton = new Button("Creer");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createEvent());

        Button updateButton = new Button("Modifier");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateEvent());

        Button clearButton = new Button("Nouveau");
        clearButton.getStyleClass().add("ghost-button");
        clearButton.setOnAction(event -> clearForm());

        Button viewButton = new Button("Voir detail");
        viewButton.getStyleClass().add("primary-button");
        viewButton.setOnAction(event -> openSelectedEventDetail());

        Button deleteButton = new Button("Supprimer selection");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteSelectedEvent());

        HBox actions = new HBox(10, createButton, updateButton, clearButton, viewButton, deleteButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        return panel;
    }

    private VBox createEventDetailPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Detail evenement et participants");
        section.getStyleClass().add("section-label");
        detailTitle.getStyleClass().add("event-name");
        detailMeta.getStyleClass().add("muted-text");

        participantSearchField.setPromptText("Filtrer les participants");
        participantSearchField.setOnAction(event -> refreshParticipants());

        availableMembersCombo.setItems(availableMembers);
        availableMembersCombo.setPromptText("Choisir un membre actif");
        availableMembersCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Member member) {
                return member == null ? "" : member.fullName();
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        });

        Button addParticipantButton = new Button("Ajouter participant");
        addParticipantButton.getStyleClass().add("accent-button");
        addParticipantButton.setOnAction(event -> addParticipant());

        Button removeParticipantButton = new Button("Retirer participant");
        removeParticipantButton.getStyleClass().add("danger-button");
        removeParticipantButton.setOnAction(event -> removeParticipant());

        Button refreshParticipantsButton = new Button("Recharger participants");
        refreshParticipantsButton.getStyleClass().add("ghost-button");
        refreshParticipantsButton.setOnAction(event -> refreshParticipants());

        HBox row1 = new HBox(10, participantSearchField, refreshParticipantsButton);
        row1.getStyleClass().add("action-row");
        HBox.setHgrow(participantSearchField, Priority.ALWAYS);

        HBox row2 = new HBox(10, availableMembersCombo, addParticipantButton, removeParticipantButton);
        row2.getStyleClass().add("action-row");
        HBox.setHgrow(availableMembersCombo, Priority.ALWAYS);

        panel.getChildren().addAll(section, detailTitle, detailMeta, row1, row2, participantsTable);
        return panel;
    }

    private TableView<Event> createEventsTable() {
        TableView<Event> table = new TableView<>(events);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Event, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<Event, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(200);

        TableColumn<Event, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().eventDate().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<Event, String> timeColumn = new TableColumn<>("Heure");
        timeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(TIME_DISPLAY.format(cell.getValue().eventTime())));
        timeColumn.setPrefWidth(90);

        TableColumn<Event, String> locationColumn = new TableColumn<>("Lieu");
        locationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().location())));
        locationColumn.setPrefWidth(170);

        TableColumn<Event, String> capacityColumn = new TableColumn<>("Capacite");
        capacityColumn.setCellValueFactory(cell -> {
            Event event = cell.getValue();
            String value = event.capacity() == null
                    ? "Libre (" + event.participantCount() + ")"
                    : event.participantCount() + "/" + event.capacity();
            return new javafx.beans.property.SimpleStringProperty(value);
        });
        capacityColumn.setPrefWidth(120);

        table.getColumns().addAll(idColumn, titleColumn, dateColumn, timeColumn, locationColumn, capacityColumn);

        table.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadEventIntoForm(row.getItem());
                    openSelectedEventDetail();
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadEventIntoForm(newValue);
            }
        });
        return table;
    }

    private TableView<Member> createParticipantsTable() {
        TableView<Member> table = new TableView<>(participants);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Member, String> nameColumn = new TableColumn<>("Participant");
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fullName()));
        nameColumn.setPrefWidth(220);

        TableColumn<Member, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().email())));
        emailColumn.setPrefWidth(200);

        TableColumn<Member, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(100);

        table.getColumns().addAll(nameColumn, emailColumn, statusColumn);
        return table;
    }

    private void refreshEvents() {
        events.setAll(eventService.getEvents(searchField.getText(), upcomingOnlyCheck.isSelected()));
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d evenements", events.size()));
        eventsTable.refresh();
    }

    private void createEvent() {
        try {
            eventService.addEvent(
                    titleField.getText(),
                    parseDate(dateField.getText()),
                    parseTime(timeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    parseCapacity(capacityField.getText())
            );
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement cree.");
            clearForm();
            refreshEvents();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void updateEvent() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un evenement a modifier.");
            return;
        }
        try {
            eventService.updateEvent(
                    editingEventId,
                    titleField.getText(),
                    parseDate(dateField.getText()),
                    parseTime(timeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    parseCapacity(capacityField.getText())
            );
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement modifie.");
            refreshEvents();
            openSelectedEventDetail();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void deleteSelectedEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un evenement a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Supprimer l'evenement \"" + selected.title() + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventService.deleteEvent(selected.id());
            AlertUtils.info(getScene().getWindow(), "Evenements", "Evenement supprime.");
            clearForm();
            clearEventDetail();
            refreshEvents();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void openSelectedEventDetail() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un evenement.");
            return;
        }
        editingEventId = selected.id();
        Event latest = eventService.getEvent(selected.id());
        detailTitle.setText(latest.title());
        String meta = latest.eventDate() + " " + TIME_DISPLAY.format(latest.eventTime())
                + " | " + defaultValue(latest.location())
                + " | participants: " + latest.participantCount()
                + (latest.capacity() == null ? " (capacite libre)" : "/" + latest.capacity());
        detailMeta.setText(meta);
        refreshParticipants();
    }

    private void refreshParticipants() {
        if (editingEventId <= 0) {
            participants.clear();
            availableMembers.clear();
            return;
        }
        participants.setAll(eventService.getParticipants(editingEventId, participantSearchField.getText()));
        availableMembers.setAll(eventService.getAvailableMembersForEvent(editingEventId, ""));
        participantsTable.refresh();
    }

    private void addParticipant() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le detail d'un evenement.");
            return;
        }
        Member selectedMember = availableMembersCombo.getValue();
        if (selectedMember == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un membre a ajouter.");
            return;
        }
        try {
            eventService.addParticipant(editingEventId, selectedMember.id());
            AlertUtils.info(getScene().getWindow(), "Evenements", "Participant ajoute.");
            openSelectedEventDetail();
            refreshEvents();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void removeParticipant() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le detail d'un evenement.");
            return;
        }
        Member selected = participantsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un participant a retirer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Retirer \"" + selected.fullName() + "\" de cet evenement ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventService.removeParticipant(editingEventId, selected.id());
            AlertUtils.info(getScene().getWindow(), "Evenements", "Participant retire.");
            openSelectedEventDetail();
            refreshEvents();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void loadEventIntoForm(Event event) {
        if (event == null) {
            return;
        }
        editingEventId = event.id();
        titleField.setText(event.title());
        dateField.setText(event.eventDate().toString());
        timeField.setText(TIME_DISPLAY.format(event.eventTime()));
        locationField.setText(defaultValue(event.location()));
        capacityField.setText(event.capacity() == null ? "" : String.valueOf(event.capacity()));
        descriptionArea.setText(defaultValue(event.description()));
    }

    private void clearForm() {
        editingEventId = -1L;
        titleField.clear();
        dateField.setText(LocalDate.now().toString());
        timeField.setText("19:00");
        locationField.clear();
        capacityField.clear();
        descriptionArea.clear();
        eventsTable.getSelectionModel().clearSelection();
    }

    private void clearEventDetail() {
        detailTitle.setText("Aucun evenement selectionne");
        detailMeta.setText("");
        participants.clear();
        availableMembers.clear();
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

    private Integer parseCapacity(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("La capacite doit etre superieure a 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Capacite invalide.");
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
