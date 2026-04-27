package com.monasso.app.ui.screen;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.TaskItem;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.service.EventService;
import com.monasso.app.service.MeetingService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.TaskService;
import com.monasso.app.util.AlertUtils;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
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
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class TasksScreen extends VBox {

    private enum DisplayMode {
        COMPACT("Compact"),
        DETAILED("Detaille");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record MemberFilter(Long memberId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record StatusFilter(TaskStatus status, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record RefOption(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final TaskService taskService;
    private final EventService eventService;
    private final MeetingService meetingService;
    private final MemberService memberService;

    private final ObservableList<TaskItem> tasks = FXCollections.observableArrayList();
    private final ObservableList<MemberFilter> assigneeFilters = FXCollections.observableArrayList();
    private final ObservableList<Member> assignees = FXCollections.observableArrayList();
    private final ObservableList<RefOption> eventOptions = FXCollections.observableArrayList();
    private final ObservableList<RefOption> meetingOptions = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ComboBox<DisplayMode> displayModeCombo = new ComboBox<>();
    private final ComboBox<MemberFilter> assigneeFilterCombo = new ComboBox<>();
    private final TextField dueDateFilterField = new TextField();
    private final ComboBox<StatusFilter> statusFilterCombo = new ComboBox<>();
    private final Label summaryLabel = new Label();

    private final TextField titleField = new TextField();
    private final ComboBox<TaskLinkType> linkTypeCombo = new ComboBox<>();
    private final ComboBox<RefOption> linkedEventCombo = new ComboBox<>();
    private final ComboBox<RefOption> linkedMeetingCombo = new ComboBox<>();
    private final ComboBox<Member> assigneeCombo = new ComboBox<>();
    private final TextField dueDateField = new TextField();
    private final ComboBox<TaskPriority> priorityCombo = new ComboBox<>();
    private final ComboBox<TaskStatus> statusCombo = new ComboBox<>();
    private final TextArea notesArea = new TextArea();

    private final TableView<TaskItem> tasksTable = createTasksTable();
    private TableColumn<TaskItem, Number> idColumn;
    private TableColumn<TaskItem, String> linkColumn;
    private TableColumn<TaskItem, String> assigneeColumn;
    private TableColumn<TaskItem, String> priorityColumn;
    private TitledPane formPane;

    private long editingTaskId = -1L;

    public TasksScreen(
            TaskService taskService,
            EventService eventService,
            MeetingService meetingService,
            MemberService memberService
    ) {
        this.taskService = taskService;
        this.eventService = eventService;
        this.meetingService = meetingService;
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Taches");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Suivi operationnel des taches avec affectation, priorite, echeance et lien evenement/reunion.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        displayModeCombo.getItems().setAll(DisplayMode.values());
        displayModeCombo.getSelectionModel().select(DisplayMode.COMPACT);
        displayModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyDisplayMode());

        HBox modeRow = new HBox(10, new Label("Mode"), displayModeCombo);
        modeRow.getStyleClass().add("action-row");

        summaryLabel.getStyleClass().add("muted-text");
        VBox.setVgrow(tasksTable, Priority.ALWAYS);

        formPane = new TitledPane("Fiche tache", createFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(false);

        getChildren().addAll(
                title,
                subtitle,
                modeRow,
                createFilterPanel(),
                summaryLabel,
                tasksTable,
                formPane
        );

        configureFilters();
        loadReferenceData();
        refreshTasks();
        applyDisplayMode();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Filtres");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher tache, notes, responsable, evenement, reunion");
        searchField.setOnAction(event -> refreshTasks());

        dueDateFilterField.setPromptText("Echeance max YYYY-MM-DD");
        dueDateFilterField.setOnAction(event -> refreshTasks());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshTasks());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser");
        resetItem.setOnAction(event -> {
            searchField.clear();
            dueDateFilterField.clear();
            assigneeFilterCombo.getSelectionModel().selectFirst();
            statusFilterCombo.getSelectionModel().selectFirst();
            refreshTasks();
        });
        moreButton.getItems().add(resetItem);

        HBox row = new HBox(
                10,
                new Label("Recherche"), searchField,
                new Label("Responsable"), assigneeFilterCombo,
                new Label("Echeance"), dueDateFilterField,
                new Label("Statut"), statusFilterCombo,
                applyButton,
                moreButton
        );
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Edition");
        section.getStyleClass().add("section-label");

        titleField.setPromptText("Titre");

        linkTypeCombo.getItems().setAll(TaskLinkType.values());
        linkTypeCombo.getSelectionModel().select(TaskLinkType.NONE);
        linkTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshLinkInputsState());

        linkedEventCombo.setItems(eventOptions);
        linkedEventCombo.setPromptText("Evenement lie");

        linkedMeetingCombo.setItems(meetingOptions);
        linkedMeetingCombo.setPromptText("Reunion liee");

        assigneeCombo.setItems(assignees);
        assigneeCombo.setPromptText("Responsable");
        assigneeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Member member) {
                return member == null ? "" : member.fullName();
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        });

        dueDateField.setPromptText("YYYY-MM-DD");

        priorityCombo.getItems().setAll(TaskPriority.values());
        priorityCombo.getSelectionModel().select(TaskPriority.MEDIUM);

        statusCombo.getItems().setAll(TaskStatus.values());
        statusCombo.getSelectionModel().select(TaskStatus.TODO);

        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Titre *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Type de lien"), 2, 0);
        grid.add(linkTypeCombo, 3, 0);
        grid.add(new Label("Evenement"), 0, 1);
        grid.add(linkedEventCombo, 1, 1);
        grid.add(new Label("Reunion"), 2, 1);
        grid.add(linkedMeetingCombo, 3, 1);
        grid.add(new Label("Responsable"), 0, 2);
        grid.add(assigneeCombo, 1, 2);
        grid.add(new Label("Echeance"), 2, 2);
        grid.add(dueDateField, 3, 2);
        grid.add(new Label("Priorite"), 0, 3);
        grid.add(priorityCombo, 1, 3);
        grid.add(new Label("Statut"), 2, 3);
        grid.add(statusCombo, 3, 3);
        grid.add(new Label("Notes"), 0, 4);
        grid.add(notesArea, 1, 4, 3, 1);

        Button createButton = new Button("Creer tache");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createTask());

        Button updateButton = new Button("Modifier tache");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateTask());

        Button deleteButton = new Button("Supprimer tache");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteSelectedTask());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Nouveau formulaire");
        clearItem.setOnAction(event -> clearForm());
        moreButton.getItems().add(clearItem);

        HBox actions = new HBox(10, createButton, updateButton, deleteButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        refreshLinkInputsState();
        return panel;
    }

    private TableView<TaskItem> createTasksTable() {
        TableView<TaskItem> table = new TableView<>(tasks);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<TaskItem, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(220);

        linkColumn = new TableColumn<>("Lien");
        linkColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().linkedLabel())));
        linkColumn.setPrefWidth(170);

        assigneeColumn = new TableColumn<>("Responsable");
        assigneeColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().assigneeName())));
        assigneeColumn.setPrefWidth(150);

        TableColumn<TaskItem, String> dueDateColumn = new TableColumn<>("Echeance");
        dueDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().dueDate() == null ? "" : cell.getValue().dueDate().toString()));
        dueDateColumn.setPrefWidth(120);

        priorityColumn = new TableColumn<>("Priorite");
        priorityColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().priority().label()));
        priorityColumn.setPrefWidth(100);

        TableColumn<TaskItem, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status().label()));
        statusColumn.setPrefWidth(110);

        table.getColumns().add(idColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(linkColumn);
        table.getColumns().add(assigneeColumn);
        table.getColumns().add(dueDateColumn);
        table.getColumns().add(priorityColumn);
        table.getColumns().add(statusColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadTaskIntoForm(newValue);
            }
        });
        return table;
    }

    private void configureFilters() {
        assigneeFilters.setAll(new MemberFilter(null, "Tous"));
        assigneeFilterCombo.setItems(assigneeFilters);
        assigneeFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.getItems().setAll(new StatusFilter(null, "Tous"));
        for (TaskStatus status : TaskStatus.values()) {
            statusFilterCombo.getItems().add(new StatusFilter(status, status.label()));
        }
        statusFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadReferenceData() {
        assignees.setAll(memberService.getMembers("", MemberStatusFilter.ACTIVE));

        assigneeFilters.clear();
        assigneeFilters.add(new MemberFilter(null, "Tous"));
        for (Member member : assignees) {
            assigneeFilters.add(new MemberFilter(member.id(), member.fullName()));
        }
        assigneeFilterCombo.setItems(assigneeFilters);
        if (assigneeFilterCombo.getSelectionModel().isEmpty()) {
            assigneeFilterCombo.getSelectionModel().selectFirst();
        }

        eventOptions.clear();
        for (Event event : eventService.getEvents("", false, ArchiveFilter.ACTIVE)) {
            eventOptions.add(new RefOption(event.id(), event.title() + " (" + event.eventDate() + ")"));
        }

        meetingOptions.clear();
        for (Meeting meeting : meetingService.getMeetings("", false, ArchiveFilter.ACTIVE)) {
            meetingOptions.add(new RefOption(meeting.id(), meeting.title() + " (" + meeting.meetingDate() + ")"));
        }
    }

    private void refreshTasks() {
        LocalDate dueDateFilter = parseOptionalDate(dueDateFilterField.getText(), "Filtre echeance");
        MemberFilter assigneeFilter = assigneeFilterCombo.getValue();
        StatusFilter statusFilter = statusFilterCombo.getValue();

        tasks.setAll(taskService.getTasks(
                searchField.getText(),
                assigneeFilter == null ? null : assigneeFilter.memberId(),
                dueDateFilter,
                statusFilter == null ? null : statusFilter.status()
        ));
        tasksTable.refresh();
        summaryLabel.setText(String.format(Locale.FRANCE, "Resultats : %d taches", tasks.size()));
    }

    private void createTask() {
        try {
            taskService.addTask(
                    titleField.getText(),
                    linkTypeCombo.getValue(),
                    selectedId(linkedEventCombo.getValue()),
                    selectedId(linkedMeetingCombo.getValue()),
                    assigneeCombo.getValue() == null ? null : assigneeCombo.getValue().id(),
                    parseOptionalDate(dueDateField.getText(), "Echeance"),
                    priorityCombo.getValue(),
                    statusCombo.getValue(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Taches", "Tache creee.");
            clearForm();
            loadReferenceData();
            refreshTasks();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Taches", e.getMessage());
        }
    }

    private void updateTask() {
        if (editingTaskId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Taches", "Selectionnez une tache a modifier.");
            return;
        }
        try {
            taskService.updateTask(
                    editingTaskId,
                    titleField.getText(),
                    linkTypeCombo.getValue(),
                    selectedId(linkedEventCombo.getValue()),
                    selectedId(linkedMeetingCombo.getValue()),
                    assigneeCombo.getValue() == null ? null : assigneeCombo.getValue().id(),
                    parseOptionalDate(dueDateField.getText(), "Echeance"),
                    priorityCombo.getValue(),
                    statusCombo.getValue(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Taches", "Tache modifiee.");
            loadReferenceData();
            refreshTasks();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Taches", e.getMessage());
        }
    }

    private void deleteSelectedTask() {
        TaskItem selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Taches", "Selectionnez une tache a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Taches",
                "Supprimer la tache \"" + selected.title() + "\" ?"
        );
        if (!confirmed) {
            return;
        }

        try {
            taskService.deleteTask(selected.id());
            AlertUtils.info(getScene().getWindow(), "Taches", "Tache supprimee.");
            clearForm();
            refreshTasks();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Taches", e.getMessage());
        }
    }

    private void loadTaskIntoForm(TaskItem task) {
        editingTaskId = task.id();
        titleField.setText(task.title());
        linkTypeCombo.setValue(task.linkType());
        dueDateField.setText(task.dueDate() == null ? "" : task.dueDate().toString());
        priorityCombo.setValue(task.priority());
        statusCombo.setValue(task.status());
        notesArea.setText(defaultValue(task.notes()));

        assigneeCombo.getSelectionModel().clearSelection();
        if (task.assigneeMemberId() != null) {
            for (Member member : assignees) {
                if (member.id() == task.assigneeMemberId()) {
                    assigneeCombo.setValue(member);
                    break;
                }
            }
        }

        linkedEventCombo.getSelectionModel().clearSelection();
        if (task.linkedEventId() != null) {
            for (RefOption option : eventOptions) {
                if (option.id().equals(task.linkedEventId())) {
                    linkedEventCombo.setValue(option);
                    break;
                }
            }
        }

        linkedMeetingCombo.getSelectionModel().clearSelection();
        if (task.linkedMeetingId() != null) {
            for (RefOption option : meetingOptions) {
                if (option.id().equals(task.linkedMeetingId())) {
                    linkedMeetingCombo.setValue(option);
                    break;
                }
            }
        }

        refreshLinkInputsState();
        if (formPane != null) {
            formPane.setExpanded(true);
        }
    }

    private void clearForm() {
        editingTaskId = -1L;
        titleField.clear();
        linkTypeCombo.getSelectionModel().select(TaskLinkType.NONE);
        linkedEventCombo.getSelectionModel().clearSelection();
        linkedMeetingCombo.getSelectionModel().clearSelection();
        assigneeCombo.getSelectionModel().clearSelection();
        dueDateField.clear();
        priorityCombo.getSelectionModel().select(TaskPriority.MEDIUM);
        statusCombo.getSelectionModel().select(TaskStatus.TODO);
        notesArea.clear();
        tasksTable.getSelectionModel().clearSelection();
        refreshLinkInputsState();
    }

    private void refreshLinkInputsState() {
        TaskLinkType linkType = linkTypeCombo.getValue() == null ? TaskLinkType.NONE : linkTypeCombo.getValue();
        boolean eventEnabled = linkType == TaskLinkType.EVENT;
        boolean meetingEnabled = linkType == TaskLinkType.MEETING;

        linkedEventCombo.setDisable(!eventEnabled);
        linkedMeetingCombo.setDisable(!meetingEnabled);
        if (!eventEnabled) {
            linkedEventCombo.getSelectionModel().clearSelection();
        }
        if (!meetingEnabled) {
            linkedMeetingCombo.getSelectionModel().clearSelection();
        }
    }

    private LocalDate parseOptionalDate(String rawValue, String label) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " invalide. Format attendu: YYYY-MM-DD.");
        }
    }

    private Long selectedId(RefOption option) {
        return option == null ? null : option.id();
    }

    private void applyDisplayMode() {
        DisplayMode mode = displayModeCombo.getValue() == null ? DisplayMode.COMPACT : displayModeCombo.getValue();
        boolean detailed = mode == DisplayMode.DETAILED;

        if (idColumn != null) {
            idColumn.setVisible(detailed);
        }
        if (linkColumn != null) {
            linkColumn.setVisible(detailed);
        }
        if (assigneeColumn != null) {
            assigneeColumn.setVisible(detailed);
        }
        if (priorityColumn != null) {
            priorityColumn.setVisible(detailed);
        }
        if (formPane != null) {
            formPane.setExpanded(detailed);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
