package com.monasso.app.ui.screen;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.DashboardScheduleItem;
import com.monasso.app.model.DashboardTaskItem;
import com.monasso.app.service.DashboardService;
import com.monasso.app.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WelcomeScreen extends VBox {

    public enum WelcomeAction {
        OPEN_MEMBERS,
        OPEN_EVENTS,
        OPEN_MEETINGS,
        OPEN_TASKS,
        OPEN_DOCUMENTS,
        OPEN_CALENDAR,
        OPEN_DASHBOARD
    }

    private record GuideSection(String title, String description, WelcomeAction action) {
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardService dashboardService;
    private final Consumer<WelcomeAction> actionHandler;
    private final VBox nextActionsBox = new VBox(8);

    public WelcomeScreen(DashboardService dashboardService, Consumer<WelcomeAction> actionHandler) {
        this.dashboardService = dashboardService;
        this.actionHandler = actionHandler;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Bienvenue");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("MonAsso vous permet de gerer votre association facilement.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        Button fullGuideButton = new Button("Voir le guide complet");
        fullGuideButton.getStyleClass().add("primary-button");
        fullGuideButton.setOnAction(event -> showFullGuide());

        HBox headerActions = new HBox(10, fullGuideButton);
        headerActions.getStyleClass().add("action-row");

        getChildren().addAll(
                title,
                subtitle,
                headerActions,
                createGuideSectionsPanel(),
                createTodayPanel()
        );

        refreshNextActions();
    }

    private VBox createGuideSectionsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label sectionTitle = new Label("Guide rapide");
        sectionTitle.getStyleClass().add("section-label");

        FlowPane cards = new FlowPane();
        cards.setHgap(10);
        cards.setVgap(10);
        cards.setPrefWrapLength(1100);

        List<GuideSection> sections = List.of(
                new GuideSection(
                        "👤 Gerer les personnes",
                        "Ajoutez membres, benevoles, salaries, intervenants ou partenaires.\nRetrouvez rapidement une fiche et mettez a jour les infos utiles.",
                        WelcomeAction.OPEN_MEMBERS
                ),
                new GuideSection(
                        "📅 Suivre les evenements",
                        "Planifiez les dates, lieux et responsables.\nSuivez l'avancement, le statut et les points importants en un coup d'oeil.",
                        WelcomeAction.OPEN_EVENTS
                ),
                new GuideSection(
                        "🧾 Organiser les reunions",
                        "Preparez l'ordre du jour et les participants.\nGardez les notes de reunion et le statut de chaque seance.",
                        WelcomeAction.OPEN_MEETINGS
                ),
                new GuideSection(
                        "✅ Gerer les taches",
                        "Distribuez les actions a faire et fixez des echeances.\nSuivez ce qui est en cours, bloque ou termine.",
                        WelcomeAction.OPEN_TASKS
                ),
                new GuideSection(
                        "📂 Stocker les documents",
                        "Associez les fichiers aux personnes, evenements, reunions ou taches.\nRetrouvez chaque document sans chercher dans plusieurs dossiers.",
                        WelcomeAction.OPEN_DOCUMENTS
                )
        );

        for (GuideSection section : sections) {
            cards.getChildren().add(createGuideCard(section));
        }

        panel.getChildren().addAll(sectionTitle, cards);
        return panel;
    }

    private VBox createGuideCard(GuideSection section) {
        VBox card = new VBox(8);
        card.getStyleClass().add("panel-card");
        card.setMinWidth(280);
        card.setPrefWidth(340);
        card.setMaxWidth(380);

        Label title = new Label(section.title());
        title.getStyleClass().add("section-label");
        title.setWrapText(true);

        Label description = new Label(section.description());
        description.getStyleClass().add("screen-subtitle");
        description.setWrapText(true);

        Button openButton = new Button("Acceder");
        openButton.getStyleClass().add("accent-button");
        openButton.setOnAction(event -> actionHandler.accept(section.action()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(title, description, spacer, openButton);
        return card;
    }

    private VBox createTodayPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("Que faire aujourd'hui ?");
        title.getStyleClass().add("section-label");

        Label shortcutHint = new Label("Raccourcis rapides");
        shortcutHint.getStyleClass().add("muted-text");

        FlowPane shortcuts = new FlowPane();
        shortcuts.setHgap(10);
        shortcuts.setVgap(10);
        shortcuts.getChildren().addAll(
                createShortcutButton("Personnes", WelcomeAction.OPEN_MEMBERS),
                createShortcutButton("Evenements", WelcomeAction.OPEN_EVENTS),
                createShortcutButton("Reunions", WelcomeAction.OPEN_MEETINGS),
                createShortcutButton("Taches", WelcomeAction.OPEN_TASKS),
                createShortcutButton("Calendrier", WelcomeAction.OPEN_CALENDAR),
                createShortcutButton("Tableau de bord", WelcomeAction.OPEN_DASHBOARD)
        );

        Label nextActionsTitle = new Label("Prochaines actions");
        nextActionsTitle.getStyleClass().add("section-label");

        nextActionsBox.getStyleClass().add("event-list");

        panel.getChildren().addAll(title, shortcutHint, shortcuts, nextActionsTitle, nextActionsBox);
        return panel;
    }

    private Button createShortcutButton(String label, WelcomeAction action) {
        Button button = new Button(label);
        button.getStyleClass().add("ghost-button");
        button.setOnAction(event -> actionHandler.accept(action));
        return button;
    }

    private void refreshNextActions() {
        nextActionsBox.getChildren().clear();

        List<String> lines = new ArrayList<>();
        try {
            DashboardMetrics metrics = dashboardService.getMetrics();

            for (DashboardScheduleItem item : metrics.todayItems().stream().limit(2).toList()) {
                lines.add("Aujourd'hui: " + item.title() + " a " + formatTime(item.startTime()));
            }
            for (DashboardTaskItem item : metrics.urgentTaskItems().stream().limit(2).toList()) {
                String due = item.dueDate() == null ? "sans echeance" : item.dueDate().toString();
                lines.add("Tache urgente: " + item.title() + " (" + due + ")");
            }
            for (DashboardScheduleItem item : metrics.nearbyMeetingItems().stream().limit(1).toList()) {
                lines.add("Prochaine reunion: " + item.title() + " le " + item.date());
            }
            for (DashboardScheduleItem item : metrics.eventsToMonitor().stream().limit(1).toList()) {
                lines.add("Prochain evenement: " + item.title() + " le " + item.date());
            }
        } catch (Exception e) {
            lines.add("Impossible de charger les prochaines actions pour le moment.");
        }

        if (lines.isEmpty()) {
            lines.add("Aucune action urgente. Vous pouvez preparer la semaine depuis le calendrier.");
        }

        for (String line : lines) {
            Label label = new Label("• " + line);
            label.getStyleClass().add("screen-subtitle");
            label.setWrapText(true);
            nextActionsBox.getChildren().add(label);
        }
    }

    private void showFullGuide() {
        String guide = """
                1. Commencez par ajouter les personnes.
                2. Creez vos evenements et reunions a venir.
                3. Ajoutez les taches a faire et les responsables.
                4. Rangez les documents au bon endroit.
                5. Revenez chaque jour sur cette page pour suivre les priorites.
                """;
        AlertUtils.info(getScene() == null ? null : getScene().getWindow(), "Guide complet", guide);
    }

    private String formatTime(java.time.LocalTime time) {
        if (time == null) {
            return "--:--";
        }
        return TIME_FORMAT.format(time);
    }
}
