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

    private final DashboardService dashboardService;
    private final StatCard membersCard = new StatCard("Membres", "MB", "0", "Adherents enregistres");
    private final StatCard eventsCard = new StatCard("Evenements", "EV", "0", "Historique total");
    private final StatCard paidContributionsCard = new StatCard("Cotisations payees", "OK", "0", "Membres a jour");
    private final StatCard pendingContributionsCard = new StatCard("Cotisations en attente", "AT", "0", "Relances necessaires");
    private final StatCard upcomingEventsCard = new StatCard("Prochains evenements", "NX", "0", "Planning a venir");

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

        Label subtitle = new Label("Vue visuelle des indicateurs de l'association (cotisations calculees sur l'annee en cours).");
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
        Label contributionTitle = new Label("Indicateur de collecte");
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
        eventsCard.setValue(String.valueOf(metrics.totalEvents()));
        paidContributionsCard.setValue(String.valueOf(metrics.paidContributions()));
        pendingContributionsCard.setValue(String.valueOf(metrics.pendingContributions()));
        upcomingEventsCard.setValue(String.valueOf(metrics.upcomingEventsCount()));

        membersCard.setHelperText(metrics.totalMembers() == 0 ? "Commencez par ajouter un membre." : "Base active");
        paidContributionsCard.setHelperText("Annee " + java.time.LocalDate.now().getYear());
        pendingContributionsCard.setHelperText(metrics.pendingContributions() == 0 ? "Aucune relance." : "A relancer rapidement");
        upcomingEventsCard.setHelperText(metrics.upcomingEventsCount() == 0 ? "Aucun evenement futur" : "Dates deja planifiees");

        totalCollectedLabel.setText(String.format(Locale.FRANCE, "Montant collecte : %.2f EUR", metrics.totalContributionAmount()));

        double ratio = metrics.totalMembers() == 0 ? 0 : (double) metrics.paidContributions() / metrics.totalMembers();
        contributionProgressBar.setProgress(Math.min(Math.max(ratio, 0), 1));
        progressLabel.setText(String.format(Locale.FRANCE,
                "Couverture cotisations: %.0f%% (%d/%d membres)",
                ratio * 100,
                metrics.paidContributions(),
                metrics.totalMembers()));

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
            Label nameLabel = new Label(event.name());
            nameLabel.getStyleClass().add("event-name");

            String location = event.location() == null || event.location().isBlank() ? "Lieu non renseigne" : event.location();
            Label locationLabel = new Label(location);
            locationLabel.getStyleClass().add("muted-text");
            details.getChildren().addAll(nameLabel, locationLabel);

            row.getChildren().addAll(dateLabel, details);
            upcomingEventsList.getChildren().add(row);
        }
    }
}
