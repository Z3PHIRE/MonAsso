package com.monasso.app.ui.screen;

import com.monasso.app.model.Member;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class MembersScreen extends VBox {

    private final MemberService memberService;
    private final ObservableList<Member> members = FXCollections.observableArrayList();

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final DatePicker joinDatePicker = new DatePicker(LocalDate.now());
    private final Label tableSummary = new Label();

    private final TableView<Member> tableView = createTable();

    public MembersScreen(MemberService memberService) {
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Membres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Ajoutez, consultez et supprimez les membres de l'association.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox formPanel = createFormPanel();

        tableSummary.getStyleClass().add("muted-text");

        getChildren().addAll(title, subtitle, formPanel, tableSummary, tableView);
        refreshMembers();
    }

    private TableView<Member> createTable() {
        TableView<Member> table = new TableView<>(members);
        table.getStyleClass().add("app-table");

        TableColumn<Member, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(80);

        TableColumn<Member, String> fullNameColumn = new TableColumn<>("Nom complet");
        fullNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fullName()));
        fullNameColumn.setPrefWidth(220);

        TableColumn<Member, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().email())));
        emailColumn.setPrefWidth(220);

        TableColumn<Member, String> phoneColumn = new TableColumn<>("Telephone");
        phoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().phone())));
        phoneColumn.setPrefWidth(150);

        TableColumn<Member, String> joinDateColumn = new TableColumn<>("Date adhesion");
        joinDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().joinDate().toString()));
        joinDateColumn.setPrefWidth(130);

        table.getColumns().addAll(idColumn, fullNameColumn, emailColumn, phoneColumn, joinDateColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label formTitle = new Label("Ajouter un membre");
        formTitle.getStyleClass().add("section-label");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");

        firstNameField.setPromptText("Prenom");
        lastNameField.setPromptText("Nom");
        emailField.setPromptText("Email");
        phoneField.setPromptText("Telephone");

        grid.add(new Label("Prenom"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Nom"), 2, 0);
        grid.add(lastNameField, 3, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Telephone"), 2, 1);
        grid.add(phoneField, 3, 1);
        grid.add(new Label("Date adhesion"), 0, 2);
        grid.add(joinDatePicker, 1, 2);

        Button addButton = new Button("Ajouter");
        addButton.getStyleClass().add("accent-button");
        addButton.setOnAction(event -> addMember());

        Button deleteButton = new Button("Supprimer selection");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteSelectedMember());

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshMembers());

        HBox actions = new HBox(10, addButton, deleteButton, refreshButton);
        actions.getStyleClass().add("action-row");
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private void addMember() {
        if (firstNameField.getText() == null || firstNameField.getText().isBlank()) {
            AlertUtils.warning(getScene().getWindow(), "Membres", "Le prenom est obligatoire.");
            return;
        }
        if (lastNameField.getText() == null || lastNameField.getText().isBlank()) {
            AlertUtils.warning(getScene().getWindow(), "Membres", "Le nom est obligatoire.");
            return;
        }
        try {
            memberService.addMember(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    joinDatePicker.getValue()
            );
            clearForm();
            refreshMembers();
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre ajoute avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Membres", e.getMessage());
        }
    }

    private void deleteSelectedMember() {
        Member selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Membres", "Selectionnez un membre a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Membres",
                "Supprimer le membre \"" + selected.fullName() + "\" ?"
        );
        if (!confirmed) {
            return;
        }
        try {
            memberService.deleteMember(selected.id());
            refreshMembers();
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre supprime.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Membres", e.getMessage());
        }
    }

    private void refreshMembers() {
        members.setAll(memberService.getAllMembers());
        tableView.refresh();
        tableSummary.setText("Total membres : " + members.size());
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        joinDatePicker.setValue(LocalDate.now());
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
