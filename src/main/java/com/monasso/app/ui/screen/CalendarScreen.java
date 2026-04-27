package com.monasso.app.ui.screen;

import com.monasso.app.model.CalendarEntry;
import com.monasso.app.model.CalendarEntryType;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.service.CalendarService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.MeetingService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class CalendarScreen extends VBox {

    private enum CalendarViewMode {
        MONTH("Vue mois"),
        WEEK("Vue semaine"),
        LIST("Vue liste");

        private final String label;

        CalendarViewMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record TypeFilterOption(CalendarEntryType value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record StatusFilterOption(ScheduleStatus value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ResponsibleOption(Long memberId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm");

    private final CalendarService calendarService;
    private final EventService eventService;
    private final MeetingService meetingService;
    private final MemberService memberService;

    private final ObservableList<CalendarEntry> entries = FXCollections.observableArrayList();
    private final ObservableList<ResponsibleOption> responsibleFilters = FXCollections.observableArrayList();

    private final ComboBox<CalendarViewMode> viewModeCombo = new ComboBox<>();
    private final ComboBox<TypeFilterOption> typeFilterCombo = new ComboBox<>();
    private final ComboBox<StatusFilterOption> statusFilterCombo = new ComboBox<>();
    private final ComboBox<ResponsibleOption> responsibleFilterCombo = new ComboBox<>();
    private final TextField categoryFilterField = new TextField();
    private final Label periodLabel = new Label();
    private final Label summaryLabel = new Label();

    private final TableView<CalendarEntry> tableView = createTable();

    private final Label detailTitleLabel = new Label("Aucun element selectionne");
    private final Label detailMetaLabel = new Label();
    private final Label detailDescriptionLabel = new Label();

    private final ComboBox<CalendarEntryType> quickTypeCombo = new ComboBox<>();
    private final TextField quickTitleField = new TextField();
    private final TextField quickDateField = new TextField(LocalDate.now().toString());
    private final TextField quickStartTimeField = new TextField("18:00");
    private final TextField quickEndTimeField = new TextField("20:00");
    private final TextField quickLocationField = new TextField();
    private final ComboBox<Member> quickResponsibleCombo = new ComboBox<>();
    private final ComboBox<ScheduleStatus> quickStatusCombo = new ComboBox<>();
    private final TextField quickCategoryField = new TextField();

    private LocalDate anchorDate = LocalDate.now();

    public CalendarScreen(
            CalendarService calendarService,
            EventService eventService,
            MeetingService meetingService,
            MemberService memberService
    ) {
        this.calendarService = calendarService;
        this.eventService = eventService;
        this.meetingService = meetingService;
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Calendrier");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Vue temporelle unifiee des evenements et reunions avec filtres et creation rapide.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        summaryLabel.getStyleClass().add("muted-text");
        periodLabel.getStyleClass().add("section-label");

        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(
                title,
                subtitle,
                createNavigationPanel(),
                createFilterPanel(),
                periodLabel,
                summaryLabel,
                tableView,
                createDetailPanel(),
                createQuickCreatePanel()
        );

        initializeFilters();
        loadResponsibleFilters();
        refreshEntries();
    }

    private VBox createNavigationPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        viewModeCombo.getItems().setAll(CalendarViewMode.values());
        viewModeCombo.getSelectionModel().select(CalendarViewMode.MONTH);
        viewModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshEntries());

        Button previousButton = new Button("Precedent");
        previousButton.getStyleClass().add("ghost-button");
        previousButton.setOnAction(event -> {
            CalendarViewMode mode = currentMode();
            switch (mode) {
                case MONTH -> anchorDate = anchorDate.minusMonths(1);
                case WEEK -> anchorDate = anchorDate.minusWeeks(1);
                case LIST -> anchorDate = anchorDate.minusDays(15);
            }
            refreshEntries();
        });

        Button todayButton = new Button("Aujourd'hui");
        todayButton.getStyleClass().add("primary-button");
        todayButton.setOnAction(event -> {
            anchorDate = LocalDate.now();
            refreshEntries();
        });

        Button nextButton = new Button("Suivant");
        nextButton.getStyleClass().add("ghost-button");
        nextButton.setOnAction(event -> {
            CalendarViewMode mode = currentMode();
            switch (mode) {
                case MONTH -> anchorDate = anchorDate.plusMonths(1);
                case WEEK -> anchorDate = anchorDate.plusWeeks(1);
                case LIST -> anchorDate = anchorDate.plusDays(15);
            }
            refreshEntries();
        });

        HBox row = new HBox(10, new Label("Vue"), viewModeCombo, previousButton, todayButton, nextButton);
        row.getStyleClass().add("action-row");
        panel.getChildren().add(row);
        return panel;
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Filtres");
        section.getStyleClass().add("section-label");

        categoryFilterField.setPromptText("Categorie");

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshEntries());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Reinitialiser filtres");
        clearItem.setOnAction(event -> {
            typeFilterCombo.getSelectionModel().selectFirst();
            statusFilterCombo.getSelectionModel().selectFirst();
            responsibleFilterCombo.getSelectionModel().selectFirst();
            categoryFilterField.clear();
            refreshEntries();
        });
        moreButton.getItems().add(clearItem);

        HBox row = new HBox(
                10,
                new Label("Type"), typeFilterCombo,
                new Label("Statut"), statusFilterCombo,
                new Label("Responsable"), responsibleFilterCombo,
                new Label("Categorie"), categoryFilterField,
                applyButton,
                moreButton
        );
        row.getStyleClass().add("action-row");
        HBox.setHgrow(categoryFilterField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Fiche element selectionne");
        section.getStyleClass().add("section-label");
        detailTitleLabel.getStyleClass().add("event-name");
        detailMetaLabel.getStyleClass().add("muted-text");
        detailDescriptionLabel.setWrapText(true);
        detailDescriptionLabel.getStyleClass().add("screen-subtitle");

        panel.getChildren().addAll(section, detailTitleLabel, detailMetaLabel, detailDescriptionLabel);
        return panel;
    }

    private VBox createQuickCreatePanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Creation rapide depuis calendrier");
        section.getStyleClass().add("section-label");

        quickTypeCombo.getItems().setAll(CalendarEntryType.values());
        quickTypeCombo.getSelectionModel().select(CalendarEntryType.EVENT);
        quickTitleField.setPromptText("Titre");
        quickDateField.setPromptText("Date YYYY-MM-DD");
        quickStartTimeField.setPromptText("Debut HH:mm");
        quickEndTimeField.setPromptText("Fin HH:mm");
        quickLocationField.setPromptText("Lieu");
        quickCategoryField.setPromptText("Categorie");
        quickStatusCombo.getItems().setAll(ScheduleStatus.values());
        quickStatusCombo.getSelectionModel().select(ScheduleStatus.CONFIRMED);

        quickResponsibleCombo.setPromptText("Responsable");
        quickResponsibleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Member member) {
                return member == null ? "" : member.fullName();
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        });
        quickResponsibleCombo.setItems(FXCollections.observableArrayList(memberService.getMembers("", MemberStatusFilter.ACTIVE)));

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Type"), 0, 0);
        grid.add(quickTypeCombo, 1, 0);
        grid.add(new Label("Titre *"), 2, 0);
        grid.add(quickTitleField, 3, 0);
        grid.add(new Label("Date *"), 0, 1);
        grid.add(quickDateField, 1, 1);
        grid.add(new Label("Debut *"), 2, 1);
        grid.add(quickStartTimeField, 3, 1);
        grid.add(new Label("Fin *"), 0, 2);
        grid.add(quickEndTimeField, 1, 2);
        grid.add(new Label("Lieu"), 2, 2);
        grid.add(quickLocationField, 3, 2);
        grid.add(new Label("Responsable"), 0, 3);
        grid.add(quickResponsibleCombo, 1, 3);
        grid.add(new Label("Statut"), 2, 3);
        grid.add(quickStatusCombo, 3, 3);
        grid.add(new Label("Categorie"), 0, 4);
        grid.add(quickCategoryField, 1, 4);

        Button createButton = new Button("Creer rapidement");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> quickCreateEntry());

        panel.getChildren().addAll(section, grid, createButton);
        return panel;
    }

    private TableView<CalendarEntry> createTable() {
        TableView<CalendarEntry> table = new TableView<>(entries);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CalendarEntry, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().date().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<CalendarEntry, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().typeLabel()));
        typeColumn.setPrefWidth(90);

        TableColumn<CalendarEntry, String> timeColumn = new TableColumn<>("Horaire");
        timeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                TIME_DISPLAY.format(cell.getValue().startTime()) + " - " + TIME_DISPLAY.format(cell.getValue().endTime())
        ));
        timeColumn.setPrefWidth(140);

        TableColumn<CalendarEntry, String> titleColumn = new TableColumn<>("Titre");
        titleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        titleColumn.setPrefWidth(220);

        TableColumn<CalendarEntry, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                (cell.getValue().status() == null ? ScheduleStatus.CONFIRMED : cell.getValue().status()).label()
        ));
        statusColumn.setPrefWidth(110);

        TableColumn<CalendarEntry, String> responsibleColumn = new TableColumn<>("Responsable");
        responsibleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().responsibleName())));
        responsibleColumn.setPrefWidth(150);

        TableColumn<CalendarEntry, String> categoryColumn = new TableColumn<>("Categorie");
        categoryColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().category())));
        categoryColumn.setPrefWidth(120);

        TableColumn<CalendarEntry, String> locationColumn = new TableColumn<>("Lieu");
        locationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().location())));
        locationColumn.setPrefWidth(160);

        TableColumn<CalendarEntry, String> conflictColumn = new TableColumn<>("Conflit");
        conflictColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().conflict() ? "Oui" : ""));
        conflictColumn.setPrefWidth(80);

        table.getColumns().addAll(dateColumn, typeColumn, timeColumn, titleColumn, statusColumn, responsibleColumn, categoryColumn, locationColumn, conflictColumn);
        table.setRowFactory(tv -> {
            TableRow<CalendarEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() >= 1 && !row.isEmpty()) {
                    openDetail(row.getItem());
                }
            });
            return row;
        });
        return table;
    }

    private void initializeFilters() {
        typeFilterCombo.getItems().setAll(
                new TypeFilterOption(null, "Tous"),
                new TypeFilterOption(CalendarEntryType.EVENT, "Evenements"),
                new TypeFilterOption(CalendarEntryType.MEETING, "Reunions")
        );
        typeFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.getItems().clear();
        statusFilterCombo.getItems().add(new StatusFilterOption(null, "Tous"));
        for (ScheduleStatus status : ScheduleStatus.values()) {
            statusFilterCombo.getItems().add(new StatusFilterOption(status, status.label()));
        }
        statusFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadResponsibleFilters() {
        responsibleFilters.clear();
        responsibleFilters.add(new ResponsibleOption(null, "Tous"));
        for (Member member : memberService.getMembers("", MemberStatusFilter.ACTIVE)) {
            responsibleFilters.add(new ResponsibleOption(member.id(), member.fullName()));
        }
        responsibleFilterCombo.setItems(responsibleFilters);
        responsibleFilterCombo.getSelectionModel().selectFirst();
    }

    private void refreshEntries() {
        CalendarViewMode mode = currentMode();
        LocalDate fromDate;
        LocalDate toDate;
        switch (mode) {
            case MONTH -> {
                fromDate = anchorDate.withDayOfMonth(1);
                toDate = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth());
            }
            case WEEK -> {
                fromDate = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                toDate = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            }
            case LIST -> {
                fromDate = anchorDate;
                toDate = anchorDate.plusDays(45);
            }
            default -> throw new IllegalStateException("Mode calendrier non supporte.");
        }

        TypeFilterOption typeFilter = typeFilterCombo.getValue();
        StatusFilterOption statusFilter = statusFilterCombo.getValue();
        ResponsibleOption responsibleFilter = responsibleFilterCombo.getValue();

        entries.setAll(calendarService.getEntries(
                fromDate,
                toDate,
                typeFilter == null ? null : typeFilter.value(),
                statusFilter == null ? null : statusFilter.value(),
                responsibleFilter == null ? null : responsibleFilter.memberId(),
                categoryFilterField.getText()
        ));
        tableView.refresh();

        periodLabel.setText("Periode: " + fromDate + " -> " + toDate + " (" + mode + ")");
        long conflictCount = entries.stream().filter(CalendarEntry::conflict).count();
        summaryLabel.setText(String.format(Locale.FRANCE, "Elements: %d | conflits detectes: %d", entries.size(), conflictCount));
    }

    private void openDetail(CalendarEntry entry) {
        if (entry == null) {
            detailTitleLabel.setText("Aucun element selectionne");
            detailMetaLabel.setText("");
            detailDescriptionLabel.setText("");
            return;
        }

        if (entry.entryType() == CalendarEntryType.EVENT) {
            Event event = eventService.getEvent(entry.sourceId());
            detailTitleLabel.setText("[Evenement] " + event.title());
            detailMetaLabel.setText(
                    event.eventDate() + " "
                            + TIME_DISPLAY.format(event.eventTime()) + "-" + TIME_DISPLAY.format(event.endTime())
                            + " | " + event.statusLabel()
                            + " | " + defaultValue(event.location())
            );
            detailDescriptionLabel.setText(defaultValue(event.description()));
            return;
        }

        Meeting meeting = meetingService.getMeeting(entry.sourceId());
        detailTitleLabel.setText("[Reunion] " + meeting.title());
        detailMetaLabel.setText(
                meeting.meetingDate() + " "
                        + TIME_DISPLAY.format(meeting.startTime()) + "-" + TIME_DISPLAY.format(meeting.endTime())
                        + " | " + meeting.statusLabel()
                        + " | " + defaultValue(meeting.location())
        );
        detailDescriptionLabel.setText(defaultValue(meeting.agenda()));
    }

    private void quickCreateEntry() {
        try {
            CalendarEntryType type = quickTypeCombo.getValue();
            String title = quickTitleField.getText();
            LocalDate date = parseDate(quickDateField.getText());
            LocalTime startTime = parseTime(quickStartTimeField.getText());
            LocalTime endTime = parseTime(quickEndTimeField.getText());
            String location = quickLocationField.getText();
            Long responsibleId = quickResponsibleCombo.getValue() == null ? null : quickResponsibleCombo.getValue().id();
            ScheduleStatus status = quickStatusCombo.getValue();
            String category = quickCategoryField.getText();

            if (type == CalendarEntryType.EVENT) {
                eventService.addEvent(
                        title,
                        date,
                        startTime,
                        endTime,
                        location,
                        "",
                        null,
                        responsibleId,
                        status == null ? ScheduleStatus.CONFIRMED : status,
                        category
                );
            } else {
                String organizer = quickResponsibleCombo.getValue() == null ? null : quickResponsibleCombo.getValue().fullName();
                meetingService.addMeeting(
                        title,
                        date,
                        startTime,
                        endTime,
                        location,
                        organizer,
                        responsibleId,
                        "",
                        "",
                        status == null ? ScheduleStatus.PLANNED : status,
                        category,
                        ""
                );
            }

            AlertUtils.info(getScene().getWindow(), "Calendrier", "Element cree depuis le calendrier.");
            quickTitleField.clear();
            quickLocationField.clear();
            quickCategoryField.clear();
            quickResponsibleCombo.getSelectionModel().clearSelection();
            refreshEntries();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Calendrier", e.getMessage());
        }
    }

    private CalendarViewMode currentMode() {
        CalendarViewMode mode = viewModeCombo.getValue();
        return mode == null ? CalendarViewMode.MONTH : mode;
    }

    private LocalDate parseDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Date obligatoire (YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide. Format attendu: YYYY-MM-DD.");
        }
    }

    private LocalTime parseTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Heure obligatoire (HH:mm).");
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
