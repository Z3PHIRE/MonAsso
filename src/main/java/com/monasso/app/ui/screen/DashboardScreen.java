package com.monasso.app.ui.screen;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.Event;
import com.monasso.app.service.DashboardService;
import com.monasso.app.ui.component.StatCard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardScreen extends VBox {

    private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardService dashboardService;
    private final StatCard membersCard = new StatCard("Membres", "MB", "0", "Membres actifs : 0");
    private final StatCard eventsCard = new StatCard("Evenements", "EV", "0", "Planning general");
    private final StatCard paidContributionsCard = new StatCard("Cotisations payees", "OK", "0", "Paye");
    private final StatCard pendingContributionsCard = new StatCard("Cotisations en attente", "AT", "0", "Relances");
    private final StatCard upcomingEventsCard = new StatCard("Prochains evenements", "NX", "0", "A venir");

    private final VBox upcomingEventsList = new VBox(8);
    private final Label totalCollectedLabel = new Label();
    private final Label progressLabel = new Label();
    private final ProgressBar contributionProgressBar = new ProgressBar(0);

    public DashboardScreen(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Indicateurs reels de l'association, relies a SQLite.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        Button refreshButton = new Button("Actualiser");
        refreshButton.getStyleClass().add("primary-button");
        refreshButton.setOnAction(event -> refreshMetrics());

        HBox actions = new HBox(10, refreshButton);
        actions.getStyleClass().add("action-row");

        FlowPane cards = new FlowPane();
        cards.getStyleClass().add("dashboard-cards");
        cards.setHgap(12);
        cards.setVgap(12);
        cards.getChildren().addAll(
                membersCard,
                eventsCard,
                paidContributionsCard,
                pendingContributionsCard,
                upcomingEventsCard
        );

        VBox contributionPanel = new VBox(10);
        contributionPanel.getStyleClass().add("panel-card");
        Label contributionTitle = new Label("Collecte sur la periode courante");
        contributionTitle.getStyleClass().add("section-label");
        totalCollectedLabel.getStyleClass().add("screen-subtitle");
        progressLabel.getStyleClass().add("muted-text");
        contributionProgressBar.getStyleClass().add("progress-meter");
        contributionProgressBar.setPrefWidth(420);
        contributionPanel.getChildren().addAll(contributionTitle, totalCollectedLabel, contributionProgressBar, progressLabel);

        VBox upcomingPanel = new VBox(10);
        upcomingPanel.getStyleClass().add("panel-card");
        Label upcomingTitle = new Label("Prochains evenements");
        upcomingTitle.getStyleClass().add("section-label");
        Label upcomingHint = new Label("Les 5 prochaines dates planifiees.");
        upcomingHint.getStyleClass().add("muted-text");
        upcomingEventsList.getStyleClass().add("event-list");
        upcomingPanel.getChildren().addAll(upcomingTitle, upcomingHint, upcomingEventsList);

        HBox lowerPanels = new HBox(12, contributionPanel, upcomingPanel);
        HBox.setHgrow(contributionPanel, Priority.ALWAYS);
        HBox.setHgrow(upcomingPanel, Priority.ALWAYS);

        getChildren().addAll(title, subtitle, actions, cards, lowerPanels);
        refreshMetrics();
    }

    private void refreshMetrics() {
        DashboardMetrics metrics = dashboardService.getMetrics();

        membersCard.setValue(String.valueOf(metrics.totalMembers()));
        membersCard.setHelperText("Membres actifs : " + metrics.activeMembers());

        eventsCard.setValue(String.valueOf(metrics.totalEvents()));
        eventsCard.setHelperText(metrics.upcomingEventsCount() + " evenements a venir");

        paidContributionsCard.setValue(String.valueOf(metrics.paidContributions()));
        paidContributionsCard.setHelperText("Periode " + metrics.currentPeriod());

        pendingContributionsCard.setValue(String.valueOf(metrics.pendingContributions()));
        pendingContributionsCard.setHelperText(metrics.pendingContributions() == 0 ? "Aucune relance" : "Relances necessaires");

        upcomingEventsCard.setValue(String.valueOf(metrics.upcomingEventsCount()));
        upcomingEventsCard.setHelperText("Calendrier des prochaines dates");

        totalCollectedLabel.setText(String.format(Locale.FRANCE, "Montant collecte (%s) : %.2f EUR",
                metrics.currentPeriod(),
                metrics.totalContributionAmount()));

        double ratio = metrics.activeMembers() == 0 ? 0 : (double) metrics.paidContributions() / metrics.activeMembers();
        contributionProgressBar.setProgress(Math.min(Math.max(ratio, 0), 1));
        progressLabel.setText(String.format(Locale.FRANCE,
                "Couverture cotisations: %.0f%% (%d/%d membres actifs)",
                ratio * 100,
                metrics.paidContributions(),
                metrics.activeMembers()));

        refreshUpcomingEvents(metrics);
    }

    private void refreshUpcomingEvents(DashboardMetrics metrics) {
        upcomingEventsList.getChildren().clear();
        if (metrics.upcomingEvents().isEmpty()) {
            Label empty = new Label("Aucun evenement a venir.");
            empty.getStyleClass().add("screen-subtitle");
            upcomingEventsList.getChildren().add(empty);
            return;
        }

        for (Event event : metrics.upcomingEvents()) {
            HBox row = new HBox(10);
            row.getStyleClass().add("event-list-item");

            Label dateLabel = new Label(EVENT_DATE_FORMAT.format(event.eventDate()));
            dateLabel.getStyleClass().add("event-date-chip");

            VBox details = new VBox(2);
            Label nameLabel = new Label(event.title() + " - " + EVENT_TIME_FORMAT.format(event.eventTime()));
            nameLabel.getStyleClass().add("event-name");

            String location = event.location() == null || event.location().isBlank() ? "Lieu non renseigne" : event.location();
            String capacityInfo = event.capacity() == null ? "capacite libre" : event.participantCount() + "/" + event.capacity() + " participants";
            Label locationLabel = new Label(location + " | " + capacityInfo);
            locationLabel.getStyleClass().add("muted-text");
            details.getChildren().addAll(nameLabel, locationLabel);

            row.getChildren().addAll(dateLabel, details);
            upcomingEventsList.getChildren().add(row);
        }
    }
}
