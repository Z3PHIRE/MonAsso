package com.monasso.app.ui.screen;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.ChecklistCategory;
import com.monasso.app.model.ChecklistItem;
import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.service.ChecklistService;
import com.monasso.app.service.MeetingService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class MeetingsScreen extends VBox {

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm");

    private final MeetingService meetingService;
    private final MemberService memberService;
    private final ChecklistService checklistService;

    private final ObservableList<Meeting> meetings = FXCollections.observableArrayList();
    private final ObservableList<Member> availableResponsibleMembers = FXCollections.observableArrayList();
    private final ObservableList<Member> participants = FXCollections.observableArrayList();
    private final ObservableList<Member> availableParticipants = FXCollections.observableArrayList();
    private final ObservableList<ChecklistCategory> checklistCategories = FXCollections.observableArrayList();
    private final ObservableList<ChecklistItem> checklistItems = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final CheckBox upcomingOnlyCheck = new CheckBox("Afficher uniquement les reunions a venir");
    private final ComboBox<ArchiveFilter> archiveFilterCombo = new ComboBox<>();
    private final Label tableSummary = new Label();

    private final TextField titleField = new TextField();
    private final TextField dateField = new TextField(LocalDate.now().toString());
    private final TextField startTimeField = new TextField("18:30");
    private final TextField endTimeField = new TextField("20:00");
    private final TextField locationField = new TextField();
    private final TextField organizerField = new TextField();
    private final ComboBox<Member> responsibleCombo = new ComboBox<>();
    private final ComboBox<ScheduleStatus> statusCombo = new ComboBox<>();
    private final TextField categoryField = new TextField();
    private final TextArea linkedDocumentsArea = new TextArea();
    private final TextArea agendaArea = new TextArea();
    private final TextArea notesArea = new TextArea();

    private final Label detailTitle = new Label("Aucune reunion selectionnee");
    private final Label detailMeta = new Label();
    private final TextField participantSearchField = new TextField();
    private final ComboBox<Member> participantCombo = new ComboBox<>();
    private final ComboBox<ChecklistCategory> checklistCategoryCombo = new ComboBox<>();
    private final TextField checklistItemField = new TextField();

    private final TableView<Meeting> meetingsTable = createMeetingsTable();
    private final TableView<Member> participantsTable = createParticipantsTable();
    private final ListView<ChecklistItem> checklistListView = createChecklistListView();

    private long editingMeetingId = -1L;

    public MeetingsScreen(MeetingService meetingService, MemberService memberService, ChecklistService checklistService) {
        this.meetingService = meetingService;
        this.memberService = memberService;
        this.checklistService = checklistService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Reunions");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Gestion complete des reunions: planning, participants, statuts et checklist.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        VBox.setVgrow(meetingsTable, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");

        TitledPane formPane = new TitledPane("Fiche reunion", createMeetingFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(false);

        TitledPane detailPane = new TitledPane("Detail, participants et checklist", createDetailPanel());
        detailPane.getStyleClass().add("folded-panel");
        detailPane.setExpanded(true);

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                tableSummary,
                meetingsTable,
                formPane,
                detailPane
        );

        loadActiveMembers();
        refreshChecklistCategories();
        refreshMeetings();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher reunion, lieu, organisateur, categorie");
        searchField.setOnAction(event -> refreshMeetings());
        upcomingOnlyCheck.setSelected(true);
        upcomingOnlyCheck.setOnAction(event -> refreshMeetings());
        archiveFilterCombo.getItems().setAll(ArchiveFilter.values());
        archiveFilterCombo.getSelectionModel().select(ArchiveFilter.ACTIVE);
        archiveFilterCombo.setOnAction(event -> refreshMeetings());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshMeetings());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser filtres");
        resetItem.setOnAction(event -> {
            searchField.clear();
            upcomingOnlyCheck.setSelected(true);
            archiveFilterCombo.getSelectionModel().select(ArchiveFilter.ACTIVE);
            refreshMeetings();
        });
        moreButton.getItems().add(resetItem);

        HBox row = new HBox(
                10,
                new Label("Recherche"),
                searchField,
                upcomingOnlyCheck,
                new Label("Archives"),
                archiveFilterCombo,
                applyButton,
                moreButton
        );
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createMeetingFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Edition");
        section.getStyleClass().add("section-label");

        titleField.setPromptText("Titre");
        dateField.setPromptText("Date YYYY-MM-DD");
        startTimeField.setPromptText("Heure debut HH:mm");
        endTimeField.setPromptText("Heure fin HH:mm");
        locationField.setPromptText("Lieu");
        organizerField.setPromptText("Organisateur");

        responsibleCombo.setItems(availableResponsibleMembers);
        responsibleCombo.setPromptText("Responsable");
        responsibleCombo.setConverter(memberConverter());

        statusCombo.getItems().setAll(ScheduleStatus.values());
        statusCombo.getSelectionModel().select(ScheduleStatus.PLANNED);

        categoryField.setPromptText("Categorie");
        linkedDocumentsArea.setPromptText("Documents lies");
        linkedDocumentsArea.setPrefRowCount(2);
        agendaArea.setPromptText("Ordre du jour");
        agendaArea.setPrefRowCount(2);
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Titre *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date *"), 2, 0);
        grid.add(dateField, 3, 0);
        grid.add(new Label("Debut *"), 0, 1);
        grid.add(startTimeField, 1, 1);
        grid.add(new Label("Fin *"), 2, 1);
        grid.add(endTimeField, 3, 1);
        grid.add(new Label("Lieu"), 0, 2);
        grid.add(locationField, 1, 2);
        grid.add(new Label("Organisateur"), 2, 2);
        grid.add(organizerField, 3, 2);
        grid.add(new Label("Responsable"), 0, 3);
        grid.add(responsibleCombo, 1, 3);
        grid.add(new Label("Statut"), 2, 3);
        grid.add(statusCombo, 3, 3);
        grid.add(new Label("Categorie"), 0, 4);
        grid.add(categoryField, 1, 4);
        grid.add(new Label("Documents"), 2, 4);
        grid.add(linkedDocumentsArea, 3, 4);
        grid.add(new Label("Ordre du jour"), 0, 5);
        grid.add(agendaArea, 1, 5, 3, 1);
        grid.add(new Label("Notes"), 0, 6);
        grid.add(notesArea, 1, 6, 3, 1);

        Button createButton = new Button("Creer reunion");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createMeeting());

        Button updateButton = new Button("Modifier reunion");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateMeeting());

        Button archiveButton = new Button("Archiver / desarchiver");
        archiveButton.getStyleClass().add("ghost-button");
        archiveButton.setOnAction(event -> toggleArchiveSelectedMeeting());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Nouveau formulaire");
        clearItem.setOnAction(event -> clearForm());
        MenuItem detailItem = new MenuItem("Voir detail");
        detailItem.setOnAction(event -> openSelectedMeetingDetail());
        MenuItem deleteItem = new MenuItem("Supprimer la selection");
        deleteItem.setOnAction(event -> deleteSelectedMeeting());
        moreButton.getItems().addAll(clearItem, detailItem, deleteItem);

        HBox actions = new HBox(10, createButton, updateButton, archiveButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Detail reunion, participants et checklist");
        section.getStyleClass().add("section-label");
        detailTitle.getStyleClass().add("event-name");
        detailMeta.getStyleClass().add("muted-text");

        participantSearchField.setPromptText("Filtrer les participants");
        participantSearchField.setOnAction(event -> refreshParticipants());

        participantCombo.setItems(availableParticipants);
        participantCombo.setPromptText("Ajouter un participant");
        participantCombo.setConverter(memberConverter());

        Button addParticipantButton = new Button("Ajouter");
        addParticipantButton.getStyleClass().add("accent-button");
        addParticipantButton.setOnAction(event -> addParticipant());

        Button removeParticipantButton = new Button("Retirer");
        removeParticipantButton.getStyleClass().add("danger-button");
        removeParticipantButton.setOnAction(event -> removeParticipant());

        Button refreshParticipantsButton = new Button("Recharger");
        refreshParticipantsButton.getStyleClass().add("ghost-button");
        refreshParticipantsButton.setOnAction(event -> refreshParticipants());

        HBox participantSearchRow = new HBox(10, participantSearchField, refreshParticipantsButton);
        participantSearchRow.getStyleClass().add("action-row");
        HBox.setHgrow(participantSearchField, Priority.ALWAYS);

        HBox participantActionRow = new HBox(10, participantCombo, addParticipantButton, removeParticipantButton);
        participantActionRow.getStyleClass().add("action-row");
        HBox.setHgrow(participantCombo, Priority.ALWAYS);

        Label checklistSection = new Label("Checklist dynamique");
        checklistSection.getStyleClass().add("section-label");

        checklistCategoryCombo.setItems(checklistCategories);
        checklistCategoryCombo.setPromptText("Categorie checklist");
        checklistCategoryCombo.setEditable(true);
        checklistCategoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ChecklistCategory category) {
                return category == null ? "" : category.name();
            }

            @Override
            public ChecklistCategory fromString(String string) {
                return null;
            }
        });
        checklistItemField.setPromptText("Nouvel item checklist");

        Button addChecklistItemButton = new Button("Ajouter item");
        addChecklistItemButton.getStyleClass().add("accent-button");
        addChecklistItemButton.setOnAction(event -> addChecklistItem());

        Button refreshChecklistButton = new Button("Recharger checklist");
        refreshChecklistButton.getStyleClass().add("ghost-button");
        refreshChecklistButton.setOnAction(event -> refreshChecklist());

        HBox checklistRow = new HBox(10, checklistCategoryCombo, checklistItemField, addChecklistItemButton, refreshChecklistButton);
        checklistRow.getStyleClass().add("action-row");
        HBox.setHgrow(checklistCategoryCombo, Priority.ALWAYS);
        HBox.setHgrow(checklistItemField, Priority.ALWAYS);

        panel.getChildren().addAll(
                section,
                detailTitle,
                detailMeta,
                participantSearchRow,
                participantActionRow,
                participantsTable,
                checklistSection,
                checklistRow,
                checklistListView
        );
        return panel;
    }

    private TableView<Meeting> createMeetingsTable() {
        TableView<Meeting> table = new TableView<>(meetings);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Meeting, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<Meeting, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(190);

        TableColumn<Meeting, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().meetingDate().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<Meeting, String> timeColumn = new TableColumn<>("Horaire");
        timeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                TIME_DISPLAY.format(cell.getValue().startTime()) + " - " + TIME_DISPLAY.format(cell.getValue().endTime())
        ));
        timeColumn.setPrefWidth(140);

        TableColumn<Meeting, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(110);

        TableColumn<Meeting, String> responsibleColumn = new TableColumn<>("Responsable");
        responsibleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().responsibleName())));
        responsibleColumn.setPrefWidth(150);

        TableColumn<Meeting, Number> participantsColumn = new TableColumn<>("Participants");
        participantsColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().participantCount()));
        participantsColumn.setPrefWidth(100);

        TableColumn<Meeting, String> archivedColumn = new TableColumn<>("Archive");
        archivedColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().archived() ? "Oui" : "Non"));
        archivedColumn.setPrefWidth(90);

        table.getColumns().addAll(idColumn, titleColumn, dateColumn, timeColumn, statusColumn, responsibleColumn, participantsColumn, archivedColumn);
        table.setRowFactory(tv -> {
            TableRow<Meeting> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadMeetingIntoForm(row.getItem());
                    openSelectedMeetingDetail();
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

    private ListView<ChecklistItem> createChecklistListView() {
        ListView<ChecklistItem> listView = new ListView<>(checklistItems);
        listView.setPrefHeight(170);
        listView.setCellFactory(ignored -> new ListCell<>() {
            private final CheckBox doneCheckBox = new CheckBox();
            private final Label label = new Label();
            private final Label category = new Label();
            private final Button deleteButton = new Button("Supprimer");
            private final HBox row = new HBox(8, doneCheckBox, label, category, deleteButton);

            {
                row.getStyleClass().add("action-row");
                category.getStyleClass().add("muted-text");
                HBox.setHgrow(label, Priority.ALWAYS);
                deleteButton.getStyleClass().add("ghost-button");

                doneCheckBox.setOnAction(event -> {
                    ChecklistItem item = getItem();
                    if (item == null) {
                        return;
                    }
                    try {
                        checklistService.setChecked(item.id(), doneCheckBox.isSelected());
                        refreshChecklist();
                    } catch (Exception e) {
                        AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
                    }
                });
                deleteButton.setOnAction(event -> {
                    ChecklistItem item = getItem();
                    if (item == null) {
                        return;
                    }
                    try {
                        checklistService.deleteItem(item.id());
                        refreshChecklist();
                    } catch (Exception e) {
                        AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(ChecklistItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                doneCheckBox.setSelected(item.checked());
                label.setText(defaultValue(item.label()));
                String categoryName = item.categoryName() == null || item.categoryName().isBlank()
                        ? "Sans categorie"
                        : item.categoryName();
                category.setText("[" + categoryName + "]");
                setGraphic(row);
            }
        });
        return listView;
    }

    private void loadActiveMembers() {
        availableResponsibleMembers.setAll(memberService.getMembers("", MemberStatusFilter.ACTIVE));
    }

    private StringConverter<Member> memberConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Member member) {
                return member == null ? "" : member.fullName();
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        };
    }

    private void refreshChecklistCategories() {
        checklistCategories.setAll(checklistService.getCategories());
    }

    private void refreshMeetings() {
        ArchiveFilter archiveFilter = archiveFilterCombo.getValue() == null ? ArchiveFilter.ACTIVE : archiveFilterCombo.getValue();
        meetings.setAll(meetingService.getMeetings(searchField.getText(), upcomingOnlyCheck.isSelected(), archiveFilter));
        meetingsTable.refresh();
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d reunions (%s)", meetings.size(), archiveFilter.label()));
    }

    private void toggleArchiveSelectedMeeting() {
        Meeting selected = meetingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez une reunion.");
            return;
        }

        boolean archivedTarget = !selected.archived();
        String actionLabel = archivedTarget ? "archiver" : "desarchiver";
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Reunions",
                "Voulez-vous " + actionLabel + " \"" + selected.title() + "\" ?"
        );
        if (!confirmed) {
            return;
        }

        try {
            meetingService.setArchived(selected.id(), archivedTarget);
            AlertUtils.info(getScene().getWindow(), "Reunions", archivedTarget ? "Reunion archivee." : "Reunion desarchivee.");
            clearForm();
            clearDetail();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void createMeeting() {
        try {
            meetingService.addMeeting(
                    titleField.getText(),
                    parseDate(dateField.getText()),
                    parseTime(startTimeField.getText()),
                    parseTime(endTimeField.getText()),
                    locationField.getText(),
                    organizerField.getText(),
                    responsibleCombo.getValue() == null ? null : responsibleCombo.getValue().id(),
                    agendaArea.getText(),
                    notesArea.getText(),
                    statusCombo.getValue(),
                    categoryField.getText(),
                    linkedDocumentsArea.getText()
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
            meetingService.updateMeeting(
                    editingMeetingId,
                    titleField.getText(),
                    parseDate(dateField.getText()),
                    parseTime(startTimeField.getText()),
                    parseTime(endTimeField.getText()),
                    locationField.getText(),
                    organizerField.getText(),
                    responsibleCombo.getValue() == null ? null : responsibleCombo.getValue().id(),
                    agendaArea.getText(),
                    notesArea.getText(),
                    statusCombo.getValue(),
                    categoryField.getText(),
                    linkedDocumentsArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Reunions", "Reunion modifiee.");
            refreshMeetings();
            openSelectedMeetingDetail();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void deleteSelectedMeeting() {
        Meeting selected = meetingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez une reunion a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Reunions",
                "Supprimer la reunion \"" + selected.title() + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            meetingService.deleteMeeting(selected.id());
            AlertUtils.info(getScene().getWindow(), "Reunions", "Reunion supprimee.");
            clearForm();
            clearDetail();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void openSelectedMeetingDetail() {
        Meeting selected = meetingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez une reunion.");
            return;
        }
        editingMeetingId = selected.id();
        Meeting latest = meetingService.getMeeting(selected.id());
        detailTitle.setText(latest.title());
        String responsible = latest.responsibleName() == null || latest.responsibleName().isBlank()
                ? "Responsable non defini"
                : latest.responsibleName();
        detailMeta.setText(
                latest.meetingDate() + " "
                        + TIME_DISPLAY.format(latest.startTime()) + "-" + TIME_DISPLAY.format(latest.endTime())
                        + " | " + defaultValue(latest.location())
                        + " | statut: " + latest.statusLabel()
                        + " | responsable: " + responsible
                        + " | participants: " + latest.participantCount()
        );
        refreshParticipants();
        refreshChecklist();
    }

    private void refreshParticipants() {
        if (editingMeetingId <= 0) {
            participants.clear();
            availableParticipants.clear();
            return;
        }
        participants.setAll(meetingService.getParticipants(editingMeetingId, participantSearchField.getText()));
        availableParticipants.setAll(meetingService.getAvailableMembersForMeeting(editingMeetingId, ""));
        participantsTable.refresh();
    }

    private void addParticipant() {
        if (editingMeetingId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Ouvrez d'abord le detail d'une reunion.");
            return;
        }
        Member selectedMember = participantCombo.getValue();
        if (selectedMember == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez un participant a ajouter.");
            return;
        }
        try {
            meetingService.addParticipant(editingMeetingId, selectedMember.id());
            AlertUtils.info(getScene().getWindow(), "Reunions", "Participant ajoute.");
            openSelectedMeetingDetail();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void removeParticipant() {
        if (editingMeetingId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Ouvrez d'abord le detail d'une reunion.");
            return;
        }
        Member selected = participantsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Selectionnez un participant a retirer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Reunions",
                "Retirer \"" + selected.fullName() + "\" de cette reunion ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            meetingService.removeParticipant(editingMeetingId, selected.id());
            AlertUtils.info(getScene().getWindow(), "Reunions", "Participant retire.");
            openSelectedMeetingDetail();
            refreshMeetings();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void refreshChecklist() {
        if (editingMeetingId <= 0) {
            checklistItems.clear();
            return;
        }
        refreshChecklistCategories();
        checklistItems.setAll(checklistService.getItems(CategoryScope.MEETING, editingMeetingId));
    }

    private void addChecklistItem() {
        if (editingMeetingId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Reunions", "Ouvrez d'abord une reunion.");
            return;
        }
        String categoryName = checklistCategoryCombo.getValue() != null
                ? checklistCategoryCombo.getValue().name()
                : checklistCategoryCombo.getEditor().getText();
        try {
            checklistService.addItem(
                    CategoryScope.MEETING,
                    editingMeetingId,
                    categoryName,
                    checklistItemField.getText()
            );
            checklistItemField.clear();
            refreshChecklist();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Reunions", e.getMessage());
        }
    }

    private void loadMeetingIntoForm(Meeting meeting) {
        if (meeting == null) {
            return;
        }
        editingMeetingId = meeting.id();
        titleField.setText(meeting.title());
        dateField.setText(meeting.meetingDate().toString());
        startTimeField.setText(TIME_DISPLAY.format(meeting.startTime()));
        endTimeField.setText(TIME_DISPLAY.format(meeting.endTime()));
        locationField.setText(defaultValue(meeting.location()));
        organizerField.setText(defaultValue(meeting.organizer()));
        categoryField.setText(defaultValue(meeting.category()));
        linkedDocumentsArea.setText(defaultValue(meeting.linkedDocuments()));
        agendaArea.setText(defaultValue(meeting.agenda()));
        notesArea.setText(defaultValue(meeting.notes()));
        statusCombo.setValue(meeting.status());

        if (meeting.responsibleMemberId() == null) {
            responsibleCombo.getSelectionModel().clearSelection();
        } else {
            for (Member member : availableResponsibleMembers) {
                if (member.id() == meeting.responsibleMemberId()) {
                    responsibleCombo.setValue(member);
                    break;
                }
            }
        }
    }

    private void clearForm() {
        editingMeetingId = -1L;
        titleField.clear();
        dateField.setText(LocalDate.now().toString());
        startTimeField.setText("18:30");
        endTimeField.setText("20:00");
        locationField.clear();
        organizerField.clear();
        responsibleCombo.getSelectionModel().clearSelection();
        statusCombo.getSelectionModel().select(ScheduleStatus.PLANNED);
        categoryField.clear();
        linkedDocumentsArea.clear();
        agendaArea.clear();
        notesArea.clear();
        checklistItemField.clear();
        meetingsTable.getSelectionModel().clearSelection();
    }

    private void clearDetail() {
        detailTitle.setText("Aucune reunion selectionnee");
        detailMeta.setText("");
        participants.clear();
        availableParticipants.clear();
        checklistItems.clear();
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
            throw new IllegalArgumentException("Heure obligatoire (format HH:mm).");
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
