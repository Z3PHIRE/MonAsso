package com.monasso.app.ui.screen;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.Member;
import com.monasso.app.service.ContributionService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;

public class ContributionsScreen extends VBox {

    private final ContributionService contributionService;
    private final MemberService memberService;
    private final ObservableList<Contribution> contributions = FXCollections.observableArrayList();
    private final ObservableList<Member> members = FXCollections.observableArrayList();

    private final ComboBox<Member> memberCombo = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final DatePicker contributionDatePicker = new DatePicker(LocalDate.now());
    private final TextField paymentMethodField = new TextField();
    private final TextArea notesArea = new TextArea();

    public ContributionsScreen(ContributionService contributionService, MemberService memberService) {
        this.contributionService = contributionService;
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Cotisations");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Enregistrez les cotisations liees aux membres.");
        subtitle.getStyleClass().add("screen-subtitle");

        TableView<Contribution> tableView = createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox formPanel = createFormPanel(tableView);

        getChildren().addAll(title, subtitle, formPanel, tableView);
        refreshData(tableView);
    }

    private TableView<Contribution> createTable() {
        TableView<Contribution> tableView = new TableView<>(contributions);

        TableColumn<Contribution, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(80);

        TableColumn<Contribution, String> memberColumn = new TableColumn<>("Membre");
        memberColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().memberName())));
        memberColumn.setPrefWidth(200);

        TableColumn<Contribution, String> amountColumn = new TableColumn<>("Montant");
        amountColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f EUR", cell.getValue().amount())));
        amountColumn.setPrefWidth(120);

        TableColumn<Contribution, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().contributionDate().toString()));
        dateColumn.setPrefWidth(120);

        TableColumn<Contribution, String> methodColumn = new TableColumn<>("Paiement");
        methodColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().paymentMethod())));
        methodColumn.setPrefWidth(160);

        TableColumn<Contribution, String> notesColumn = new TableColumn<>("Notes");
        notesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().notes())));
        notesColumn.setPrefWidth(260);

        tableView.getColumns().addAll(idColumn, memberColumn, amountColumn, dateColumn, methodColumn, notesColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tableView;
    }

    private VBox createFormPanel(TableView<Contribution> tableView) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label formTitle = new Label("Ajouter une cotisation");
        formTitle.getStyleClass().add("section-label");

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

        amountField.setPromptText("Montant (ex: 35.00)");
        paymentMethodField.setPromptText("Mode de paiement");
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");

        grid.add(new Label("Membre"), 0, 0);
        grid.add(memberCombo, 1, 0);
        grid.add(new Label("Montant"), 2, 0);
        grid.add(amountField, 3, 0);
        grid.add(new Label("Date"), 0, 1);
        grid.add(contributionDatePicker, 1, 1);
        grid.add(new Label("Paiement"), 2, 1);
        grid.add(paymentMethodField, 3, 1);
        grid.add(new Label("Notes"), 0, 2);
        grid.add(notesArea, 1, 2, 3, 1);

        Button addButton = new Button("Ajouter");
        addButton.getStyleClass().add("accent-button");
        addButton.setOnAction(event -> addContribution(tableView));

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshData(tableView));

        HBox actions = new HBox(10, addButton, refreshButton);
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private void addContribution(TableView<Contribution> tableView) {
        try {
            Member selectedMember = memberCombo.getValue();
            double amount = parseAmount(amountField.getText());
            contributionService.addContribution(
                    selectedMember,
                    amount,
                    contributionDatePicker.getValue(),
                    paymentMethodField.getText(),
                    notesArea.getText()
            );
            clearForm();
            refreshData(tableView);
            AlertUtils.info(getScene().getWindow(), "Cotisations", "Cotisation enregistree avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Cotisations", e.getMessage());
        }
    }

    private void refreshData(TableView<Contribution> tableView) {
        members.setAll(memberService.getAllMembers());
        contributions.setAll(contributionService.getAllContributions());
        tableView.refresh();
    }

    private void clearForm() {
        memberCombo.getSelectionModel().clearSelection();
        amountField.clear();
        contributionDatePicker.setValue(LocalDate.now());
        paymentMethodField.clear();
        notesArea.clear();
    }

    private double parseAmount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Le montant est obligatoire.");
        }
        String normalized = rawValue.replace(",", ".").trim();
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant invalide: " + rawValue);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
