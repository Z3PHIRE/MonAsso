package com.monasso.app.ui.screen;

import com.monasso.app.model.ContributionReminder;
import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.DashboardScheduleItem;
import com.monasso.app.model.DashboardTaskItem;
import com.monasso.app.service.DashboardService;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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

    private final DashboardService dashboardService;
    @SuppressWarnings("unused")
    private final Consumer<QuickAction> quickActionHandler;

    private final VBox todayList = new VBox(6);
    private final VBox upcomingList = new VBox(6);
    private final VBox urgentTasksList = new VBox(6);
    private final VBox remindersList = new VBox(6);
    private final VBox eventsToMonitorList = new VBox(6);
    private final VBox nearbyMeetingsList = new VBox(6);

    public DashboardScreen(DashboardService dashboardService, Consumer<QuickAction> quickActionHandler) {
        this.dashboardService = dashboardService;
        this.quickActionHandler = quickActionHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Vue quotidienne: aujourd'hui, a venir, urgences, relances et points de suivi.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        HBox firstRow = new HBox(
                12,
                createWidget("Aujourd'hui", "Elements du jour", todayList),
                createWidget("A venir", "Prochains 14 jours", upcomingList),
                createWidget("Taches urgentes", "Echeances sur 7 jours", urgentTasksList)
        );
        firstRow.getStyleClass().add("action-row");

        HBox secondRow = new HBox(
                12,
                createWidget("Cotisations a relancer", "Membres actifs non regles", remindersList),
                createWidget("Evenements a surveiller", "Prochains evenements", eventsToMonitorList),
                createWidget("Reunions proches", "Prochaines reunions", nearbyMeetingsList)
        );
        secondRow.getStyleClass().add("action-row");

        getChildren().addAll(title, subtitle, firstRow, secondRow);
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

        renderScheduleList(todayList, metrics.todayItems(), "Aucun element aujourd'hui.");
        renderScheduleList(upcomingList, metrics.upcomingItems(), "Aucun element a venir.");
        renderTaskList(urgentTasksList, metrics.urgentTaskItems(), "Aucune tache urgente.");
        renderContributionList(remindersList, metrics.contributionReminders(), "Aucune relance necessaire.");
        renderScheduleList(eventsToMonitorList, metrics.eventsToMonitor(), "Aucun evenement a surveiller.");
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

        int max = Math.min(items.size(), 6);
        for (int i = 0; i < max; i++) {
            DashboardScheduleItem item = items.get(i);
            String start = item.startTime() == null ? "--:--" : TIME_FORMAT.format(item.startTime());
            String end = item.endTime() == null ? "--:--" : TIME_FORMAT.format(item.endTime());
            String label = item.type().label()
                    + " | "
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

        int max = Math.min(items.size(), 8);
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

    private void renderContributionList(VBox target, List<ContributionReminder> items, String emptyText) {
        target.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label empty = new Label(emptyText);
            empty.getStyleClass().add("muted-text");
            target.getChildren().add(empty);
            return;
        }

        int max = Math.min(items.size(), 8);
        for (int i = 0; i < max; i++) {
            ContributionReminder item = items.get(i);
            String email = item.email() == null || item.email().isBlank() ? "sans email" : item.email();
            Label line = new Label(item.memberName() + " | " + email);
            line.getStyleClass().add("screen-subtitle");
            line.setWrapText(true);
            target.getChildren().add(line);
        }
    }
}
