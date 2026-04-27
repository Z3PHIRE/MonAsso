package com.monasso.app.ui.screen;

import com.monasso.app.model.AppDocument;
import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.DocumentTargetType;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.TaskItem;
import com.monasso.app.service.DocumentService;
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

import java.util.Locale;

public class DocumentsScreen extends VBox {

    private record TargetOption(long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record TargetFilter(DocumentTargetType type, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final DocumentService documentService;
    private final MemberService memberService;
    private final EventService eventService;
    private final MeetingService meetingService;
    private final TaskService taskService;

    private final ObservableList<AppDocument> documents = FXCollections.observableArrayList();
    private final ObservableList<TargetOption> targetOptions = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ComboBox<TargetFilter> targetFilterCombo = new ComboBox<>();
    private final Label summaryLabel = new Label();

    private final ComboBox<DocumentTargetType> targetTypeCombo = new ComboBox<>();
    private final ComboBox<TargetOption> targetCombo = new ComboBox<>();
    private final TextField filePathField = new TextField();
    private final TextField fileNameField = new TextField();
    private final TextField fileTypeField = new TextField();
    private final TextArea notesArea = new TextArea();

    private final TableView<AppDocument> documentsTable = createDocumentsTable();

    public DocumentsScreen(
            DocumentService documentService,
            MemberService memberService,
            EventService eventService,
            MeetingService meetingService,
            TaskService taskService
    ) {
        this.documentService = documentService;
        this.memberService = memberService;
        this.eventService = eventService;
        this.meetingService = meetingService;
        this.taskService = taskService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Documents");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Pieces jointes liees aux personnes, evenements, reunions et taches.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        summaryLabel.getStyleClass().add("muted-text");
        VBox.setVgrow(documentsTable, Priority.ALWAYS);

        TitledPane formPane = new TitledPane("Nouveau document", createFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(true);

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                summaryLabel,
                documentsTable,
                createActionsPanel(),
                formPane
        );

        initializeFilters();
        targetTypeCombo.getItems().setAll(DocumentTargetType.values());
        targetTypeCombo.getSelectionModel().select(DocumentTargetType.PERSON);
        targetTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshTargetOptions());
        refreshTargetOptions();
        refreshDocuments();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Filtres");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher nom, chemin, type, notes");
        searchField.setOnAction(event -> refreshDocuments());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshDocuments());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser");
        resetItem.setOnAction(event -> {
            searchField.clear();
            targetFilterCombo.getSelectionModel().selectFirst();
            refreshDocuments();
        });
        moreButton.getItems().add(resetItem);

        HBox row = new HBox(
                10,
                new Label("Recherche"), searchField,
                new Label("Cible"), targetFilterCombo,
                applyButton,
                moreButton
        );
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createActionsPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Actions document");
        section.getStyleClass().add("section-label");

        Button openFileButton = new Button("Ouvrir le fichier");
        openFileButton.getStyleClass().add("primary-button");
        openFileButton.setOnAction(event -> openSelectedDocument());

        Button openFolderButton = new Button("Ouvrir le dossier");
        openFolderButton.getStyleClass().add("ghost-button");
        openFolderButton.setOnAction(event -> openSelectedDocumentFolder());

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteSelectedDocument());

        HBox row = new HBox(10, openFileButton, openFolderButton, deleteButton);
        row.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Attacher un fichier");
        section.getStyleClass().add("section-label");

        targetCombo.setItems(targetOptions);
        targetCombo.setPromptText("Choisir la cible");

        filePathField.setPromptText("Chemin fichier (ex: C:\\\\Docs\\\\facture.pdf)");
        fileNameField.setPromptText("Nom de fichier (optionnel)");
        fileTypeField.setPromptText("Type (optionnel)");

        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Type cible *"), 0, 0);
        grid.add(targetTypeCombo, 1, 0);
        grid.add(new Label("Element cible *"), 2, 0);
        grid.add(targetCombo, 3, 0);
        grid.add(new Label("Chemin fichier *"), 0, 1);
        grid.add(filePathField, 1, 1, 3, 1);
        grid.add(new Label("Nom"), 0, 2);
        grid.add(fileNameField, 1, 2);
        grid.add(new Label("Type"), 2, 2);
        grid.add(fileTypeField, 3, 2);
        grid.add(new Label("Notes"), 0, 3);
        grid.add(notesArea, 1, 3, 3, 1);

        Button attachButton = new Button("Attacher document");
        attachButton.getStyleClass().add("accent-button");
        attachButton.setOnAction(event -> attachDocument());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Vider formulaire");
        clearItem.setOnAction(event -> clearForm());
        moreButton.getItems().add(clearItem);

        HBox actions = new HBox(10, attachButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        return panel;
    }

    private TableView<AppDocument> createDocumentsTable() {
        TableView<AppDocument> table = new TableView<>(documents);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<AppDocument, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<AppDocument, String> targetTypeColumn = new TableColumn<>("Type cible");
        targetTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().targetType().label()));
        targetTypeColumn.setPrefWidth(110);

        TableColumn<AppDocument, String> targetColumn = new TableColumn<>("Element");
        targetColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().targetLabel())));
        targetColumn.setPrefWidth(200);

        TableColumn<AppDocument, String> nameColumn = new TableColumn<>("Nom fichier");
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().fileName())));
        nameColumn.setPrefWidth(190);

        TableColumn<AppDocument, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().fileType())));
        typeColumn.setPrefWidth(100);

        TableColumn<AppDocument, String> dateColumn = new TableColumn<>("Ajoute le");
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().addedAt())));
        dateColumn.setPrefWidth(170);

        TableColumn<AppDocument, String> pathColumn = new TableColumn<>("Chemin");
        pathColumn.setCellValueFactory(cell -> new SimpleStringProperty(defaultValue(cell.getValue().filePath())));
        pathColumn.setPrefWidth(260);

        table.getColumns().addAll(idColumn, targetTypeColumn, targetColumn, nameColumn, typeColumn, dateColumn, pathColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadDocumentIntoForm(newValue);
            }
        });
        return table;
    }

    private void initializeFilters() {
        targetFilterCombo.getItems().setAll(new TargetFilter(null, "Tous"));
        for (DocumentTargetType type : DocumentTargetType.values()) {
            targetFilterCombo.getItems().add(new TargetFilter(type, type.label()));
        }
        targetFilterCombo.getSelectionModel().selectFirst();
    }

    private void refreshTargetOptions() {
        targetOptions.clear();
        DocumentTargetType targetType = targetTypeCombo.getValue();
        if (targetType == null) {
            return;
        }
        switch (targetType) {
            case PERSON -> {
                for (Member member : memberService.getMembers("", MemberStatusFilter.ALL)) {
                    targetOptions.add(new TargetOption(member.id(), member.fullName()));
                }
            }
            case EVENT -> {
                for (Event event : eventService.getEvents("", false, ArchiveFilter.ALL)) {
                    targetOptions.add(new TargetOption(event.id(), event.title() + " (" + event.eventDate() + ")"));
                }
            }
            case MEETING -> {
                for (Meeting meeting : meetingService.getMeetings("", false, ArchiveFilter.ALL)) {
                    targetOptions.add(new TargetOption(meeting.id(), meeting.title() + " (" + meeting.meetingDate() + ")"));
                }
            }
            case TASK -> {
                for (TaskItem task : taskService.getTasks("", null, null, null)) {
                    targetOptions.add(new TargetOption(task.id(), task.title()));
                }
            }
        }
        targetCombo.getSelectionModel().clearSelection();
    }

    private void refreshDocuments() {
        TargetFilter filter = targetFilterCombo.getValue();
        documents.setAll(documentService.getDocuments(filter == null ? null : filter.type(), searchField.getText()));
        documentsTable.refresh();
        summaryLabel.setText(String.format(Locale.FRANCE, "Resultats : %d documents", documents.size()));
    }

    private void attachDocument() {
        try {
            DocumentTargetType targetType = targetTypeCombo.getValue();
            TargetOption target = targetCombo.getValue();
            if (targetType == null || target == null) {
                throw new IllegalArgumentException("Selectionnez une cible de document.");
            }
            documentService.addDocument(
                    targetType,
                    target.id(),
                    filePathField.getText(),
                    fileNameField.getText(),
                    fileTypeField.getText(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Documents", "Document attache.");
            clearForm();
            refreshDocuments();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Documents", e.getMessage());
        }
    }

    private void openSelectedDocument() {
        AppDocument selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Documents", "Selectionnez un document.");
            return;
        }
        try {
            documentService.openDocument(selected.id());
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Documents", e.getMessage());
        }
    }

    private void openSelectedDocumentFolder() {
        AppDocument selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Documents", "Selectionnez un document.");
            return;
        }
        try {
            documentService.openDocumentDirectory(selected.id());
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Documents", e.getMessage());
        }
    }

    private void deleteSelectedDocument() {
        AppDocument selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Documents", "Selectionnez un document a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Documents",
                "Supprimer le document \"" + selected.fileName() + "\" ?"
        );
        if (!confirmed) {
            return;
        }

        try {
            documentService.deleteDocument(selected.id());
            AlertUtils.info(getScene().getWindow(), "Documents", "Document supprime.");
            refreshDocuments();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Documents", e.getMessage());
        }
    }

    private void loadDocumentIntoForm(AppDocument document) {
        targetTypeCombo.setValue(document.targetType());
        refreshTargetOptions();
        for (TargetOption option : targetOptions) {
            if (option.id() == document.targetId()) {
                targetCombo.setValue(option);
                break;
            }
        }
        filePathField.setText(defaultValue(document.filePath()));
        fileNameField.setText(defaultValue(document.fileName()));
        fileTypeField.setText(defaultValue(document.fileType()));
        notesArea.setText(defaultValue(document.notes()));
    }

    private void clearForm() {
        targetTypeCombo.getSelectionModel().select(DocumentTargetType.PERSON);
        refreshTargetOptions();
        filePathField.clear();
        fileNameField.clear();
        fileTypeField.clear();
        notesArea.clear();
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
