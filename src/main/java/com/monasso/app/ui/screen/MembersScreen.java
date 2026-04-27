package com.monasso.app.ui.screen;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomCategoryValue;
import com.monasso.app.model.CustomFieldType;
import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.PersonType;
import com.monasso.app.service.CustomCategoryService;
import com.monasso.app.service.MemberService;
import com.monasso.app.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MembersScreen extends VBox {

    private enum DisplayMode {
        COMPACT("Compact"),
        DETAILED("Detaille");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final MemberService memberService;
    private final CustomCategoryService customCategoryService;
    private final ObservableList<Member> members = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ComboBox<MemberStatusFilter> statusFilterCombo = new ComboBox<>();
    private final ComboBox<DisplayMode> displayModeCombo = new ComboBox<>();
    private final Label tableSummary = new Label();

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final ComboBox<PersonType> personTypeCombo = new ComboBox<>();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final CheckBox activeCheckBox = new CheckBox("Personne active");

    private final TextField addressField = new TextField();
    private final DatePicker joinDatePicker = new DatePicker(LocalDate.now());
    private final TextField associationRoleField = new TextField();
    private final TextField skillsField = new TextField();
    private final TextField availabilityField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final TextField emergencyContactField = new TextField();
    private final TextField clothingSizeField = new TextField();
    private final TextField certificationsField = new TextField();
    private final TextField constraintsInfoField = new TextField();
    private final TextArea linkedDocumentsArea = new TextArea();

    private final VBox customFieldsContainer = new VBox(8);
    private final Map<Long, Control> customFieldInputs = new LinkedHashMap<>();
    private List<CustomCategory> personCategories = List.of();

    private final TableView<Member> tableView = createTable();
    private TableColumn<Member, Number> idColumn;
    private TableColumn<Member, String> phoneColumn;
    private TableColumn<Member, String> emailColumn;
    private TableColumn<Member, String> roleColumn;
    private long editingMemberId = -1L;

    public MembersScreen(MemberService memberService, CustomCategoryService customCategoryService) {
        this.memberService = memberService;
        this.customCategoryService = customCategoryService;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Personnes");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Fiche personne avec champs principaux visibles et options avancees repliables.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        displayModeCombo.getItems().setAll(DisplayMode.values());
        displayModeCombo.getSelectionModel().select(DisplayMode.COMPACT);
        displayModeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyDisplayMode());

        HBox modeRow = new HBox(10, new Label("Mode"), displayModeCombo);
        modeRow.getStyleClass().add("action-row");

        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableSummary.getStyleClass().add("muted-text");

        TitledPane formPane = new TitledPane("Fiche personne", createFormPanel());
        formPane.getStyleClass().add("folded-panel");
        formPane.setExpanded(true);

        getChildren().addAll(
                title,
                subtitle,
                modeRow,
                createFilterPanel(),
                tableSummary,
                tableView,
                formPane
        );
        refreshMembers();
        applyDisplayMode();
    }

    private VBox createFilterPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Recherche et filtres");
        section.getStyleClass().add("section-label");

        searchField.setPromptText("Rechercher nom, type, role, email, telephone ou notes");
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
        personTypeCombo.getItems().setAll(PersonType.values());
        personTypeCombo.getSelectionModel().select(PersonType.MEMBER);
        phoneField.setPromptText("Telephone");
        emailField.setPromptText("Email");
        activeCheckBox.setSelected(true);

        GridPane mainGrid = new GridPane();
        mainGrid.getStyleClass().add("form-grid");
        mainGrid.add(new Label("Prenom *"), 0, 0);
        mainGrid.add(firstNameField, 1, 0);
        mainGrid.add(new Label("Nom *"), 2, 0);
        mainGrid.add(lastNameField, 3, 0);
        mainGrid.add(new Label("Type *"), 0, 1);
        mainGrid.add(personTypeCombo, 1, 1);
        mainGrid.add(new Label("Telephone"), 2, 1);
        mainGrid.add(phoneField, 3, 1);
        mainGrid.add(new Label("Email"), 0, 2);
        mainGrid.add(emailField, 1, 2);
        mainGrid.add(activeCheckBox, 2, 2);

        TitledPane advancedPane = new TitledPane("Informations avancees", createAdvancedPanel());
        advancedPane.getStyleClass().add("folded-panel");
        advancedPane.setExpanded(false);

        TitledPane customCategoriesPane = new TitledPane("Categories personnalisables", createCustomFieldsPanel());
        customCategoriesPane.getStyleClass().add("folded-panel");
        customCategoriesPane.setExpanded(false);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("light-tabs");

        Tab followUpTab = new Tab("Suivi");
        followUpTab.setClosable(false);
        VBox followUpContent = new VBox(10);
        followUpContent.getStyleClass().add("panel-card");
        Label followUpHint = new Label("Suivi de la personne: categories personnalisables et informations de contexte.");
        followUpHint.getStyleClass().add("muted-text");
        followUpHint.setWrapText(true);
        followUpContent.getChildren().addAll(followUpHint, customCategoriesPane);
        followUpTab.setContent(followUpContent);

        Tab documentsTab = new Tab("Documents");
        documentsTab.setClosable(false);
        VBox documentsContent = new VBox(10);
        documentsContent.getStyleClass().add("panel-card");
        Label documentsHint = new Label("Documents lies, references ou chemins locaux associes a la personne.");
        documentsHint.getStyleClass().add("muted-text");
        documentsHint.setWrapText(true);
        linkedDocumentsArea.setPromptText("Documents lies (references, liens, chemins)");
        linkedDocumentsArea.setPrefRowCount(4);
        documentsContent.getChildren().addAll(documentsHint, linkedDocumentsArea);
        documentsTab.setContent(documentsContent);

        tabs.getTabs().addAll(followUpTab, documentsTab);

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

        panel.getChildren().addAll(formTitle, mainGrid, advancedPane, tabs, actions);
        return panel;
    }

    private VBox createAdvancedPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        addressField.setPromptText("Adresse");
        associationRoleField.setPromptText("Role dans l'association");
        skillsField.setPromptText("Competences");
        availabilityField.setPromptText("Disponibilites");
        emergencyContactField.setPromptText("Contact d'urgence");
        clothingSizeField.setPromptText("Taille vetements");
        certificationsField.setPromptText("Certifications");
        constraintsInfoField.setPromptText("Allergies ou contraintes");

        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Adresse"), 0, 0);
        grid.add(addressField, 1, 0, 3, 1);
        grid.add(new Label("Date d'entree"), 0, 1);
        grid.add(joinDatePicker, 1, 1);
        grid.add(new Label("Role"), 2, 1);
        grid.add(associationRoleField, 3, 1);
        grid.add(new Label("Competences"), 0, 2);
        grid.add(skillsField, 1, 2, 3, 1);
        grid.add(new Label("Disponibilites"), 0, 3);
        grid.add(availabilityField, 1, 3, 3, 1);
        grid.add(new Label("Contact urgence"), 0, 4);
        grid.add(emergencyContactField, 1, 4);
        grid.add(new Label("Taille vetements"), 2, 4);
        grid.add(clothingSizeField, 3, 4);
        grid.add(new Label("Certifications"), 0, 5);
        grid.add(certificationsField, 1, 5, 3, 1);
        grid.add(new Label("Allergies/contraintes"), 0, 6);
        grid.add(constraintsInfoField, 1, 6, 3, 1);
        grid.add(new Label("Notes"), 0, 7);
        grid.add(notesArea, 1, 7, 3, 1);

        panel.getChildren().add(grid);
        return panel;
    }

    private VBox createCustomFieldsPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel-card");

        Label info = new Label("Champs additionnels actives sur les personnes (categories/sous-categories).");
        info.getStyleClass().add("muted-text");
        info.setWrapText(true);

        reloadCustomFieldInputs(Map.of());

        panel.getChildren().addAll(info, customFieldsContainer);
        return panel;
    }

    private void reloadCustomFieldInputs(Map<Long, CustomCategoryValue> existingValuesByCategory) {
        customFieldInputs.clear();
        customFieldsContainer.getChildren().clear();
        personCategories = customCategoryService.getCategoriesForScope(CategoryScope.PERSON);

        if (personCategories.isEmpty()) {
            Label empty = new Label("Aucune categorie personnalisee active pour les personnes.");
            empty.getStyleClass().add("muted-text");
            customFieldsContainer.getChildren().add(empty);
            return;
        }

        for (CustomCategory category : personCategories) {
            Control control = createInputControl(category);
            CustomCategoryValue existingValue = existingValuesByCategory.get(category.id());
            applyExistingValue(category, control, existingValue);
            customFieldInputs.put(category.id(), control);

            Label fieldLabel = new Label(labelForCategory(category));
            fieldLabel.getStyleClass().add("field-label");
            fieldLabel.setMinWidth(220);

            HBox row = new HBox(10, fieldLabel, control);
            row.getStyleClass().add("action-row");
            HBox.setHgrow(control, Priority.ALWAYS);
            customFieldsContainer.getChildren().add(row);
        }
    }

    private TableView<Member> createTable() {
        TableView<Member> table = new TableView<>(members);
        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));
        idColumn.setPrefWidth(70);

        TableColumn<Member, String> fullNameColumn = new TableColumn<>("Nom complet");
        fullNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fullName()));
        fullNameColumn.setPrefWidth(190);

        TableColumn<Member, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().personTypeLabel()));
        typeColumn.setPrefWidth(120);

        phoneColumn = new TableColumn<>("Telephone");
        phoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().phone())));
        phoneColumn.setPrefWidth(130);

        emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().email())));
        emailColumn.setPrefWidth(190);

        TableColumn<Member, String> statusColumn = new TableColumn<>("Statut");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().statusLabel()));
        statusColumn.setPrefWidth(100);

        roleColumn = new TableColumn<>("Role");
        roleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(defaultValue(cell.getValue().associationRole())));
        roleColumn.setPrefWidth(170);

        table.getColumns().add(idColumn);
        table.getColumns().add(fullNameColumn);
        table.getColumns().add(typeColumn);
        table.getColumns().add(phoneColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(statusColumn);
        table.getColumns().add(roleColumn);

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

    private Control createInputControl(CustomCategory category) {
        CustomFieldType fieldType = category.fieldType();
        return switch (fieldType) {
            case CHECKBOX -> new CheckBox("Active");
            case LIST -> {
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().setAll(parseListOptions(category.listOptions()));
                combo.setEditable(true);
                yield combo;
            }
            case DATE -> {
                DatePicker picker = new DatePicker();
                picker.setPromptText("YYYY-MM-DD");
                yield picker;
            }
            case NUMBER -> {
                TextField numberField = new TextField();
                numberField.setPromptText("Nombre");
                yield numberField;
            }
            case SHORT_TEXT -> {
                TextField textField = new TextField();
                textField.setPromptText("Texte");
                yield textField;
            }
        };
    }

    private void applyExistingValue(CustomCategory category, Control control, CustomCategoryValue value) {
        if (value == null) {
            return;
        }
        switch (category.fieldType()) {
            case CHECKBOX -> {
                if (control instanceof CheckBox checkBox && value.boolValue() != null) {
                    checkBox.setSelected(value.boolValue());
                }
            }
            case LIST -> {
                if (control instanceof ComboBox<?> comboBox && value.textValue() != null) {
                    @SuppressWarnings("unchecked")
                    ComboBox<String> stringCombo = (ComboBox<String>) comboBox;
                    stringCombo.setValue(value.textValue());
                }
            }
            case DATE -> {
                if (control instanceof DatePicker picker && value.dateValue() != null && !value.dateValue().isBlank()) {
                    picker.setValue(LocalDate.parse(value.dateValue()));
                }
            }
            case NUMBER -> {
                if (control instanceof TextField textField && value.numberValue() != null) {
                    textField.setText(String.valueOf(value.numberValue()));
                }
            }
            case SHORT_TEXT -> {
                if (control instanceof TextField textField && value.textValue() != null) {
                    textField.setText(value.textValue());
                }
            }
        }
    }

    private void refreshMembers() {
        MemberStatusFilter statusFilter = statusFilterCombo.getValue() == null ? MemberStatusFilter.ALL : statusFilterCombo.getValue();
        members.setAll(memberService.getMembers(searchField.getText(), statusFilter));
        tableView.refresh();

        long activeCount = members.stream().filter(Member::active).count();
        tableSummary.setText(String.format(Locale.FRANCE, "Resultats : %d personnes | actives : %d", members.size(), activeCount));
    }

    private void createMember() {
        try {
            Member created = memberService.addMember(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    personTypeCombo.getValue(),
                    phoneField.getText(),
                    emailField.getText(),
                    activeCheckBox.isSelected(),
                    addressField.getText(),
                    joinDatePicker.getValue(),
                    associationRoleField.getText(),
                    skillsField.getText(),
                    availabilityField.getText(),
                    notesArea.getText(),
                    emergencyContactField.getText(),
                    clothingSizeField.getText(),
                    certificationsField.getText(),
                    constraintsInfoField.getText(),
                    linkedDocumentsArea.getText()
            );
            saveCustomFieldValues(created.id());
            AlertUtils.info(getScene().getWindow(), "Personnes", "Personne creee.");
            clearForm();
            refreshMembers();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Personnes", e.getMessage());
        }
    }

    private void updateMember() {
        if (editingMemberId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Personnes", "Selectionnez une personne a modifier.");
            return;
        }
        try {
            memberService.updateMember(
                    editingMemberId,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    personTypeCombo.getValue(),
                    phoneField.getText(),
                    emailField.getText(),
                    activeCheckBox.isSelected(),
                    addressField.getText(),
                    joinDatePicker.getValue(),
                    associationRoleField.getText(),
                    skillsField.getText(),
                    availabilityField.getText(),
                    notesArea.getText(),
                    emergencyContactField.getText(),
                    clothingSizeField.getText(),
                    certificationsField.getText(),
                    constraintsInfoField.getText(),
                    linkedDocumentsArea.getText()
            );
            saveCustomFieldValues(editingMemberId);
            AlertUtils.info(getScene().getWindow(), "Personnes", "Personne modifiee.");
            clearForm();
            refreshMembers();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Personnes", e.getMessage());
        }
    }

    private void saveCustomFieldValues(long memberId) {
        if (memberId <= 0 || personCategories.isEmpty()) {
            return;
        }
        for (CustomCategory category : personCategories) {
            Control control = customFieldInputs.get(category.id());
            if (control == null) {
                continue;
            }
            String rawValue = valueFromControl(category.fieldType(), control);
            customCategoryService.saveValue(category.id(), CategoryScope.PERSON, memberId, rawValue);
        }
    }

    private String valueFromControl(CustomFieldType fieldType, Control control) {
        return switch (fieldType) {
            case CHECKBOX -> control instanceof CheckBox checkBox ? Boolean.toString(checkBox.isSelected()) : null;
            case DATE -> control instanceof DatePicker picker && picker.getValue() != null ? picker.getValue().toString() : null;
            case SHORT_TEXT, NUMBER -> control instanceof TextInputControl textInput ? textInput.getText() : null;
            case LIST -> {
                if (control instanceof ComboBox<?> comboBox) {
                    Object selected = comboBox.getValue();
                    if (selected != null) {
                        yield selected.toString();
                    }
                    yield comboBox.isEditable() ? comboBox.getEditor().getText() : null;
                }
                yield null;
            }
        };
    }

    private void deleteSelectedMember() {
        Member selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warning(getScene().getWindow(), "Personnes", "Selectionnez une personne a supprimer.");
            return;
        }
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Personnes",
                "Supprimer la personne \"" + selected.fullName() + "\" ? Cette action est definitive."
        );
        if (!confirmed) {
            return;
        }
        try {
            memberService.deleteMember(selected.id());
            AlertUtils.info(getScene().getWindow(), "Personnes", "Personne supprimee.");
            clearForm();
            refreshMembers();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Personnes", e.getMessage());
        }
    }

    private void loadMemberIntoForm(Member member) {
        if (member == null) {
            return;
        }
        editingMemberId = member.id();
        firstNameField.setText(member.firstName());
        lastNameField.setText(member.lastName());
        personTypeCombo.setValue(member.personType() == null ? PersonType.MEMBER : member.personType());
        phoneField.setText(defaultValue(member.phone()));
        emailField.setText(defaultValue(member.email()));
        activeCheckBox.setSelected(member.active());

        addressField.setText(defaultValue(member.address()));
        joinDatePicker.setValue(member.joinDate());
        associationRoleField.setText(defaultValue(member.associationRole()));
        skillsField.setText(defaultValue(member.skills()));
        availabilityField.setText(defaultValue(member.availability()));
        notesArea.setText(defaultValue(member.notes()));
        emergencyContactField.setText(defaultValue(member.emergencyContact()));
        clothingSizeField.setText(defaultValue(member.clothingSize()));
        certificationsField.setText(defaultValue(member.certifications()));
        constraintsInfoField.setText(defaultValue(member.constraintsInfo()));
        linkedDocumentsArea.setText(defaultValue(member.linkedDocuments()));

        Map<Long, CustomCategoryValue> valuesByCategory = customCategoryService.getValuesByCategoryId(CategoryScope.PERSON, member.id());
        reloadCustomFieldInputs(valuesByCategory);
    }

    private void clearForm() {
        editingMemberId = -1L;
        firstNameField.clear();
        lastNameField.clear();
        personTypeCombo.getSelectionModel().select(PersonType.MEMBER);
        phoneField.clear();
        emailField.clear();
        activeCheckBox.setSelected(true);

        addressField.clear();
        joinDatePicker.setValue(LocalDate.now());
        associationRoleField.clear();
        skillsField.clear();
        availabilityField.clear();
        notesArea.clear();
        emergencyContactField.clear();
        clothingSizeField.clear();
        certificationsField.clear();
        constraintsInfoField.clear();
        linkedDocumentsArea.clear();

        reloadCustomFieldInputs(Map.of());
        tableView.getSelectionModel().clearSelection();
    }

    private List<String> parseListOptions(String options) {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(options.split(";"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String labelForCategory(CustomCategory category) {
        if (category.parentId() == null) {
            return category.name();
        }
        return category.name() + " (sous-categorie)";
    }

    private void applyDisplayMode() {
        DisplayMode mode = displayModeCombo.getValue() == null ? DisplayMode.COMPACT : displayModeCombo.getValue();
        boolean detailed = mode == DisplayMode.DETAILED;

        if (idColumn != null) {
            idColumn.setVisible(detailed);
        }
        if (phoneColumn != null) {
            phoneColumn.setVisible(detailed);
        }
        if (emailColumn != null) {
            emailColumn.setVisible(detailed);
        }
        if (roleColumn != null) {
            roleColumn.setVisible(detailed);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
