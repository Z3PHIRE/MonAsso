package com.monasso.app.ui.screen;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.ChecklistCategory;
import com.monasso.app.model.ChecklistItem;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomCategoryValue;
import com.monasso.app.model.CustomFieldType;
import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Event;
import com.monasso.app.model.EventAttendanceStatus;
import com.monasso.app.model.EventBudgetLine;
import com.monasso.app.model.EventBudgetLineType;
import com.monasso.app.model.EventBudgetPhase;
import com.monasso.app.model.EventBudgetSummary;
import com.monasso.app.model.EventDocument;
import com.monasso.app.model.EventHistoryEntry;
import com.monasso.app.model.EventParticipantAttendance;
import com.monasso.app.model.EventProgressSnapshot;
import com.monasso.app.model.EventTask;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.service.ChecklistService;
import com.monasso.app.service.CustomCategoryService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.EventTrackingService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EventsScreen extends VBox {

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm");

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

    private final EventService eventService;
    private final EventTrackingService eventTrackingService;
    private final MemberService memberService;
    private final CustomCategoryService customCategoryService;
    private final ChecklistService checklistService;

    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final ObservableList<Member> activeMembers = FXCollections.observableArrayList();
    private final ObservableList<Member> availableMembers = FXCollections.observableArrayList();
    private final ObservableList<EventParticipantAttendance> participantAttendance = FXCollections.observableArrayList();
    private final ObservableList<EventBudgetLine> budgetLines = FXCollections.observableArrayList();
    private final ObservableList<EventTask> tasks = FXCollections.observableArrayList();
    private final ObservableList<EventDocument> documents = FXCollections.observableArrayList();
    private final ObservableList<EventHistoryEntry> historyEntries = FXCollections.observableArrayList();
    private final ObservableList<ChecklistCategory> checklistCategories = FXCollections.observableArrayList();
    private final ObservableList<ChecklistItem> checklistItems = FXCollections.observableArrayList();
    private final ObservableList<CustomCategory> eventCategories = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final CheckBox upcomingOnlyCheck = new CheckBox("Afficher uniquement les evenements a venir");
    private final ComboBox<ArchiveFilter> archiveFilterCombo = new ComboBox<>();
    private final ComboBox<DisplayMode> displayModeCombo = new ComboBox<>();
    private final Label tableSummary = new Label();

    private final TextField titleField = new TextField();
    private final TextField dateField = new TextField(LocalDate.now().toString());
    private final TextField startTimeField = new TextField("19:00");
    private final TextField endTimeField = new TextField("21:00");
    private final TextField locationField = new TextField();
    private final ComboBox<Member> responsibleCombo = new ComboBox<>();
    private final ComboBox<ScheduleStatus> statusCombo = new ComboBox<>();
    private final TextField capacityField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    private final TextArea materialsArea = new TextArea();
    private final TextArea logisticsArea = new TextArea();
    private final TextArea partnersArea = new TextArea();
    private final TextArea internalNotesArea = new TextArea();

    private final Label detailTitle = new Label("Aucun evenement selectionne");
    private final Label detailMeta = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("Progression: -");
    private final Label tasksSummaryLabel = new Label("Taches ouvertes: -");
    private final Label participantsSummaryLabel = new Label("Participants: -");
    private final Label budgetSummaryLabel = new Label("Budget: -");
    private final Label checklistSummaryLabel = new Label("Checklist: -");

    private final TextField participantSearchField = new TextField();
    private final ComboBox<Member> availableMembersCombo = new ComboBox<>();
    private final TableView<EventParticipantAttendance> participantsTable = createParticipantsTable();

    private final ComboBox<EventBudgetLineType> budgetTypeCombo = new ComboBox<>();
    private final ComboBox<EventBudgetPhase> budgetPhaseCombo = new ComboBox<>();
    private final TextField budgetCategoryField = new TextField();
    private final TextField budgetLabelField = new TextField();
    private final TextField budgetAmountField = new TextField();
    private final TextField budgetNotesField = new TextField();
    private final Label plannedTotalLabel = new Label("Total prevu: 0,00");
    private final Label actualTotalLabel = new Label("Total reel: 0,00");
    private final Label remainingLabel = new Label("Reste / depassement: 0,00");
    private final TableView<EventBudgetLine> budgetTable = createBudgetTable();

    private final TextField taskTitleField = new TextField();
    private final TextField taskDueDateField = new TextField();
    private final ComboBox<Member> taskResponsibleCombo = new ComboBox<>();
    private final TextArea taskDescriptionArea = new TextArea();
    private final CheckBox openTasksOnlyCheck = new CheckBox("Afficher uniquement les taches ouvertes");
    private final TableView<EventTask> tasksTable = createTasksTable();

    private final TextField documentNameField = new TextField();
    private final TextField documentRefField = new TextField();
    private final TextField documentNotesField = new TextField();
    private final TableView<EventDocument> documentsTable = createDocumentsTable();

    private final ListView<EventHistoryEntry> historyListView = createHistoryListView();

    private final ComboBox<ChecklistCategory> checklistCategoryCombo = new ComboBox<>();
    private final TextField checklistItemField = new TextField();
    private final ListView<ChecklistItem> checklistListView = createChecklistListView();

    private final VBox categoriesEditorBox = new VBox(8);
    private final Map<Long, Control> categoryControls = new LinkedHashMap<>();
    private final Map<Long, CustomCategory> categoryById = new LinkedHashMap<>();

    private final TableView<Event> eventsTable = createEventsTable();
    private TableColumn<Event, Number> idColumn;
    private TableColumn<Event, String> responsibleColumn;
    private TableColumn<Event, String> categoryColumn;
    private TableColumn<Event, String> capacityColumn;
    private TableColumn<Event, String> archivedColumn;
    private long editingEventId = -1L;

    public EventsScreen(
            EventService eventService,
            EventTrackingService eventTrackingService,
            MemberService memberService,
            CustomCategoryService customCategoryService,
            ChecklistService checklistService
    ) {
        this.eventService = eventService;
        this.eventTrackingService = eventTrackingService;
        this.memberService = memberService;
        this.customCategoryService = customCategoryService;
        this.checklistService = checklistService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Evenements");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Pilotage complet des evenements: preparation, execution et suivi post-evenement.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        displayModeCombo.getItems().setAll(DisplayMode.values());
        displayModeCombo.getSelectionModel().select(DisplayMode.COMPACT);
        displayModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyDisplayMode());

        HBox modeRow = new HBox(10, new Label("Mode"), displayModeCombo);
        modeRow.getStyleClass().add("action-row");

        tableSummary.getStyleClass().add("muted-text");
        detailTitle.getStyleClass().add("event-name");
        detailMeta.getStyleClass().add("muted-text");
        progressLabel.getStyleClass().add("muted-text");
        tasksSummaryLabel.getStyleClass().add("muted-text");
        participantsSummaryLabel.getStyleClass().add("muted-text");
        budgetSummaryLabel.getStyleClass().add("muted-text");
        checklistSummaryLabel.getStyleClass().add("muted-text");
        progressBar.getStyleClass().add("progress-meter");

        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        TitledPane formPane = new TitledPane("Fiche evenement", createEventFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(false);

        TitledPane detailPane = new TitledPane("Suivi evenement", createEventDetailPanel());
        detailPane.getStyleClass().add("folded-panel");
        detailPane.setExpanded(true);

        getChildren().addAll(
                title,
                subtitle,
                modeRow,
                createFilterPanel(),
                tableSummary,
                eventsTable,
                formPane,
                detailPane
        );

        loadActiveMembers();
        refreshChecklistCategories();
        refreshCategoryDefinitions();
        refreshEvents();
        applyDisplayMode();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher titre, lieu, categorie, responsable, logistique, partenaires");
        searchField.setOnAction(event -> refreshEvents());
        upcomingOnlyCheck.setOnAction(event -> refreshEvents());
        archiveFilterCombo.getItems().setAll(ArchiveFilter.values());
        archiveFilterCombo.getSelectionModel().select(ArchiveFilter.ACTIVE);
        archiveFilterCombo.setOnAction(event -> refreshEvents());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshEvents());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser filtres");
        resetItem.setOnAction(event -> {
            searchField.clear();
            upcomingOnlyCheck.setSelected(false);
            archiveFilterCombo.getSelectionModel().select(ArchiveFilter.ACTIVE);
            refreshEvents();
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

    private VBox createEventFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Informations principales");
        section.getStyleClass().add("section-label");

        titleField.setPromptText("Titre");
        dateField.setPromptText("Date YYYY-MM-DD");
        startTimeField.setPromptText("Debut HH:mm");
        endTimeField.setPromptText("Fin HH:mm");
        locationField.setPromptText("Lieu");
        capacityField.setPromptText("Capacite optionnelle");
        categoryField.setPromptText("Categorie");
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(2);

        responsibleCombo.setItems(activeMembers);
        responsibleCombo.setPromptText("Responsable");
        responsibleCombo.setConverter(memberConverter());

        statusCombo.getItems().setAll(ScheduleStatus.values());
        statusCombo.getSelectionModel().select(ScheduleStatus.PLANNED);

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
        grid.add(new Label("Responsable"), 2, 2);
        grid.add(responsibleCombo, 3, 2);
        grid.add(new Label("Statut"), 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(new Label("Capacite"), 2, 3);
        grid.add(capacityField, 3, 3);
        grid.add(new Label("Categorie"), 0, 4);
        grid.add(categoryField, 1, 4);
        grid.add(new Label("Description"), 2, 4);
        grid.add(descriptionArea, 3, 4);

        TitledPane advancedPane = new TitledPane("Informations avancees", createAdvancedEventInfoPanel());
        advancedPane.getStyleClass().add("folded-panel");
        advancedPane.setExpanded(false);

        Button createButton = new Button("Creer");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createEvent());

        Button updateButton = new Button("Modifier");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateEvent());

        Button archiveButton = new Button("Archiver / desarchiver");
        archiveButton.getStyleClass().add("ghost-button");
        archiveButton.setOnAction(event -> toggleArchiveSelectedEvent());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Nouveau formulaire");
        clearItem.setOnAction(event -> clearForm());
        MenuItem viewItem = new MenuItem("Voir suivi");
        viewItem.setOnAction(event -> openSelectedEventDetail());
        MenuItem deleteItem = new MenuItem("Supprimer la selection");
        deleteItem.setOnAction(event -> deleteSelectedEvent());
        moreButton.getItems().addAll(clearItem, viewItem, deleteItem);

        HBox actions = new HBox(10, createButton, updateButton, archiveButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, advancedPane, actions);
        return panel;
    }

    private VBox createAdvancedEventInfoPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        materialsArea.setPromptText("Liste de materiel");
        materialsArea.setPrefRowCount(2);
        logisticsArea.setPromptText("Besoins logistiques");
        logisticsArea.setPrefRowCount(2);
        partnersArea.setPromptText("Partenaires");
        partnersArea.setPrefRowCount(2);
        internalNotesArea.setPromptText("Notes internes");
        internalNotesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Materiel"), 0, 0);
        grid.add(materialsArea, 1, 0);
        grid.add(new Label("Logistique"), 2, 0);
        grid.add(logisticsArea, 3, 0);
        grid.add(new Label("Partenaires"), 0, 1);
        grid.add(partnersArea, 1, 1);
        grid.add(new Label("Notes internes"), 2, 1);
        grid.add(internalNotesArea, 3, 1);

        panel.getChildren().add(grid);
        return panel;
    }

    private VBox createEventDetailPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Suivi operationnel");
        section.getStyleClass().add("section-label");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("light-tabs");

        Tab participantsTab = new Tab("Participants", createParticipantsTab());
        participantsTab.setClosable(false);

        Tab followUpTab = new Tab("Suivi", createFollowUpTab());
        followUpTab.setClosable(false);

        Tab documentsTab = new Tab("Documents", createDocumentsPanel());
        documentsTab.setClosable(false);

        Tab checklistTab = new Tab("Checklist", createChecklistTab());
        checklistTab.setClosable(false);

        Tab categoriesTab = new Tab("Categories", createCategoriesTab());
        categoriesTab.setClosable(false);

        tabPane.getTabs().addAll(participantsTab, followUpTab, documentsTab, checklistTab, categoriesTab);

        panel.getChildren().addAll(section, detailTitle, detailMeta, tabPane);
        return panel;
    }

    private VBox createParticipantsTab() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        participantSearchField.setPromptText("Filtrer les participants");
        participantSearchField.setOnAction(event -> refreshParticipants());

        availableMembersCombo.setItems(availableMembers);
        availableMembersCombo.setPromptText("Choisir un membre actif");
        availableMembersCombo.setConverter(memberConverter());

        Button addParticipantButton = new Button("Ajouter participant");
        addParticipantButton.getStyleClass().add("accent-button");
        addParticipantButton.setOnAction(event -> addParticipant());

        Button removeParticipantButton = new Button("Retirer participant");
        removeParticipantButton.getStyleClass().add("danger-button");
        removeParticipantButton.setOnAction(event -> removeParticipant());

        Button markPresentButton = new Button("Marquer present");
        markPresentButton.getStyleClass().add("primary-button");
        markPresentButton.setOnAction(event -> markParticipantAttendance(EventAttendanceStatus.PRESENT));

        Button markAbsentButton = new Button("Marquer absent");
        markAbsentButton.getStyleClass().add("ghost-button");
        markAbsentButton.setOnAction(event -> markParticipantAttendance(EventAttendanceStatus.ABSENT));

        Button resetAttendanceButton = new Button("Reinitialiser presence");
        resetAttendanceButton.getStyleClass().add("ghost-button");
        resetAttendanceButton.setOnAction(event -> markParticipantAttendance(EventAttendanceStatus.REGISTERED));

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshParticipants());

        HBox row1 = new HBox(10, participantSearchField, refreshButton);
        row1.getStyleClass().add("action-row");
        HBox.setHgrow(participantSearchField, Priority.ALWAYS);

        HBox row2 = new HBox(10, availableMembersCombo, addParticipantButton, removeParticipantButton);
        row2.getStyleClass().add("action-row");
        HBox.setHgrow(availableMembersCombo, Priority.ALWAYS);

        HBox row3 = new HBox(10, markPresentButton, markAbsentButton, resetAttendanceButton);
        row3.getStyleClass().add("action-row");

        VBox.setVgrow(participantsTable, Priority.ALWAYS);
        panel.getChildren().addAll(row1, row2, row3, participantsTable);
        return panel;
    }

    private VBox createFollowUpTab() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        VBox summaryBlock = new VBox(6, progressLabel, progressBar, tasksSummaryLabel, participantsSummaryLabel, budgetSummaryLabel, checklistSummaryLabel);
        summaryBlock.getStyleClass().add("panel-card");

        TitledPane budgetPane = new TitledPane("Budget evenement", createBudgetPanel());
        budgetPane.getStyleClass().add("folded-panel");
        budgetPane.setExpanded(true);

        TitledPane tasksPane = new TitledPane("Taches liees", createTasksPanel());
        tasksPane.getStyleClass().add("folded-panel");
        tasksPane.setExpanded(true);

        TitledPane historyPane = new TitledPane("Historique des modifications", createHistoryPanel());
        historyPane.getStyleClass().add("folded-panel");
        historyPane.setExpanded(false);

        Accordion accordion = new Accordion(budgetPane, tasksPane, historyPane);
        accordion.setExpandedPane(budgetPane);

        VBox.setVgrow(accordion, Priority.ALWAYS);
        panel.getChildren().addAll(summaryBlock, accordion);
        return panel;
    }

    private VBox createBudgetPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        budgetTypeCombo.getItems().setAll(EventBudgetLineType.values());
        budgetTypeCombo.getSelectionModel().select(EventBudgetLineType.EXPENSE);
        budgetPhaseCombo.getItems().setAll(EventBudgetPhase.values());
        budgetPhaseCombo.getSelectionModel().select(EventBudgetPhase.PLANNED);

        budgetCategoryField.setPromptText("Categorie");
        budgetLabelField.setPromptText("Libelle");
        budgetAmountField.setPromptText("Montant");
        budgetNotesField.setPromptText("Notes");

        Button addLineButton = new Button("Ajouter ligne");
        addLineButton.getStyleClass().add("accent-button");
        addLineButton.setOnAction(event -> addBudgetLine());

        Button removeLineButton = new Button("Supprimer ligne");
        removeLineButton.getStyleClass().add("danger-button");
        removeLineButton.setOnAction(event -> removeSelectedBudgetLine());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Type"), 0, 0);
        grid.add(budgetTypeCombo, 1, 0);
        grid.add(new Label("Phase"), 2, 0);
        grid.add(budgetPhaseCombo, 3, 0);
        grid.add(new Label("Categorie"), 0, 1);
        grid.add(budgetCategoryField, 1, 1);
        grid.add(new Label("Libelle *"), 2, 1);
        grid.add(budgetLabelField, 3, 1);
        grid.add(new Label("Montant *"), 0, 2);
        grid.add(budgetAmountField, 1, 2);
        grid.add(new Label("Notes"), 2, 2);
        grid.add(budgetNotesField, 3, 2);

        HBox actions = new HBox(10, addLineButton, removeLineButton);
        actions.getStyleClass().add("action-row");

        HBox totals = new HBox(18, plannedTotalLabel, actualTotalLabel, remainingLabel);
        totals.getStyleClass().add("action-row");
        plannedTotalLabel.getStyleClass().add("muted-text");
        actualTotalLabel.getStyleClass().add("muted-text");
        remainingLabel.getStyleClass().add("muted-text");

        VBox.setVgrow(budgetTable, Priority.ALWAYS);
        panel.getChildren().addAll(grid, actions, totals, budgetTable);
        return panel;
    }

    private VBox createTasksPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        taskTitleField.setPromptText("Titre tache");
        taskDueDateField.setPromptText("Echeance YYYY-MM-DD (optionnel)");
        taskResponsibleCombo.setItems(activeMembers);
        taskResponsibleCombo.setPromptText("Responsable");
        taskResponsibleCombo.setConverter(memberConverter());
        taskDescriptionArea.setPromptText("Description tache");
        taskDescriptionArea.setPrefRowCount(2);
        openTasksOnlyCheck.setSelected(false);
        openTasksOnlyCheck.setOnAction(event -> refreshTasks());

        Button addTaskButton = new Button("Ajouter tache");
        addTaskButton.getStyleClass().add("accent-button");
        addTaskButton.setOnAction(event -> addTask());

        Button toggleTaskButton = new Button("Basculer ouverte/terminee");
        toggleTaskButton.getStyleClass().add("primary-button");
        toggleTaskButton.setOnAction(event -> toggleSelectedTask());

        Button deleteTaskButton = new Button("Supprimer tache");
        deleteTaskButton.getStyleClass().add("danger-button");
        deleteTaskButton.setOnAction(event -> deleteSelectedTask());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Titre *"), 0, 0);
        grid.add(taskTitleField, 1, 0);
        grid.add(new Label("Echeance"), 2, 0);
        grid.add(taskDueDateField, 3, 0);
        grid.add(new Label("Responsable"), 0, 1);
        grid.add(taskResponsibleCombo, 1, 1);
        grid.add(new Label("Description"), 2, 1);
        grid.add(taskDescriptionArea, 3, 1);

        HBox actions = new HBox(10, addTaskButton, toggleTaskButton, deleteTaskButton, openTasksOnlyCheck);
        actions.getStyleClass().add("action-row");

        VBox.setVgrow(tasksTable, Priority.ALWAYS);
        panel.getChildren().addAll(grid, actions, tasksTable);
        return panel;
    }

    private VBox createDocumentsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        documentNameField.setPromptText("Nom document");
        documentRefField.setPromptText("Reference / chemin");
        documentNotesField.setPromptText("Notes");

        Button addDocumentButton = new Button("Ajouter document");
        addDocumentButton.getStyleClass().add("accent-button");
        addDocumentButton.setOnAction(event -> addDocument());

        Button deleteDocumentButton = new Button("Supprimer document");
        deleteDocumentButton.getStyleClass().add("danger-button");
        deleteDocumentButton.setOnAction(event -> deleteSelectedDocument());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Nom *"), 0, 0);
        grid.add(documentNameField, 1, 0);
        grid.add(new Label("Reference"), 2, 0);
        grid.add(documentRefField, 3, 0);
        grid.add(new Label("Notes"), 0, 1);
        grid.add(documentNotesField, 1, 1, 3, 1);

        HBox actions = new HBox(10, addDocumentButton, deleteDocumentButton);
        actions.getStyleClass().add("action-row");

        VBox.setVgrow(documentsTable, Priority.ALWAYS);
        panel.getChildren().addAll(grid, actions, documentsTable);
        return panel;
    }

    private VBox createHistoryPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        panel.getChildren().add(historyListView);
        return panel;
    }

    private VBox createChecklistTab() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

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

        VBox.setVgrow(checklistListView, Priority.ALWAYS);
        panel.getChildren().addAll(checklistRow, checklistListView);
        return panel;
    }

    private VBox createCategoriesTab() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label helper = new Label("Categories personnalisables actives pour les evenements.");
        helper.getStyleClass().add("muted-text");

        Button saveButton = new Button("Enregistrer categories");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(event -> saveCategoryValues());

        VBox.setVgrow(categoriesEditorBox, Priority.ALWAYS);
        panel.getChildren().addAll(helper, categoriesEditorBox, saveButton);
        return panel;
    }

    private TableView<Event> createEventsTable() {
        TableView<Event> table = new TableView<>(events);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<Event, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(180);

        TableColumn<Event, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().eventDate().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<Event, String> timeColumn = new TableColumn<>("Horaire");
        timeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                TIME_DISPLAY.format(cell.getValue().eventTime()) + " - " + TIME_DISPLAY.format(cell.getValue().endTime())
        ));
        timeColumn.setPrefWidth(140);

        TableColumn<Event, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(110);

        responsibleColumn = new TableColumn<>("Responsable");
        responsibleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().responsibleName())));
        responsibleColumn.setPrefWidth(150);

        categoryColumn = new TableColumn<>("Categorie");
        categoryColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().category())));
        categoryColumn.setPrefWidth(120);

        capacityColumn = new TableColumn<>("Capacite");
        capacityColumn.setCellValueFactory(cell -> {
            Event event = cell.getValue();
            String value = event.capacity() == null
                    ? "Libre (" + event.participantCount() + ")"
                    : event.participantCount() + "/" + event.capacity();
            return new javafx.beans.property.SimpleStringProperty(value);
        });
        capacityColumn.setPrefWidth(120);

        archivedColumn = new TableColumn<>("Archive");
        archivedColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().archived() ? "Oui" : "Non"));
        archivedColumn.setPrefWidth(90);

        table.getColumns().add(idColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(statusColumn);
        table.getColumns().add(responsibleColumn);
        table.getColumns().add(categoryColumn);
        table.getColumns().add(capacityColumn);
        table.getColumns().add(archivedColumn);
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

    private TableView<EventParticipantAttendance> createParticipantsTable() {
        TableView<EventParticipantAttendance> table = new TableView<>(participantAttendance);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<EventParticipantAttendance, String> nameColumn = new TableColumn<>("Participant");
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fullName()));
        nameColumn.setPrefWidth(220);

        TableColumn<EventParticipantAttendance, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().email())));
        emailColumn.setPrefWidth(220);

        TableColumn<EventParticipantAttendance, String> activeColumn = new TableColumn<>("Actif");
        activeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().active() ? "Oui" : "Non"));
        activeColumn.setPrefWidth(80);

        TableColumn<EventParticipantAttendance, String> attendanceColumn = new TableColumn<>("Presence");
        attendanceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().attendanceStatus().label()));
        attendanceColumn.setPrefWidth(120);

        table.getColumns().add(nameColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(activeColumn);
        table.getColumns().add(attendanceColumn);
        return table;
    }

    private TableView<EventBudgetLine> createBudgetTable() {
        TableView<EventBudgetLine> table = new TableView<>(budgetLines);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(170);

        TableColumn<EventBudgetLine, String> phaseColumn = new TableColumn<>("Phase");
        phaseColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().budgetPhase().label()));
        phaseColumn.setPrefWidth(120);

        TableColumn<EventBudgetLine, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().lineType().label()));
        typeColumn.setPrefWidth(110);

        TableColumn<EventBudgetLine, String> categoryColumn = new TableColumn<>("Categorie");
        categoryColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().category())));
        categoryColumn.setPrefWidth(120);

        TableColumn<EventBudgetLine, String> labelColumn = new TableColumn<>("Libelle");
        labelColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().label())));
        labelColumn.setPrefWidth(180);

        TableColumn<EventBudgetLine, String> amountColumn = new TableColumn<>("Montant");
        amountColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatAmount(cell.getValue().amount())));
        amountColumn.setPrefWidth(120);

        table.getColumns().add(phaseColumn);
        table.getColumns().add(typeColumn);
        table.getColumns().add(categoryColumn);
        table.getColumns().add(labelColumn);
        table.getColumns().add(amountColumn);
        return table;
    }

    private TableView<EventTask> createTasksTable() {
        TableView<EventTask> table = new TableView<>(tasks);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(170);

        TableColumn<EventTask, String> titleColumn = new TableColumn<>("Tache");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(220);

        TableColumn<EventTask, String> dueDateColumn = new TableColumn<>("Echeance");
        dueDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().dueDate() == null ? "" : cell.getValue().dueDate().toString()
        ));
        dueDateColumn.setPrefWidth(120);

        TableColumn<EventTask, String> responsibleColumn = new TableColumn<>("Responsable");
        responsibleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().responsibleName())));
        responsibleColumn.setPrefWidth(150);

        TableColumn<EventTask, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(110);

        table.getColumns().add(titleColumn);
        table.getColumns().add(dueDateColumn);
        table.getColumns().add(responsibleColumn);
        table.getColumns().add(statusColumn);
        return table;
    }

    private TableView<EventDocument> createDocumentsTable() {
        TableView<EventDocument> table = new TableView<>(documents);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(160);

        TableColumn<EventDocument, String> nameColumn = new TableColumn<>("Document");
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().documentName())));
        nameColumn.setPrefWidth(200);

        TableColumn<EventDocument, String> refColumn = new TableColumn<>("Reference");
        refColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().documentRef())));
        refColumn.setPrefWidth(220);

        TableColumn<EventDocument, String> notesColumn = new TableColumn<>("Notes");
        notesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().notes())));
        notesColumn.setPrefWidth(220);

        table.getColumns().add(nameColumn);
        table.getColumns().add(refColumn);
        table.getColumns().add(notesColumn);
        return table;
    }

    private ListView<EventHistoryEntry> createHistoryListView() {
        ListView<EventHistoryEntry> listView = new ListView<>(historyEntries);
        listView.getStyleClass().add("app-list");
        listView.setPrefHeight(180);
        listView.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(EventHistoryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String details = defaultValue(item.details());
                setText(item.createdAt() + " | " + item.actionType() + (details.isBlank() ? "" : " | " + details));
            }
        });
        return listView;
    }

    private ListView<ChecklistItem> createChecklistListView() {
        ListView<ChecklistItem> listView = new ListView<>(checklistItems);
        listView.getStyleClass().add("app-list");
        listView.setPrefHeight(180);
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
                        refreshProgressSummary();
                        if (editingEventId > 0) {
                            eventTrackingService.addHistory(
                                    editingEventId,
                                    "CHECKLIST",
                                    "Mise a jour item checklist: " + defaultValue(item.label())
                            );
                            refreshHistory();
                        }
                    } catch (Exception e) {
                        AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
                    }
                });
                deleteButton.setOnAction(event -> {
                    ChecklistItem item = getItem();
                    if (item == null) {
                        return;
                    }
                    boolean confirmed = AlertUtils.confirm(
                            getScene().getWindow(),
                            "Evenements",
                            "Supprimer l'item checklist \"" + defaultValue(item.label()) + "\" ?"
                    );
                    if (!confirmed) {
                        return;
                    }
                    try {
                        checklistService.deleteItem(item.id());
                        refreshChecklist();
                        refreshProgressSummary();
                        if (editingEventId > 0) {
                            eventTrackingService.addHistory(
                                    editingEventId,
                                    "CHECKLIST",
                                    "Suppression item checklist: " + defaultValue(item.label())
                            );
                            refreshHistory();
                        }
                    } catch (Exception e) {
                        AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
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

    private void loadActiveMembers() {
        activeMembers.setAll(memberService.getMembers("", MemberStatusFilter.ACTIVE));
    }

    private void refreshEvents() {
        ArchiveFilter archiveFilter = archiveFilterCombo.getValue() == null ? ArchiveFilter.ACTIVE : archiveFilterCombo.getValue();
        events.setAll(eventService.getEvents(searchField.getText(), upcomingOnlyCheck.isSelected(), archiveFilter));
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d evenements (%s)", events.size(), archiveFilter.label()));
        eventsTable.refresh();
    }

    private void toggleArchiveSelectedEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un evenement.");
            return;
        }

        boolean archivedTarget = !selected.archived();
        String actionLabel = archivedTarget ? "archiver" : "desarchiver";
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Voulez-vous " + actionLabel + " \"" + selected.title() + "\" ?"
        );
        if (!confirmed) {
            return;
        }

        try {
            eventService.setArchived(selected.id(), archivedTarget);
            AlertUtils.info(getScene().getWindow(), "Evenements", archivedTarget ? "Evenement archive." : "Evenement desarchive.");
            clearForm();
            clearEventDetail();
            refreshEvents();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshChecklistCategories() {
        checklistCategories.setAll(checklistService.getCategories());
    }

    private void refreshCategoryDefinitions() {
        eventCategories.setAll(customCategoryService.getCategoriesForScope(CategoryScope.EVENT));
    }

    private void createEvent() {
        try {
            Event created = eventService.addEvent(
                    titleField.getText(),
                    parseDate(dateField.getText()),
                    parseTime(startTimeField.getText()),
                    parseTime(endTimeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    parseCapacity(capacityField.getText()),
                    responsibleCombo.getValue() == null ? null : responsibleCombo.getValue().id(),
                    statusCombo.getValue(),
                    categoryField.getText(),
                    materialsArea.getText(),
                    logisticsArea.getText(),
                    partnersArea.getText(),
                    internalNotesArea.getText()
            );
            eventTrackingService.addHistory(created.id(), "EVENT", "Creation evenement");
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
                    parseTime(startTimeField.getText()),
                    parseTime(endTimeField.getText()),
                    locationField.getText(),
                    descriptionArea.getText(),
                    parseCapacity(capacityField.getText()),
                    responsibleCombo.getValue() == null ? null : responsibleCombo.getValue().id(),
                    statusCombo.getValue(),
                    categoryField.getText(),
                    materialsArea.getText(),
                    logisticsArea.getText(),
                    partnersArea.getText(),
                    internalNotesArea.getText()
            );
            eventTrackingService.addHistory(editingEventId, "EVENT", "Mise a jour fiche evenement");
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
        detailMeta.setText(buildMetaLine(latest));
        refreshAllDetailData();
    }

    private String buildMetaLine(Event event) {
        String responsible = event.responsibleName() == null || event.responsibleName().isBlank()
                ? "Responsable non defini"
                : event.responsibleName();
        return event.eventDate() + " "
                + TIME_DISPLAY.format(event.eventTime()) + "-" + TIME_DISPLAY.format(event.endTime())
                + " | statut: " + event.statusLabel()
                + " | lieu: " + defaultValue(event.location())
                + " | categorie: " + defaultValue(event.category())
                + " | responsable: " + responsible;
    }

    private void refreshAllDetailData() {
        refreshParticipants();
        refreshBudget();
        refreshTasks();
        refreshDocuments();
        refreshHistory();
        refreshChecklist();
        refreshCategoryEditors();
        refreshProgressSummary();
    }

    private void refreshParticipants() {
        if (editingEventId <= 0) {
            participantAttendance.clear();
            availableMembers.clear();
            return;
        }
        participantAttendance.setAll(eventTrackingService.getParticipantAttendance(editingEventId, participantSearchField.getText()));
        availableMembers.setAll(eventService.getAvailableMembersForEvent(editingEventId, ""));
        participantsTable.refresh();
    }

    private void addParticipant() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        Member selectedMember = availableMembersCombo.getValue();
        if (selectedMember == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un membre a ajouter.");
            return;
        }
        try {
            eventService.addParticipant(editingEventId, selectedMember.id());
            eventTrackingService.addHistory(editingEventId, "PARTICIPANT", "Ajout participant: " + selectedMember.fullName());
            AlertUtils.info(getScene().getWindow(), "Evenements", "Participant ajoute.");
            refreshEvents();
            refreshParticipants();
            refreshProgressSummary();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void removeParticipant() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventParticipantAttendance selected = participantsTable.getSelectionModel().getSelectedItem();
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
            eventService.removeParticipant(editingEventId, selected.memberId());
            eventTrackingService.addHistory(editingEventId, "PARTICIPANT", "Retrait participant: " + selected.fullName());
            AlertUtils.info(getScene().getWindow(), "Evenements", "Participant retire.");
            refreshEvents();
            refreshParticipants();
            refreshProgressSummary();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void markParticipantAttendance(EventAttendanceStatus attendanceStatus) {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventParticipantAttendance selected = participantsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un participant.");
            return;
        }
        try {
            eventTrackingService.updateParticipantAttendance(editingEventId, selected.memberId(), attendanceStatus);
            refreshParticipants();
            refreshProgressSummary();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshBudget() {
        if (editingEventId <= 0) {
            budgetLines.clear();
            resetBudgetSummary();
            return;
        }
        budgetLines.setAll(eventTrackingService.getBudgetLines(editingEventId));
        EventBudgetSummary summary = eventTrackingService.getBudgetSummary(editingEventId);
        plannedTotalLabel.setText("Total prevu: " + formatAmount(summary.totalPlanned()));
        actualTotalLabel.setText("Total reel: " + formatAmount(summary.totalActual()));
        remainingLabel.setText("Reste / depassement: " + formatAmount(summary.remainingOrOverrun()));
        budgetTable.refresh();
    }

    private void addBudgetLine() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        try {
            eventTrackingService.addBudgetLine(
                    editingEventId,
                    budgetTypeCombo.getValue(),
                    budgetPhaseCombo.getValue(),
                    budgetCategoryField.getText(),
                    budgetLabelField.getText(),
                    parseAmount(budgetAmountField.getText()),
                    budgetNotesField.getText()
            );
            budgetLabelField.clear();
            budgetAmountField.clear();
            budgetCategoryField.clear();
            budgetNotesField.clear();
            refreshBudget();
            refreshProgressSummary();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void removeSelectedBudgetLine() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventBudgetLine selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez une ligne budget.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Supprimer la ligne budget \"" + defaultValue(selected.label()) + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventTrackingService.deleteBudgetLine(editingEventId, selected.id());
            refreshBudget();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshTasks() {
        if (editingEventId <= 0) {
            tasks.clear();
            return;
        }
        tasks.setAll(eventTrackingService.getTasks(editingEventId, openTasksOnlyCheck.isSelected()));
        tasksTable.refresh();
    }

    private void addTask() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        try {
            eventTrackingService.addTask(
                    editingEventId,
                    taskTitleField.getText(),
                    taskDescriptionArea.getText(),
                    parseOptionalDate(taskDueDateField.getText()),
                    taskResponsibleCombo.getValue() == null ? null : taskResponsibleCombo.getValue().id()
            );
            taskTitleField.clear();
            taskDueDateField.clear();
            taskDescriptionArea.clear();
            taskResponsibleCombo.getSelectionModel().clearSelection();
            refreshTasks();
            refreshProgressSummary();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void toggleSelectedTask() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventTask selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez une tache.");
            return;
        }
        try {
            eventTrackingService.setTaskCompleted(editingEventId, selected.id(), !selected.completed());
            refreshTasks();
            refreshProgressSummary();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void deleteSelectedTask() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventTask selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez une tache.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Supprimer la tache \"" + defaultValue(selected.title()) + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventTrackingService.deleteTask(editingEventId, selected.id());
            refreshTasks();
            refreshProgressSummary();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshDocuments() {
        if (editingEventId <= 0) {
            documents.clear();
            return;
        }
        documents.setAll(eventTrackingService.getDocuments(editingEventId));
        documentsTable.refresh();
    }

    private void addDocument() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        try {
            eventTrackingService.addDocument(
                    editingEventId,
                    documentNameField.getText(),
                    documentRefField.getText(),
                    documentNotesField.getText()
            );
            documentNameField.clear();
            documentRefField.clear();
            documentNotesField.clear();
            refreshDocuments();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void deleteSelectedDocument() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        EventDocument selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Selectionnez un document.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Evenements",
                "Supprimer le document \"" + defaultValue(selected.documentName()) + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            eventTrackingService.deleteDocument(editingEventId, selected.id());
            refreshDocuments();
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshHistory() {
        if (editingEventId <= 0) {
            historyEntries.clear();
            return;
        }
        historyEntries.setAll(eventTrackingService.getHistory(editingEventId, 80));
    }

    private void refreshChecklist() {
        if (editingEventId <= 0) {
            checklistItems.clear();
            return;
        }
        refreshChecklistCategories();
        checklistItems.setAll(checklistService.getItems(CategoryScope.EVENT, editingEventId));
    }

    private void addChecklistItem() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        String categoryName = checklistCategoryCombo.getValue() != null
                ? checklistCategoryCombo.getValue().name()
                : checklistCategoryCombo.getEditor().getText();
        try {
            checklistService.addItem(
                    CategoryScope.EVENT,
                    editingEventId,
                    categoryName,
                    checklistItemField.getText()
            );
            checklistItemField.clear();
            refreshChecklist();
            refreshProgressSummary();
            eventTrackingService.addHistory(editingEventId, "CHECKLIST", "Ajout item checklist");
            refreshHistory();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private void refreshCategoryEditors() {
        categoriesEditorBox.getChildren().clear();
        categoryControls.clear();
        categoryById.clear();
        if (editingEventId <= 0) {
            return;
        }
        refreshCategoryDefinitions();
        Map<Long, CustomCategoryValue> valuesByCategoryId = customCategoryService.getValuesByCategoryId(CategoryScope.EVENT, editingEventId);
        for (CustomCategory category : eventCategories) {
            categoryById.put(category.id(), category);
            String initialValue = toRawValue(valuesByCategoryId.get(category.id()), category.fieldType());
            Control editor = createEditorForCategory(category, initialValue);
            categoryControls.put(category.id(), editor);

            String labelPrefix = category.parentId() == null ? "" : "  - ";
            Label label = new Label(labelPrefix + category.name() + " (" + category.fieldType().label() + ")");
            label.getStyleClass().add("field-label");

            HBox row = new HBox(10, label, editor);
            row.getStyleClass().add("action-row");
            HBox.setHgrow(editor, Priority.ALWAYS);
            categoriesEditorBox.getChildren().add(row);
        }
    }

    private Control createEditorForCategory(CustomCategory category, String initialValue) {
        switch (category.fieldType()) {
            case CHECKBOX -> {
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected("true".equalsIgnoreCase(initialValue));
                return checkBox;
            }
            case LIST -> {
                ComboBox<String> combo = new ComboBox<>();
                if (category.listOptions() != null && !category.listOptions().isBlank()) {
                    for (String option : category.listOptions().split(";")) {
                        String safeOption = option == null ? "" : option.trim();
                        if (!safeOption.isEmpty()) {
                            combo.getItems().add(safeOption);
                        }
                    }
                }
                combo.setEditable(true);
                if (initialValue != null) {
                    combo.getEditor().setText(initialValue);
                }
                return combo;
            }
            case NUMBER -> {
                TextField field = new TextField(initialValue == null ? "" : initialValue);
                field.setPromptText("Nombre");
                return field;
            }
            case DATE -> {
                TextField field = new TextField(initialValue == null ? "" : initialValue);
                field.setPromptText("YYYY-MM-DD");
                return field;
            }
            case SHORT_TEXT -> {
                TextField field = new TextField(initialValue == null ? "" : initialValue);
                field.setPromptText("Texte");
                return field;
            }
            default -> throw new IllegalStateException("Type de categorie non supporte.");
        }
    }

    private void saveCategoryValues() {
        if (editingEventId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Evenements", "Ouvrez d'abord le suivi d'un evenement.");
            return;
        }
        try {
            for (Map.Entry<Long, Control> entry : categoryControls.entrySet()) {
                long categoryId = entry.getKey();
                CustomCategory category = categoryById.get(categoryId);
                if (category == null) {
                    continue;
                }
                String rawValue = readControlValue(entry.getValue(), category.fieldType());
                customCategoryService.saveValue(categoryId, CategoryScope.EVENT, editingEventId, rawValue);
            }
            eventTrackingService.addHistory(editingEventId, "CATEGORY", "Mise a jour categories personnalisees");
            refreshHistory();
            AlertUtils.info(getScene().getWindow(), "Evenements", "Categories personnalisees enregistrees.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Evenements", e.getMessage());
        }
    }

    private String readControlValue(Control control, CustomFieldType fieldType) {
        return switch (fieldType) {
            case CHECKBOX -> String.valueOf(((CheckBox) control).isSelected());
            case LIST -> {
                ComboBox<?> combo = (ComboBox<?>) control;
                Object selected = combo.getValue();
                if (selected != null) {
                    yield selected.toString();
                }
                String typed = combo.getEditor().getText();
                yield typed == null || typed.isBlank() ? null : typed.trim();
            }
            case NUMBER, DATE, SHORT_TEXT -> {
                String text = ((TextField) control).getText();
                yield text == null || text.isBlank() ? null : text.trim();
            }
        };
    }

    private String toRawValue(CustomCategoryValue value, CustomFieldType fieldType) {
        if (value == null) {
            return null;
        }
        return switch (fieldType) {
            case CHECKBOX -> value.boolValue() == null ? null : String.valueOf(value.boolValue());
            case NUMBER -> value.numberValue() == null ? null : String.valueOf(value.numberValue());
            case DATE -> value.dateValue();
            case LIST, SHORT_TEXT -> value.textValue();
        };
    }

    private void refreshProgressSummary() {
        if (editingEventId <= 0) {
            progressBar.setProgress(0);
            progressLabel.setText("Progression: -");
            tasksSummaryLabel.setText("Taches ouvertes: -");
            participantsSummaryLabel.setText("Participants: -");
            budgetSummaryLabel.setText("Budget: -");
            checklistSummaryLabel.setText("Checklist: -");
            return;
        }
        EventProgressSnapshot progress = eventTrackingService.getProgress(editingEventId);
        EventBudgetSummary budget = eventTrackingService.getBudgetSummary(editingEventId);
        progressBar.setProgress(progress.completionRatio());
        progressLabel.setText(String.format(Locale.FRANCE, "Progression globale: %.0f%%", progress.completionRatio() * 100));
        tasksSummaryLabel.setText("Taches ouvertes: " + progress.openTasks() + " / " + progress.tasksTotal());
        participantsSummaryLabel.setText("Participants (present/absent): " + progress.participantsPresent() + " / " + progress.participantsAbsent());
        budgetSummaryLabel.setText("Budget prevu/reel: " + formatAmount(budget.totalPlanned()) + " / " + formatAmount(budget.totalActual()));
        checklistSummaryLabel.setText("Checklist terminee: " + progress.checklistCompleted() + " / " + progress.checklistTotal());
    }

    private void loadEventIntoForm(Event event) {
        if (event == null) {
            return;
        }
        editingEventId = event.id();
        titleField.setText(event.title());
        dateField.setText(event.eventDate().toString());
        startTimeField.setText(TIME_DISPLAY.format(event.eventTime()));
        endTimeField.setText(TIME_DISPLAY.format(event.endTime()));
        locationField.setText(defaultValue(event.location()));
        capacityField.setText(event.capacity() == null ? "" : String.valueOf(event.capacity()));
        descriptionArea.setText(defaultValue(event.description()));
        statusCombo.setValue(event.status());
        categoryField.setText(defaultValue(event.category()));
        materialsArea.setText(defaultValue(event.materials()));
        logisticsArea.setText(defaultValue(event.logisticsNeeds()));
        partnersArea.setText(defaultValue(event.partners()));
        internalNotesArea.setText(defaultValue(event.internalNotes()));

        if (event.responsibleMemberId() == null) {
            responsibleCombo.getSelectionModel().clearSelection();
        } else {
            for (Member member : activeMembers) {
                if (member.id() == event.responsibleMemberId()) {
                    responsibleCombo.setValue(member);
                    break;
                }
            }
        }
    }

    private void clearForm() {
        editingEventId = -1L;
        titleField.clear();
        dateField.setText(LocalDate.now().toString());
        startTimeField.setText("19:00");
        endTimeField.setText("21:00");
        locationField.clear();
        capacityField.clear();
        categoryField.clear();
        descriptionArea.clear();
        materialsArea.clear();
        logisticsArea.clear();
        partnersArea.clear();
        internalNotesArea.clear();
        statusCombo.getSelectionModel().select(ScheduleStatus.PLANNED);
        responsibleCombo.getSelectionModel().clearSelection();
        eventsTable.getSelectionModel().clearSelection();
    }

    private void clearEventDetail() {
        detailTitle.setText("Aucun evenement selectionne");
        detailMeta.setText("");
        participantAttendance.clear();
        availableMembers.clear();
        budgetLines.clear();
        tasks.clear();
        documents.clear();
        historyEntries.clear();
        checklistItems.clear();
        categoriesEditorBox.getChildren().clear();
        categoryControls.clear();
        categoryById.clear();
        resetBudgetSummary();
        refreshProgressSummary();
    }

    private void resetBudgetSummary() {
        plannedTotalLabel.setText("Total prevu: 0,00");
        actualTotalLabel.setText("Total reel: 0,00");
        remainingLabel.setText("Reste / depassement: 0,00");
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

    private LocalDate parseOptionalDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
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

    private double parseAmount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Le montant budget est obligatoire.");
        }
        try {
            double amount = Double.parseDouble(rawValue.trim().replace(",", "."));
            if (amount <= 0) {
                throw new IllegalArgumentException("Le montant budget doit etre superieur a 0.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant budget invalide.");
        }
    }

    private String formatAmount(double amount) {
        return String.format(Locale.FRANCE, "%.2f", amount);
    }

    private void applyDisplayMode() {
        DisplayMode mode = displayModeCombo.getValue() == null ? DisplayMode.COMPACT : displayModeCombo.getValue();
        boolean detailed = mode == DisplayMode.DETAILED;

        if (idColumn != null) {
            idColumn.setVisible(detailed);
        }
        if (responsibleColumn != null) {
            responsibleColumn.setVisible(detailed);
        }
        if (categoryColumn != null) {
            categoryColumn.setVisible(detailed);
        }
        if (capacityColumn != null) {
            capacityColumn.setVisible(detailed);
        }
        if (archivedColumn != null) {
            archivedColumn.setVisible(detailed);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
