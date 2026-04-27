package com.monasso.app.ui.screen;

import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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

import java.time.LocalDate;
import java.util.Locale;

public class MembersScreen extends VBox {

    private final MemberService memberService;
    private final ObservableList<Member> members = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ComboBox<MemberStatusFilter> statusFilterCombo = new ComboBox<>();
    private final Label tableSummary = new Label();

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField addressField = new TextField();
    private final DatePicker joinDatePicker = new DatePicker(LocalDate.now());
    private final CheckBox activeCheckBox = new CheckBox("Membre actif");
    private final TextArea notesArea = new TextArea();

    private final TableView<Member> tableView = createTable();

    private long editingMemberId = -1L;

    public MembersScreen(MemberService memberService) {
        this.memberService = memberService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Membres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Gerez les membres: creation, modification, suppression, recherche et filtres.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");
        TitledPane formPane = new TitledPane("Fiche personne", createFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(false);

        getChildren().addAll(
                title,
                subtitle,
                createFilterPanel(),
                tableSummary,
                tableView,
                formPane
        );
        refreshMembers();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche et filtres");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher nom, email, telephone ou adresse");
        searchField.setOnAction(event -> refreshMembers());

        statusFilterCombo.getItems().setAll(MemberStatusFilter.values());
        statusFilterCombo.getSelectionModel().select(MemberStatusFilter.ALL);
        statusFilterCombo.setOnAction(event -> refreshMembers());

        Button applyButton = new Button("Appliquer");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> refreshMembers());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Reinitialiser filtres");
        clearItem.setOnAction(event -> {
            searchField.clear();
            statusFilterCombo.getSelectionModel().select(MemberStatusFilter.ALL);
            refreshMembers();
        });
        moreButton.getItems().add(clearItem);

        HBox row = new HBox(10,
                new Label("Recherche"), searchField,
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

        Label formTitle = new Label("Edition");
        formTitle.getStyleClass().add("section-label");

        firstNameField.setPromptText("Prenom");
        lastNameField.setPromptText("Nom");
        emailField.setPromptText("Email");
        phoneField.setPromptText("Telephone");
        addressField.setPromptText("Adresse");
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);
        activeCheckBox.setSelected(true);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Prenom *"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Nom *"), 2, 0);
        grid.add(lastNameField, 3, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Telephone"), 2, 1);
        grid.add(phoneField, 3, 1);
        grid.add(new Label("Adresse"), 0, 2);
        grid.add(addressField, 1, 2, 3, 1);
        grid.add(new Label("Date adhesion *"), 0, 3);
        grid.add(joinDatePicker, 1, 3);
        grid.add(activeCheckBox, 2, 3);
        grid.add(new Label("Notes"), 0, 4);
        grid.add(notesArea, 1, 4, 3, 1);

        Button createButton = new Button("Creer");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createMember());

        Button updateButton = new Button("Modifier");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateMember());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem clearItem = new MenuItem("Nouveau formulaire");
        clearItem.setOnAction(event -> clearForm());
        MenuItem deleteItem = new MenuItem("Supprimer la selection");
        deleteItem.setOnAction(event -> deleteSelectedMember());
        moreButton.getItems().addAll(clearItem, deleteItem);

        HBox actions = new HBox(10, createButton, updateButton, moreButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(formTitle, grid, actions);
        return panel;
    }

    private TableView<Member> createTable() {
        TableView<Member> table = new TableView<>(members);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Member, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(70);

        TableColumn<Member, String> fullNameColumn = new TableColumn<>("Nom complet");
        fullNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fullName()));
        fullNameColumn.setPrefWidth(180);

        TableColumn<Member, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().email())));
        emailColumn.setPrefWidth(190);

        TableColumn<Member, String> phoneColumn = new TableColumn<>("Telephone");
        phoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().phone())));
        phoneColumn.setPrefWidth(140);

        TableColumn<Member, String> addressColumn = new TableColumn<>("Adresse");
        addressColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().address())));
        addressColumn.setPrefWidth(220);

        TableColumn<Member, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(100);

        TableColumn<Member, String> joinDateColumn = new TableColumn<>("Adhesion");
        joinDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().joinDate().toString()));
        joinDateColumn.setPrefWidth(110);

        table.getColumns().addAll(idColumn, fullNameColumn, emailColumn, phoneColumn, addressColumn, statusColumn, joinDateColumn);

        table.setRowFactory(tv -> {
            TableRow<Member> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadMemberIntoForm(row.getItem());
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadMemberIntoForm(newValue);
            }
        });
        return table;
    }

    private void refreshMembers() {
        MemberStatusFilter statusFilter = statusFilterCombo.getValue() == null ? MemberStatusFilter.ALL : statusFilterCombo.getValue();
        members.setAll(memberService.getMembers(searchField.getText(), statusFilter));
        tableView.refresh();

        long activeCount = members.stream().filter(Member::active).count();
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d membres | actifs : %d", members.size(), activeCount));
    }

    private void createMember() {
        try {
            memberService.addMember(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    addressField.getText(),
                    joinDatePicker.getValue(),
                    activeCheckBox.isSelected(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre cree.");
            clearForm();
            refreshMembers();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Membres", e.getMessage());
        }
    }

    private void updateMember() {
        if (editingMemberId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Membres", "Selectionnez un membre a modifier.");
            return;
        }
        try {
            memberService.updateMember(
                    editingMemberId,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    addressField.getText(),
                    joinDatePicker.getValue(),
                    activeCheckBox.isSelected(),
                    notesArea.getText()
            );
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre modifie.");
            clearForm();
            refreshMembers();
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
                "Supprimer le membre \"" + selected.fullName() + "\" ? Cette action est definitive."
        );
        if (!confirmed) {
            return;
        }
        try {
            memberService.deleteMember(selected.id());
            AlertUtils.info(getScene().getWindow(), "Membres", "Membre supprime.");
            clearForm();
            refreshMembers();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Membres", e.getMessage());
        }
    }

    private void loadMemberIntoForm(Member member) {
        if (member == null) {
            return;
        }
        editingMemberId = member.id();
        firstNameField.setText(member.firstName());
        lastNameField.setText(member.lastName());
        emailField.setText(defaultValue(member.email()));
        phoneField.setText(defaultValue(member.phone()));
        addressField.setText(defaultValue(member.address()));
        joinDatePicker.setValue(member.joinDate());
        activeCheckBox.setSelected(member.active());
        notesArea.setText(defaultValue(member.notes()));
    }

    private void clearForm() {
        editingMemberId = -1L;
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();
        joinDatePicker.setValue(LocalDate.now());
        activeCheckBox.setSelected(true);
        notesArea.clear();
        tableView.getSelectionModel().clearSelection();
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
