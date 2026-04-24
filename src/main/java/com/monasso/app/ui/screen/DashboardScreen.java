package com.monasso.app.ui.screen;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.service.DashboardService;
import com.monasso.app.ui.component.StatCard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;

public class DashboardScreen extends VBox {

    private final DashboardService dashboardService;
    private final StatCard membersCard = new StatCard("Membres", "0");
    private final StatCard eventsCard = new StatCard("Evenements", "0");
    private final StatCard contributionsCountCard = new StatCard("Cotisations", "0");
    private final StatCard totalAmountCard = new StatCard("Total collecte", "0,00 EUR");

    public DashboardScreen(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Vue synthese de l'association, basee sur les donnees SQLite locales.");
        subtitle.getStyleClass().add("screen-subtitle");

        FlowPane cards = new FlowPane();
        cards.setHgap(12);
        cards.setVgap(12);
        cards.getChildren().addAll(membersCard, eventsCard, contributionsCountCard, totalAmountCard);

        Button refreshButton = new Button("Actualiser");
        refreshButton.getStyleClass().add("primary-button");
        refreshButton.setOnAction(event -> refreshMetrics());

        HBox actions = new HBox(10, refreshButton);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox note = new VBox(6);
        note.getStyleClass().add("panel-card");
        Label noteTitle = new Label("Actions rapides");
        noteTitle.getStyleClass().add("section-label");
        Label noteText = new Label("Utilisez la navigation a gauche pour ajouter des membres, planifier des evenements, enregistrer des cotisations puis exporter.");
        noteText.setWrapText(true);
        noteText.getStyleClass().add("screen-subtitle");
        note.getChildren().addAll(noteTitle, noteText);

        getChildren().addAll(title, subtitle, actions, cards, note);
        refreshMetrics();
    }

    private void refreshMetrics() {
        DashboardMetrics metrics = dashboardService.getMetrics();
        membersCard.setValue(String.valueOf(metrics.totalMembers()));
        eventsCard.setValue(String.valueOf(metrics.totalEvents()));
        contributionsCountCard.setValue(String.valueOf(metrics.totalContributions()));
        totalAmountCard.setValue(String.format(Locale.FRANCE, "%.2f EUR", metrics.totalContributionAmount()));
    }
}
