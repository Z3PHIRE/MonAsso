package com.monasso.app.ui.screen;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.DashboardScheduleItem;
import com.monasso.app.model.DashboardTaskItem;
import com.monasso.app.service.DashboardService;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardScreen extends VBox {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public enum QuickAction {
        ADD_PERSON,
        CREATE_EVENT,
        CREATE_MEETING,
        RECORD_CONTRIBUTION,
        EXPORT_DATA
    }

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

    private final DashboardService dashboardService;
    @SuppressWarnings("unused")
    private final Consumer<QuickAction> quickActionHandler;
    private final ComboBox<DisplayMode> displayModeCombo = new ComboBox<>();

    private final VBox todayList = new VBox(6);
    private final VBox weekList = new VBox(6);
    private final VBox urgentTasksList = new VBox(6);
    private final VBox upcomingEventsList = new VBox(6);
    private final VBox nearbyMeetingsList = new VBox(6);

    public DashboardScreen(DashboardService dashboardService, Consumer<QuickAction> quickActionHandler) {
        this.dashboardService = dashboardService;
        this.quickActionHandler = quickActionHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Vue quotidienne epuree: aujourd'hui, semaine, urgences, prochains evenements et prochaines reunions.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        displayModeCombo.getItems().setAll(DisplayMode.values());
        displayModeCombo.getSelectionModel().select(DisplayMode.COMPACT);
        displayModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshMetrics());

        HBox modeRow = new HBox(10, new Label("Mode"), displayModeCombo);
        modeRow.getStyleClass().add("action-row");

        HBox firstRow = new HBox(
                12,
                createWidget("Aujourd'hui", "Elements du jour", todayList),
                createWidget("Cette semaine", "Prochains 7 jours", weekList),
                createWidget("Taches urgentes", "Echeances sur 7 jours", urgentTasksList)
        );
        firstRow.getStyleClass().add("action-row");

        HBox secondRow = new HBox(
                12,
                createWidget("Prochains evenements", "Evenements a surveiller", upcomingEventsList),
                createWidget("Reunions proches", "Prochaines reunions", nearbyMeetingsList)
        );
        secondRow.getStyleClass().add("action-row");

        getChildren().addAll(title, subtitle, modeRow, firstRow, secondRow);
        refreshMetrics();
    }

    private VBox createWidget(String title, String hint, VBox listContainer) {
        VBox widget = new VBox(8);
        widget.getStyleClass().add("panel-card");
        widget.setMinWidth(0);

        Label widgetTitle = new Label(title);
        widgetTitle.getStyleClass().add("section-label");

        Label widgetHint = new Label(hint);
        widgetHint.getStyleClass().add("muted-text");

        listContainer.getStyleClass().add("event-list");
        widget.getChildren().addAll(widgetTitle, widgetHint, listContainer);
        HBox.setHgrow(widget, Priority.ALWAYS);
        return widget;
    }

    private void refreshMetrics() {
        DashboardMetrics metrics = dashboardService.getMetrics();
        LocalDate weekLimit = LocalDate.now().plusDays(7);
        List<DashboardScheduleItem> weeklyItems = metrics.upcomingItems()
                .stream()
                .filter(item -> !item.date().isAfter(weekLimit))
                .toList();

        renderScheduleList(todayList, metrics.todayItems(), "Aucun element aujourd'hui.");
        renderScheduleList(weekList, weeklyItems, "Aucun element cette semaine.");
        renderTaskList(urgentTasksList, metrics.urgentTaskItems(), "Aucune tache urgente.");
        renderScheduleList(upcomingEventsList, metrics.eventsToMonitor(), "Aucun evenement a surveiller.");
        renderScheduleList(nearbyMeetingsList, metrics.nearbyMeetingItems(), "Aucune reunion proche.");
    }

    private void renderScheduleList(VBox target, List<DashboardScheduleItem> items, String emptyText) {
        target.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label empty = new Label(emptyText);
            empty.getStyleClass().add("muted-text");
            target.getChildren().add(empty);
            return;
        }

        int max = Math.min(items.size(), currentMode() == DisplayMode.COMPACT ? 4 : 8);
        for (int i = 0; i < max; i++) {
            DashboardScheduleItem item = items.get(i);
            String start = item.startTime() == null ? "--:--" : TIME_FORMAT.format(item.startTime());
            String end = item.endTime() == null ? "--:--" : TIME_FORMAT.format(item.endTime());
            String label = "["
                    + item.type().label()
                    + "] "
                    + DATE_FORMAT.format(item.date())
                    + " "
                    + start
                    + "-"
                    + end
                    + " | "
                    + item.title();
            if (item.statusLabel() != null && !item.statusLabel().isBlank()) {
                label = label + " | " + item.statusLabel();
            }
            Label line = new Label(label);
            line.getStyleClass().add("screen-subtitle");
            line.setWrapText(true);
            target.getChildren().add(line);
        }
    }

    private void renderTaskList(VBox target, List<DashboardTaskItem> items, String emptyText) {
        target.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label empty = new Label(emptyText);
            empty.getStyleClass().add("muted-text");
            target.getChildren().add(empty);
            return;
        }

        int max = Math.min(items.size(), currentMode() == DisplayMode.COMPACT ? 5 : 10);
        for (int i = 0; i < max; i++) {
            DashboardTaskItem item = items.get(i);
            String due = item.dueDate() == null ? "Sans echeance" : item.dueDate().toString();
            String assignee = item.assigneeName() == null || item.assigneeName().isBlank() ? "Non assignee" : item.assigneeName();
            String linked = item.linkedLabel() == null || item.linkedLabel().isBlank() ? "Sans lien" : item.linkedLabel();
            String label = String.format(
                    Locale.FRANCE,
                    "%s | %s | %s | %s | %s",
                    item.priority().label(),
                    due,
                    assignee,
                    linked,
                    item.title()
            );
            Label line = new Label(label);
            line.getStyleClass().add("screen-subtitle");
            line.setWrapText(true);
            target.getChildren().add(line);
        }
    }

    private DisplayMode currentMode() {
        DisplayMode mode = displayModeCombo.getValue();
        return mode == null ? DisplayMode.COMPACT : mode;
    }
}
