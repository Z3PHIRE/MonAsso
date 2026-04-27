package com.monasso.app.ui.screen;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.service.ContributionService;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ContributionsScreen extends VBox {

    private final ContributionService contributionService;
    private final MemberService memberService;

    private final ObservableList<Contribution> contributions = FXCollections.observableArrayList();
    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private final ObservableList<Contribution> memberHistory = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final TextField periodFilterField = new TextField();
    private final ComboBox<ContributionStatus> statusFilterCombo = new ComboBox<>();
    private final Label tableSummary = new Label();

    private final ComboBox<Member> memberCombo = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final TextField dateField = new TextField(LocalDate.now().toString());
    private final TextField periodField = new TextField();
    private final ComboBox<ContributionStatus> statusCombo = new ComboBox<>();
    private final TextField paymentMethodField = new TextField();
    private final TextArea notesArea = new TextArea();

    private final TableView<Contribution> contributionsTable = createContributionsTable();
    private final TableView<Contribution> historyTable = createHistoryTable();

    public ContributionsScreen(ContributionService contributionService, MemberService memberService) {
        this.contributionService = contributionService;
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Cotisations");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Enregistrez les paiements, suivez l'historique et filtrez les cotisations.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox.setVgrow(contributionsTable, Priority.ALWAYS);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");
        TitledPane formPane = new TitledPane("Saisie cotisation", createFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(false);
        TitledPane historyPane = new TitledPane("Historique du membre", createHistoryPanel());
        historyPane.getStyleClass().add("folded-panel");
        historyPane.setExpanded(false);

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                tableSummary,
                contributionsTable,
                formPane,
                historyPane
        );

        loadMembers();
        refreshContributions();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Filtrer les cotisations");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher un membre, une periode ou des notes");
        periodFilterField.setPromptText("Periode (ex: 2026)");
        periodFilterField.setText(contributionService.currentPeriod());
        statusFilterCombo.getItems().setAll(ContributionStatus.values());
        statusFilterCombo.setPromptText("Tous statuts");

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshContributions());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem resetItem = new MenuItem("Reinitialiser filtres");
        resetItem.setOnAction(event -> {
            searchField.clear();
            periodFilterField.setText(contributionService.currentPeriod());
            statusFilterCombo.getSelectionModel().clearSelection();
            refreshContributions();
        });
        moreButton.getItems().add(resetItem);

        HBox row = new HBox(
                10,
                new Label("Recherche"), searchField,
                new Label("Periode"), periodFilterField,
                new Label("Statut"), statusFilterCombo,
                applyButton, moreButton
        );
        row.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        panel.getChildren().addAll(section, row);
        return panel;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Edition");
        section.getStyleClass().add("section-label");

        memberCombo.setItems(members);
        memberCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Member member) {
                return member == null ? "" : member.fullName();
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        });
        memberCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshHistoryForMember(newValue));

        amountField.setPromptText("Montant");
        periodField.setPromptText("Periode");
        periodField.setText(contributionService.currentPeriod());
        statusCombo.getItems().setAll(ContributionStatus.values());
        statusCombo.getSelectionModel().select(ContributionStatus.PAID);
        paymentMethodField.setPromptText("Moyen de paiement");
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Membre *"), 0, 0);
        grid.add(memberCombo, 1, 0);
        grid.add(new Label("Montant *"), 2, 0);
        grid.add(amountField, 3, 0);
        grid.add(new Label("Date *"), 0, 1);
        grid.add(dateField, 1, 1);
        grid.add(new Label("Periode *"), 2, 1);
        grid.add(periodField, 3, 1);
        grid.add(new Label("Statut *"), 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(new Label("Paiement"), 2, 2);
        grid.add(paymentMethodField, 3, 2);
        grid.add(new Label("Notes"), 0, 3);
        grid.add(notesArea, 1, 3, 3, 1);

        Button createButton = new Button("Enregistrer");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createContribution());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem deleteItem = new MenuItem("Supprimer la selection");
        deleteItem.setOnAction(event -> deleteSelectedContribution());
        MenuItem refreshItem = new MenuItem("Recharger la liste");
        refreshItem.setOnAction(event -> refreshContributions());
        moreButton.getItems().addAll(deleteItem, refreshItem);

        HBox actions = new HBox(10, createButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, grid, actions);
        return panel;
    }

    private VBox createHistoryPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Historique du membre selectionne");
        section.getStyleClass().add("section-label");

        panel.getChildren().addAll(section, historyTable);
        return panel;
    }

    private TableView<Contribution> createContributionsTable() {
        TableView<Contribution> table = new TableView<>(contributions);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Contribution, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<Contribution, String> memberColumn = new TableColumn<>("Membre");
        memberColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().memberName()));
        memberColumn.setPrefWidth(180);

        TableColumn<Contribution, String> amountColumn = new TableColumn<>("Montant");
        amountColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.format(Locale.FRANCE, "%.2f EUR", cell.getValue().amount())));
        amountColumn.setPrefWidth(120);

        TableColumn<Contribution, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().contributionDate().toString()));
        dateColumn.setPrefWidth(110);

        TableColumn<Contribution, String> periodColumn = new TableColumn<>("Periode");
        periodColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().periodLabel())));
        periodColumn.setPrefWidth(90);

        TableColumn<Contribution, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().status().label()));
        statusColumn.setPrefWidth(110);

        TableColumn<Contribution, String> methodColumn = new TableColumn<>("Paiement");
        methodColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().paymentMethod())));
        methodColumn.setPrefWidth(140);

        table.getColumns().addAll(idColumn, memberColumn, amountColumn, dateColumn, periodColumn, statusColumn, methodColumn);
        table.setRowFactory(tv -> {
            TableRow<Contribution> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectMemberForHistory(row.getItem().memberId());
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectMemberForHistory(newValue.memberId());
            }
        });
        return table;
    }

    private TableView<Contribution> createHistoryTable() {
        TableView<Contribution> table = new TableView<>(memberHistory);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Contribution, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().contributionDate().toString()));
        dateColumn.setPrefWidth(100);

        TableColumn<Contribution, String> periodColumn = new TableColumn<>("Periode");
        periodColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().periodLabel())));
        periodColumn.setPrefWidth(90);

        TableColumn<Contribution, String> amountColumn = new TableColumn<>("Montant");
        amountColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.format(Locale.FRANCE, "%.2f EUR", cell.getValue().amount())));
        amountColumn.setPrefWidth(120);

        TableColumn<Contribution, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().status().label()));
        statusColumn.setPrefWidth(110);

        table.getColumns().addAll(dateColumn, periodColumn, amountColumn, statusColumn);
        return table;
    }

    private void loadMembers() {
        members.setAll(memberService.getMembers("", MemberStatusFilter.ACTIVE));
        if (!members.isEmpty()) {
            memberCombo.getSelectionModel().selectFirst();
            refreshHistoryForMember(memberCombo.getValue());
        }
    }

    private void refreshContributions() {
        contributions.setAll(contributionService.getContributions(
                searchField.getText(),
                periodFilterField.getText(),
                statusFilterCombo.getValue()
        ));
        contributionsTable.refresh();

        double total = contributions.stream().mapToDouble(Contribution::amount).sum();
        String period = periodFilterField.getText() == null || periodFilterField.getText().isBlank()
                ? contributionService.currentPeriod()
                : periodFilterField.getText().trim();

        tableSummary.setText(String.format(Locale.FRANCE,
                "Resultats : %d | Total affiche : %.2f EUR | Membres payes (%s) : %d",
                contributions.size(),
                total,
                period,
                contributionService.countPaidMembersForPeriod(period)
        ));
    }

    private void createContribution() {
        Member member = memberCombo.getValue();
        if (member == null) {
            AlertUtils.warning(getScene().getWindow(), "Cotisations", "Selectionnez un membre.");
            return;
        }

        try {
            contributionService.addContribution(
                    member.id(),
                    parseAmount(amountField.getText()),
                    parseDate(dateField.getText()),
                    periodField.getText(),
                    statusCombo.getValue(),
                    paymentMethodField.getText(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Cotisations", "Cotisation enregistree.");
            clearForm();
            refreshContributions();
            refreshHistoryForMember(memberCombo.getValue());
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Cotisations", e.getMessage());
        }
    }

    private void deleteSelectedContribution() {
        Contribution selected = contributionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Cotisations", "Selectionnez une cotisation a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Cotisations",
                "Supprimer la cotisation #" + selected.id() + " de " + selected.memberName() + " ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            contributionService.deleteContribution(selected.id());
            AlertUtils.info(getScene().getWindow(), "Cotisations", "Cotisation supprimee.");
            refreshContributions();
            refreshHistoryForMember(memberCombo.getValue());
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Cotisations", e.getMessage());
        }
    }

    private void selectMemberForHistory(long memberId) {
        for (Member member : members) {
            if (member.id() == memberId) {
                memberCombo.getSelectionModel().select(member);
                refreshHistoryForMember(member);
                return;
            }
        }
    }

    private void refreshHistoryForMember(Member member) {
        if (member == null) {
            memberHistory.clear();
            return;
        }
        memberHistory.setAll(contributionService.getMemberHistory(member.id()));
        historyTable.refresh();
    }

    private void clearForm() {
        amountField.clear();
        dateField.setText(LocalDate.now().toString());
        periodField.setText(contributionService.currentPeriod());
        statusCombo.getSelectionModel().select(ContributionStatus.PAID);
        paymentMethodField.clear();
        notesArea.clear();
    }

    private LocalDate parseDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("La date est obligatoire (format YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide. Format attendu: YYYY-MM-DD.");
        }
    }

    private double parseAmount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Le montant est obligatoire.");
        }
        String normalized = rawValue.replace(",", ".").trim();
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant invalide.");
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
