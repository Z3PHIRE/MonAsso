package com.monasso.app.ui.screen;

import com.monasso.app.model.CalendarEntryType;
import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.DashboardScheduleItem;
import com.monasso.app.model.DashboardTaskItem;
import com.monasso.app.model.Event;
import com.monasso.app.model.TaskItem;
import com.monasso.app.service.DashboardService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.TaskService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class DailyUseScreen extends VBox {

    public enum DailyAction {
        CREATE_EVENT,
        CREATE_TASK,
        ADD_PERSON,
        OPEN_DASHBOARD,
        OPEN_CALENDAR,
        OPEN_MEMBERS,
        OPEN_EVENTS,
        OPEN_MEETINGS,
        OPEN_TASKS,
        OPEN_DOCUMENTS
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardService dashboardService;
    private final TaskService taskService;
    private final EventService eventService;
    private final Consumer<DailyAction> actionHandler;

    private final VBox todayEventsBox = new VBox(6);
    private final VBox todayMeetingsBox = new VBox(6);
    private final VBox todayUrgentTasksBox = new VBox(6);
    private final VBox weekBox = new VBox(6);
    private final VBox overdueTasksBox = new VBox(6);
    private final VBox eventsToPrepareBox = new VBox(6);

    public DailyUseScreen(
            DashboardService dashboardService,
            TaskService taskService,
            EventService eventService,
            Consumer<DailyAction> actionHandler
    ) {
        this.dashboardService = dashboardService;
        this.taskService = taskService;
        this.eventService = eventService;
        this.actionHandler = actionHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Utilisation quotidienne");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Ce qui est important aujourd'hui, puis cette semaine.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        Button refreshButton = new Button("Actualiser");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshData());
        HBox topActions = new HBox(10, refreshButton);
        topActions.getStyleClass().add("action-row");

        HBox row1 = new HBox(12, createTodayPanel(), createWeekPanel());
        row1.getStyleClass().add("action-row");

        HBox row2 = new HBox(12, createAlertsPanel(), createQuickActionsPanel(), createNavigationPanel());
        row2.getStyleClass().add("action-row");

        getChildren().addAll(title, subtitle, topActions, row1, row2);
        refreshData();
    }

    private VBox createTodayPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label section = new Label("Aujourd'hui");
        section.getStyleClass().add("section-label");

        panel.getChildren().addAll(
                section,
                createSubBlock("Evenements du jour", todayEventsBox),
                createSubBlock("Reunions du jour", todayMeetingsBox),
                createSubBlock("Taches urgentes", todayUrgentTasksBox)
        );
        return panel;
    }

    private VBox createWeekPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label section = new Label("A venir (semaine)");
        section.getStyleClass().add("section-label");

        panel.getChildren().addAll(section, weekBox);
        return panel;
    }

    private VBox createAlertsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label section = new Label("Alertes");
        section.getStyleClass().add("section-label");

        panel.getChildren().addAll(
                section,
                createSubBlock("Taches en retard", overdueTasksBox),
                createSubBlock("Evenements a preparer", eventsToPrepareBox)
        );
        return panel;
    }

    private VBox createQuickActionsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label section = new Label("Actions rapides");
        section.getStyleClass().add("section-label");

        Button createEventButton = new Button("Creer evenement");
        createEventButton.getStyleClass().add("accent-button");
        createEventButton.setOnAction(event -> actionHandler.accept(DailyAction.CREATE_EVENT));

        Button createTaskButton = new Button("Creer tache");
        createTaskButton.getStyleClass().add("accent-button");
        createTaskButton.setOnAction(event -> actionHandler.accept(DailyAction.CREATE_TASK));

        Button addPersonButton = new Button("Ajouter personne");
        addPersonButton.getStyleClass().add("accent-button");
        addPersonButton.setOnAction(event -> actionHandler.accept(DailyAction.ADD_PERSON));

        panel.getChildren().addAll(section, createEventButton, createTaskButton, addPersonButton);
        return panel;
    }

    private VBox createNavigationPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label section = new Label("Navigation rapide");
        section.getStyleClass().add("section-label");

        FlowPane links = new FlowPane();
        links.setHgap(8);
        links.setVgap(8);

        links.getChildren().addAll(
                createNavButton("Tableau de bord", DailyAction.OPEN_DASHBOARD),
                createNavButton("Calendrier", DailyAction.OPEN_CALENDAR),
                createNavButton("Personnes", DailyAction.OPEN_MEMBERS),
                createNavButton("Evenements", DailyAction.OPEN_EVENTS),
                createNavButton("Reunions", DailyAction.OPEN_MEETINGS),
                createNavButton("Taches", DailyAction.OPEN_TASKS),
                createNavButton("Documents", DailyAction.OPEN_DOCUMENTS)
        );

        panel.getChildren().addAll(section, links);
        return panel;
    }

    private Button createNavButton(String label, DailyAction action) {
        Button button = new Button(label);
        button.getStyleClass().add("ghost-button");
        button.setOnAction(event -> actionHandler.accept(action));
        return button;
    }

    private VBox createSubBlock(String title, VBox contentBox) {
        VBox block = new VBox(6);
        Label subTitle = new Label(title);
        subTitle.getStyleClass().add("muted-text");
        block.getChildren().addAll(subTitle, contentBox);
        return block;
    }

    private void refreshData() {
        DashboardMetrics metrics = loadMetrics();
        if (metrics != null) {
            renderToday(metrics);
            renderWeek(metrics);
        } else {
            renderScheduleLines(todayEventsBox, List.of(), "Chargement des evenements indisponible.");
            renderScheduleLines(todayMeetingsBox, List.of(), "Chargement des reunions indisponible.");
            renderTaskLines(todayUrgentTasksBox, List.of(), "Chargement des taches indisponible.");
            renderScheduleLines(weekBox, List.of(), "Chargement des elements de la semaine indisponible.");
        }
        renderAlerts();
    }

    private void renderToday(DashboardMetrics metrics) {
        List<DashboardScheduleItem> todayEvents = metrics.todayItems().stream()
                .filter(item -> item.type() == CalendarEntryType.EVENT)
                .limit(3)
                .toList();
        List<DashboardScheduleItem> todayMeetings = metrics.todayItems().stream()
                .filter(item -> item.type() == CalendarEntryType.MEETING)
                .limit(3)
                .toList();
        List<DashboardTaskItem> urgentTasks = metrics.urgentTaskItems().stream()
                .limit(4)
                .toList();

        renderScheduleLines(todayEventsBox, todayEvents, "Aucun evenement aujourd'hui.");
        renderScheduleLines(todayMeetingsBox, todayMeetings, "Aucune reunion aujourd'hui.");
        renderTaskLines(todayUrgentTasksBox, urgentTasks, "Aucune tache urgente.");
    }

    private void renderWeek(DashboardMetrics metrics) {
        LocalDate weekLimit = LocalDate.now().plusDays(7);
        List<DashboardScheduleItem> weekItems = metrics.upcomingItems().stream()
                .filter(item -> !item.date().isAfter(weekLimit))
                .limit(6)
                .toList();
        renderScheduleLines(weekBox, weekItems, "Aucun element sur les 7 prochains jours.");
    }

    private void renderAlerts() {
        List<TaskItem> overdueTasks = loadOverdueTasks();
        List<Event> eventsToPrepare = loadEventsToPrepare();

        renderOverdueTaskLines(
                overdueTasksBox,
                overdueTasks,
                overdueTasks == null ? "Chargement des alertes taches indisponible." : "Aucune tache en retard."
        );
        renderEventPrepareLines(
                eventsToPrepareBox,
                eventsToPrepare,
                eventsToPrepare == null ? "Chargement des alertes evenements indisponible." : "Aucun evenement urgent a preparer."
        );
    }

    private void renderScheduleLines(VBox box, List<DashboardScheduleItem> items, String emptyText) {
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            box.getChildren().add(mutedLine(emptyText));
            return;
        }
        for (DashboardScheduleItem item : items) {
            String line = item.date() + " " + formatTime(item.startTime()) + " | " + item.title();
            if (item.statusLabel() != null && !item.statusLabel().isBlank()) {
                line += " (" + item.statusLabel() + ")";
            }
            box.getChildren().add(normalLine(line));
        }
    }

    private void renderTaskLines(VBox box, List<DashboardTaskItem> items, String emptyText) {
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            box.getChildren().add(mutedLine(emptyText));
            return;
        }
        for (DashboardTaskItem item : items) {
            String due = item.dueDate() == null ? "sans echeance" : item.dueDate().toString();
            box.getChildren().add(normalLine(item.title() + " | " + due));
        }
    }

    private void renderOverdueTaskLines(VBox box, List<TaskItem> items, String emptyText) {
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            box.getChildren().add(mutedLine(emptyText));
            return;
        }
        for (TaskItem item : items) {
            String due = item.dueDate() == null ? "sans echeance" : item.dueDate().toString();
            box.getChildren().add(normalLine(item.title() + " | echeance " + due));
        }
    }

    private void renderEventPrepareLines(VBox box, List<Event> items, String emptyText) {
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            box.getChildren().add(mutedLine(emptyText));
            return;
        }
        for (Event item : items) {
            box.getChildren().add(normalLine(item.eventDate() + " | " + item.title() + " (" + item.statusLabel() + ")"));
        }
    }

    private Label normalLine(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("screen-subtitle");
        label.setWrapText(true);
        return label;
    }

    private Label mutedLine(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        label.setWrapText(true);
        return label;
    }

    private String formatTime(java.time.LocalTime time) {
        if (time == null) {
            return "--:--";
        }
        return TIME_FORMAT.format(time);
    }

    private DashboardMetrics loadMetrics() {
        try {
            return dashboardService.getMetrics();
        } catch (Exception e) {
            return null;
        }
    }

    private List<TaskItem> loadOverdueTasks() {
        try {
            return taskService.getOverdueTasks(4);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Event> loadEventsToPrepare() {
        try {
            return eventService.getEventsToPrepare(4);
        } catch (Exception e) {
            return null;
        }
    }
}
