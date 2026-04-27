package com.monasso.app.ui.screen;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.Event;
import com.monasso.app.service.DashboardService;
import com.monasso.app.ui.component.StatCard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardScreen extends VBox {

    private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public enum QuickAction {
        ADD_PERSON,
        CREATE_EVENT,
        CREATE_MEETING,
        RECORD_CONTRIBUTION,
        EXPORT_DATA
    }

    private final DashboardService dashboardService;
    private final Consumer<QuickAction> quickActionHandler;

    private final StatCard membersCard = new StatCard("Personnes actives", "PS", "0", "Actifs sur total");
    private final StatCard upcomingEventsCard = new StatCard("Evenements a venir", "EV", "0", "Dans le calendrier");
    private final StatCard paidContributionsCard = new StatCard("Cotisations payees", "OK", "0", "Membres a jour");
    private final StatCard pendingContributionsCard = new StatCard("Cotisations en attente", "AT", "0", "Relances utiles");
    private final StatCard collectedCard = new StatCard("Total collecte", "EU", "0 EUR", "Periode courante");

    private final VBox upcomingEventsList = new VBox(8);
    private final Label coverageLabel = new Label();

    public DashboardScreen(DashboardService dashboardService, Consumer<QuickAction> quickActionHandler) {
        this.dashboardService = dashboardService;
        this.quickActionHandler = quickActionHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Indicateurs reels de l'association, relies a SQLite.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        HBox quickActions = createQuickActions();

        FlowPane cards = new FlowPane();
        cards.getStyleClass().add("dashboard-cards");
        cards.setHgap(12);
        cards.setVgap(12);
        cards.getChildren().addAll(
                membersCard,
                upcomingEventsCard,
                paidContributionsCard,
                pendingContributionsCard,
                collectedCard
        );

        VBox upcomingPanel = new VBox(10);
        upcomingPanel.getStyleClass().add("panel-card");
        Label upcomingTitle = new Label("Prochains evenements");
        upcomingTitle.getStyleClass().add("section-label");
        Label upcomingHint = new Label("Les 5 prochaines dates planifiees.");
        upcomingHint.getStyleClass().add("muted-text");
        upcomingEventsList.getStyleClass().add("event-list");
        coverageLabel.getStyleClass().add("screen-subtitle");
        upcomingPanel.getChildren().addAll(upcomingTitle, upcomingHint, new Separator(), upcomingEventsList, coverageLabel);

        getChildren().addAll(title, subtitle, quickActions, cards, upcomingPanel);
        refreshMetrics();
    }

    private HBox createQuickActions() {
        HBox row = new HBox(10);
        row.getStyleClass().addAll("panel-card", "action-row");
        row.getChildren().addAll(
                createQuickActionButton("Ajouter une personne", QuickAction.ADD_PERSON),
                createQuickActionButton("Creer un evenement", QuickAction.CREATE_EVENT),
                createQuickActionButton("Creer une reunion", QuickAction.CREATE_MEETING),
                createQuickActionButton("Enregistrer une cotisation", QuickAction.RECORD_CONTRIBUTION),
                createQuickActionButton("Exporter", QuickAction.EXPORT_DATA)
        );
        return row;
    }

    private Button createQuickActionButton(String label, QuickAction action) {
        Button button = new Button(label);
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> quickActionHandler.accept(action));
        return button;
    }

    private void refreshMetrics() {
        DashboardMetrics metrics = dashboardService.getMetrics();

        membersCard.setValue(String.valueOf(metrics.activeMembers()));
        membersCard.setHelperText(metrics.activeMembers() + " actifs / " + metrics.totalMembers() + " personnes");

        paidContributionsCard.setValue(String.valueOf(metrics.paidContributions()));
        paidContributionsCard.setHelperText("Periode " + metrics.currentPeriod());

        pendingContributionsCard.setValue(String.valueOf(metrics.pendingContributions()));
        pendingContributionsCard.setHelperText(metrics.pendingContributions() == 0 ? "Aucune relance" : "Relances necessaires");

        upcomingEventsCard.setValue(String.valueOf(metrics.upcomingEventsCount()));
        upcomingEventsCard.setHelperText("Calendrier des prochaines dates");

        collectedCard.setValue(String.format(Locale.FRANCE, "%.2f EUR", metrics.totalContributionAmount()));
        collectedCard.setHelperText("Periode " + metrics.currentPeriod());
        double ratio = metrics.activeMembers() == 0 ? 0 : (double) metrics.paidContributions() / metrics.activeMembers();
        coverageLabel.setText(String.format(Locale.FRANCE,
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
