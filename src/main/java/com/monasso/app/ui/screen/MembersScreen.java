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

    public MembersScreen(MemberService memberService) {
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Membres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Ajout et consultation des membres de l'association.");
        subtitle.getStyleClass().add("screen-subtitle");

        TableView<Member> tableView = createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox formPanel = createFormPanel(tableView);

        getChildren().addAll(title, subtitle, formPanel, tableView);
        refreshMembers(tableView);
    }

    private TableView<Member> createTable() {
        TableView<Member> tableView = new TableView<>(members);

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

        tableView.getColumns().addAll(idColumn, fullNameColumn, emailColumn, phoneColumn, joinDateColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tableView;
    }

    private VBox createFormPanel(TableView<Member> tableView) {
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
        addButton.setOnAction(event -> addMember(tableView));

        Button refreshButton = new Button("Recharger");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(event -> refreshMembers(tableView));

        HBox actions = new HBox(10, addButton, refreshButton);
        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private void addMember(TableView<Member> tableView) {
        try {
            memberService.addMember(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    joinDatePicker.getValue()
            );
            clearForm();
            refreshMembers(tableView);
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre ajoute avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Membres", e.getMessage());
        }
    }

    private void refreshMembers(TableView<Member> tableView) {
        members.setAll(memberService.getAllMembers());
        tableView.refresh();
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
