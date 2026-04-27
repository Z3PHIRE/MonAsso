package com.monasso.app.ui.screen;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomFieldType;
import com.monasso.app.service.CustomCategoryService;
import com.monasso.app.service.DataSafetyService;
import com.monasso.app.service.DemoDataService;
import com.monasso.app.service.SettingsService;
import com.monasso.app.util.AlertUtils;
import com.monasso.app.util.DesktopUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SettingsScreen extends VBox {

    private final SettingsService settingsService;
    private final DataSafetyService dataSafetyService;
    private final DemoDataService demoDataService;
    private final CustomCategoryService customCategoryService;
    private final Runnable onDataReload;

    private final TextField databasePathField = new TextField();
    private final TextField exportPathField = new TextField();
    private final TextField backupPathField = new TextField();
    private final TextField selectedBackupField = new TextField();
    private final Label statusLabel = new Label();
    private final ObservableList<CustomCategory> customCategories = FXCollections.observableArrayList();
    private final TableView<CustomCategory> customCategoriesTable = createCustomCategoriesTable();
    private final TextField categoryNameField = new TextField();
    private final ComboBox<CustomCategory> parentCategoryCombo = new ComboBox<>();
    private final ComboBox<CustomFieldType> fieldTypeCombo = new ComboBox<>();
    private final TextField listOptionsField = new TextField();
    private final TextField sortOrderField = new TextField("0");
    private final CheckBox categoryActiveCheck = new CheckBox("Categorie active");
    private final CheckBox scopePersonCheck = new CheckBox("Personne");
    private final CheckBox scopeEventCheck = new CheckBox("Evenement");
    private final CheckBox scopeMeetingCheck = new CheckBox("Reunion");
    private final CheckBox scopeTaskCheck = new CheckBox("Tache");
    private long editingCategoryId = -1L;

    public SettingsScreen(
            SettingsService settingsService,
            DataSafetyService dataSafetyService,
            DemoDataService demoDataService,
            CustomCategoryService customCategoryService,
            Runnable onDataReload
    ) {
        this.settingsService = settingsService;
        this.dataSafetyService = dataSafetyService;
        this.demoDataService = demoDataService;
        this.customCategoryService = customCategoryService;
        this.onDataReload = onDataReload;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Parametres");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Chemins applicatifs, sauvegarde et restauration de la base SQLite.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        Accordion accordion = new Accordion();
        TitledPane pathsPane = new TitledPane("Chemins", createPathsPanel());
        pathsPane.getStyleClass().add("folded-panel");
        TitledPane dataSafetyPane = new TitledPane("Securite des donnees", createDataSafetyPanel());
        dataSafetyPane.getStyleClass().add("folded-panel");
        TitledPane customCategoriesPane = new TitledPane("Categories personnalisables", createCustomCategoriesPanel());
        customCategoriesPane.getStyleClass().add("folded-panel");
        TitledPane actionsPane = new TitledPane("Actions", createActionsPanel());
        actionsPane.getStyleClass().add("folded-panel");
        accordion.getPanes().addAll(pathsPane, dataSafetyPane, customCategoriesPane, actionsPane);
        accordion.setExpandedPane(pathsPane);

        getChildren().addAll(
                title,
                subtitle,
                accordion
        );
        reloadFromSettings();
        reloadCustomCategories();
    }

    private VBox createPathsPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label panelTitle = new Label("Chemins applicatifs");
        panelTitle.getStyleClass().add("section-label");

        databasePathField.setEditable(false);
        databasePathField.setFocusTraversable(false);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(12);

        addPathRow(grid, 0, "Base SQLite", databasePathField, false);
        addPathRow(grid, 1, "Exports", exportPathField, true);
        addPathRow(grid, 2, "Sauvegardes", backupPathField, true);

        statusLabel.getStyleClass().add("muted-text");
        statusLabel.setWrapText(true);

        panel.getChildren().addAll(panelTitle, grid, statusLabel);
        return panel;
    }

    private VBox createDataSafetyPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        selectedBackupField.setEditable(false);
        selectedBackupField.setPromptText("Selectionnez un fichier de sauvegarde .db");

        Button createBackupButton = new Button("Creer sauvegarde maintenant");
        createBackupButton.getStyleClass().add("accent-button");
        createBackupButton.setOnAction(event -> createBackupNow());

        Button chooseBackupButton = new Button("Choisir sauvegarde");
        chooseBackupButton.getStyleClass().add("ghost-button");
        chooseBackupButton.setOnAction(event -> chooseBackupFile());

        Button restoreButton = new Button("Restaurer sauvegarde");
        restoreButton.getStyleClass().add("danger-button");
        restoreButton.setOnAction(event -> restoreSelectedBackup());

        MenuButton moreButton = new MenuButton("Plus d'options");
        moreButton.getStyleClass().add("ghost-button");
        MenuItem openFolderItem = new MenuItem("Ouvrir dossier backups");
        openFolderItem.setOnAction(event -> openBackupDirectory());
        MenuItem demoDataItem = new MenuItem("Charger donnees de demonstration");
        demoDataItem.setOnAction(event -> loadDemoData());
        moreButton.getItems().addAll(openFolderItem, demoDataItem);

        HBox row = new HBox(10, createBackupButton, chooseBackupButton, restoreButton, moreButton);
        row.getStyleClass().add("action-row");

        panel.getChildren().addAll(selectedBackupField, row);
        return panel;
    }

    private VBox createActionsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Button saveButton = new Button("Enregistrer les chemins");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(event -> savePathSettings());

        Button reloadButton = new Button("Recharger");
        reloadButton.getStyleClass().add("ghost-button");
        reloadButton.setOnAction(event -> reloadFromSettings());

        HBox row = new HBox(10, saveButton, reloadButton);
        row.getStyleClass().add("action-row");

        panel.getChildren().add(row);
        return panel;
    }

    private VBox createCustomCategoriesPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");

        Label section = new Label("Creation de categories, sous-categories et activation par cible");
        section.getStyleClass().add("section-label");
        Label helper = new Label("Types supportes: case a cocher, texte court, nombre, date, liste.");
        helper.getStyleClass().add("muted-text");

        VBox.setVgrow(customCategoriesTable, Priority.ALWAYS);

        fieldTypeCombo.getItems().setAll(CustomFieldType.values());
        fieldTypeCombo.getSelectionModel().select(CustomFieldType.SHORT_TEXT);
        fieldTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            listOptionsField.setDisable(newValue != CustomFieldType.LIST);
            if (newValue != CustomFieldType.LIST) {
                listOptionsField.clear();
            }
        });
        listOptionsField.setPromptText("Options liste separees par ';'");
        listOptionsField.setDisable(true);

        categoryNameField.setPromptText("Nom categorie");
        parentCategoryCombo.setPromptText("Categorie parente (optionnel)");
        parentCategoryCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(CustomCategory category) {
                return category == null ? "" : category.name();
            }

            @Override
            public CustomCategory fromString(String string) {
                return null;
            }
        });

        sortOrderField.setPromptText("Ordre");
        categoryActiveCheck.setSelected(true);
        scopePersonCheck.setSelected(true);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.add(new Label("Nom *"), 0, 0);
        grid.add(categoryNameField, 1, 0);
        grid.add(new Label("Parent"), 2, 0);
        grid.add(parentCategoryCombo, 3, 0);
        grid.add(new Label("Type *"), 0, 1);
        grid.add(fieldTypeCombo, 1, 1);
        grid.add(new Label("Options liste"), 2, 1);
        grid.add(listOptionsField, 3, 1);
        grid.add(new Label("Ordre"), 0, 2);
        grid.add(sortOrderField, 1, 2);
        grid.add(categoryActiveCheck, 2, 2);

        HBox scopesRow = new HBox(10, new Label("Activation"), scopePersonCheck, scopeEventCheck, scopeMeetingCheck, scopeTaskCheck);
        scopesRow.getStyleClass().add("action-row");

        Button createButton = new Button("Creer categorie");
        createButton.getStyleClass().add("accent-button");
        createButton.setOnAction(event -> createCategory());

        Button updateButton = new Button("Mettre a jour");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> updateCategory());

        Button resetButton = new Button("Nouveau formulaire");
        resetButton.getStyleClass().add("ghost-button");
        resetButton.setOnAction(event -> clearCategoryForm());

        Button reloadButton = new Button("Recharger");
        reloadButton.getStyleClass().add("ghost-button");
        reloadButton.setOnAction(event -> reloadCustomCategories());

        HBox actions = new HBox(10, createButton, updateButton, resetButton, reloadButton);
        actions.getStyleClass().add("action-row");

        panel.getChildren().addAll(section, helper, customCategoriesTable, grid, scopesRow, actions);
        return panel;
    }

    private TableView<CustomCategory> createCustomCategoriesTable() {
        TableView<CustomCategory> table = new TableView<>(customCategories);
        table.getStyleClass().add("app-table");
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CustomCategory, String> nameColumn = new TableColumn<>("Categorie");
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().name()));
        nameColumn.setPrefWidth(180);

        TableColumn<CustomCategory, String> parentColumn = new TableColumn<>("Parent");
        parentColumn.setCellValueFactory(cell -> {
            CustomCategory category = cell.getValue();
            String parentLabel = category.parentId() == null
                    ? "-"
                    : customCategories.stream()
                    .filter(candidate -> candidate.id() == category.parentId())
                    .map(CustomCategory::name)
                    .findFirst()
                    .orElse("#" + category.parentId());
            return new javafx.beans.property.SimpleStringProperty(parentLabel);
        });
        parentColumn.setPrefWidth(150);

        TableColumn<CustomCategory, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fieldType().label()));
        typeColumn.setPrefWidth(140);

        TableColumn<CustomCategory, String> scopesColumn = new TableColumn<>("Cibles");
        scopesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().scopes().stream().map(CategoryScope::label).collect(Collectors.joining(", "))
        ));
        scopesColumn.setPrefWidth(220);

        TableColumn<CustomCategory, String> activeColumn = new TableColumn<>("Actif");
        activeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().active() ? "Oui" : "Non"));
        activeColumn.setPrefWidth(90);

        table.getColumns().addAll(nameColumn, parentColumn, typeColumn, scopesColumn, activeColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                loadCategoryIntoForm(newValue);
            }
        });
        return table;
    }

    private void addPathRow(GridPane grid, int rowIndex, String label, TextField field, boolean editable) {
        Label rowLabel = new Label(label);
        rowLabel.getStyleClass().add("field-label");

        field.setEditable(editable);
        HBox.setHgrow(field, Priority.ALWAYS);

        Button chooseButton = new Button("Choisir");
        chooseButton.getStyleClass().add("ghost-button");
        chooseButton.setDisable(!editable);
        chooseButton.setOnAction(event -> chooseFolderForField(field));

        Button openButton = new Button("Ouvrir");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(event -> openFolder(field));

        HBox buttonBox = new HBox(8, chooseButton, openButton);
        buttonBox.getStyleClass().add("action-row");

        grid.add(rowLabel, 0, rowIndex);
        grid.add(field, 1, rowIndex);
        grid.add(buttonBox, 2, rowIndex);
    }

    private void chooseFolderForField(TextField field) {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir un dossier");
            Path current = pathFromField(field);
            if (current != null && current.toFile().exists()) {
                chooser.setInitialDirectory(current.toFile());
            }
            var selected = chooser.showDialog(getScene().getWindow());
            if (selected != null) {
                field.setText(selected.toPath().toAbsolutePath().normalize().toString());
            }
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void openFolder(TextField field) {
        try {
            Path path = pathFromField(field);
            if (path == null) {
                AlertUtils.warning(getScene().getWindow(), "Parametres", "Aucun dossier defini.");
                return;
            }
            Path targetDirectory = path.toFile().isDirectory() ? path : path.getParent();
            if (targetDirectory == null) {
                AlertUtils.warning(getScene().getWindow(), "Parametres", "Impossible de determiner le dossier a ouvrir.");
                return;
            }
            DesktopUtils.openDirectory(targetDirectory);
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void createBackupNow() {
        try {
            Path backupFile = dataSafetyService.createBackup();
            selectedBackupField.setText(backupFile.toString());
            statusLabel.setText("Sauvegarde creee: " + backupFile);
            AlertUtils.info(getScene().getWindow(), "Sauvegarde", "Sauvegarde terminee.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Sauvegarde", e.getMessage());
        }
    }

    private void openBackupDirectory() {
        try {
            DesktopUtils.openDirectory(settingsService.getBackupDirectory());
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Sauvegarde", e.getMessage());
        }
    }

    private void chooseBackupFile() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir un fichier de sauvegarde");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base SQLite (*.db)", "*.db"));

            Path backupDirectory = settingsService.getBackupDirectory();
            if (backupDirectory != null && backupDirectory.toFile().exists()) {
                chooser.setInitialDirectory(backupDirectory.toFile());
            }

            var selected = chooser.showOpenDialog(getScene().getWindow());
            if (selected != null) {
                selectedBackupField.setText(selected.toPath().toAbsolutePath().normalize().toString());
            }
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Restauration", e.getMessage());
        }
    }

    private void restoreSelectedBackup() {
        Path selectedBackup = pathFromField(selectedBackupField);
        if (selectedBackup == null) {
            AlertUtils.warning(getScene().getWindow(), "Restauration", "Selectionnez une sauvegarde avant de restaurer.");
            return;
        }

        boolean basicConfirm = AlertUtils.confirm(
                getScene().getWindow(),
                "Restauration",
                "Restaurer la base depuis:\n" + selectedBackup + "\n\nLes donnees actuelles seront remplacees."
        );
        if (!basicConfirm) {
            return;
        }

        if (!confirmStrongRestore()) {
            return;
        }

        try {
            Path restoredDb = dataSafetyService.restoreBackup(selectedBackup);
            statusLabel.setText("Restauration terminee: " + restoredDb);
            onDataReload.run();
            AlertUtils.info(getScene().getWindow(), "Restauration", "Restauration terminee. Les ecrans ont ete recharges.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Restauration", e.getMessage());
        }
    }

    private boolean confirmStrongRestore() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Confirmation forte");
        dialog.setHeaderText("Tapez RESTAURER pour confirmer");
        dialog.setContentText("Confirmation:");
        Optional<String> value = dialog.showAndWait();
        return value.filter(text -> "RESTAURER".equals(text.trim())).isPresent();
    }

    private void loadDemoData() {
        boolean confirmed = AlertUtils.confirm(
                getScene().getWindow(),
                "Donnees de demonstration",
                "Charger un jeu de demonstration ?\nAction reservee a une base vide."
        );
        if (!confirmed) {
            return;
        }

        try {
            DemoDataService.DemoDataResult result = demoDataService.loadDemoData();
            statusLabel.setText("Donnees demo chargees: "
                    + result.members() + " membres, "
                    + result.events() + " evenements, "
                    + result.meetings() + " reunions, "
                    + result.contributions() + " cotisations.");
            onDataReload.run();
            AlertUtils.info(getScene().getWindow(), "Donnees de demonstration", "Jeu de demonstration charge.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Donnees de demonstration", e.getMessage());
        }
    }

    private void savePathSettings() {
        try {
            settingsService.setExportDirectory(pathFromField(exportPathField));
            settingsService.setBackupDirectory(pathFromField(backupPathField));
            reloadFromSettings();
            statusLabel.setText("Chemins enregistres avec succes.");
            AlertUtils.info(getScene().getWindow(), "Parametres", "Configuration enregistree.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void reloadFromSettings() {
        databasePathField.setText(settingsService.getDatabasePath().toString());
        exportPathField.setText(settingsService.getExportDirectory().toString());
        backupPathField.setText(settingsService.getBackupDirectory().toString());
        statusLabel.setText("Configuration chargee.");
    }

    private void reloadCustomCategories() {
        customCategories.setAll(customCategoryService.getAllCategories());
        List<CustomCategory> roots = customCategories.stream()
                .filter(CustomCategory::isRootCategory)
                .toList();
        parentCategoryCombo.getItems().setAll(roots);
        customCategoriesTable.refresh();
    }

    private void createCategory() {
        try {
            customCategoryService.createCategory(
                    categoryNameField.getText(),
                    parentCategoryCombo.getValue() == null ? null : parentCategoryCombo.getValue().id(),
                    fieldTypeCombo.getValue(),
                    listOptionsField.getText(),
                    categoryActiveCheck.isSelected(),
                    parseSortOrder(sortOrderField.getText()),
                    selectedScopes()
            );
            AlertUtils.info(getScene().getWindow(), "Parametres", "Categorie personnalisee creee.");
            clearCategoryForm();
            reloadCustomCategories();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void updateCategory() {
        if (editingCategoryId <= 0) {
            AlertUtils.warning(getScene().getWindow(), "Parametres", "Selectionnez une categorie a modifier.");
            return;
        }
        try {
            customCategoryService.updateCategory(
                    editingCategoryId,
                    categoryNameField.getText(),
                    parentCategoryCombo.getValue() == null ? null : parentCategoryCombo.getValue().id(),
                    fieldTypeCombo.getValue(),
                    listOptionsField.getText(),
                    categoryActiveCheck.isSelected(),
                    parseSortOrder(sortOrderField.getText()),
                    selectedScopes()
            );
            AlertUtils.info(getScene().getWindow(), "Parametres", "Categorie personnalisee mise a jour.");
            clearCategoryForm();
            reloadCustomCategories();
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Parametres", e.getMessage());
        }
    }

    private void loadCategoryIntoForm(CustomCategory category) {
        editingCategoryId = category.id();
        categoryNameField.setText(category.name());
        fieldTypeCombo.setValue(category.fieldType());
        listOptionsField.setText(category.listOptions() == null ? "" : category.listOptions());
        listOptionsField.setDisable(category.fieldType() != CustomFieldType.LIST);
        categoryActiveCheck.setSelected(category.active());
        sortOrderField.setText(String.valueOf(category.sortOrder()));

        CustomCategory parent = category.parentId() == null
                ? null
                : customCategories.stream()
                .filter(candidate -> candidate.id() == category.parentId())
                .findFirst()
                .orElse(null);
        parentCategoryCombo.setValue(parent);

        scopePersonCheck.setSelected(category.scopes().contains(CategoryScope.PERSON));
        scopeEventCheck.setSelected(category.scopes().contains(CategoryScope.EVENT));
        scopeMeetingCheck.setSelected(category.scopes().contains(CategoryScope.MEETING));
        scopeTaskCheck.setSelected(category.scopes().contains(CategoryScope.TASK));
    }

    private void clearCategoryForm() {
        editingCategoryId = -1L;
        categoryNameField.clear();
        parentCategoryCombo.getSelectionModel().clearSelection();
        fieldTypeCombo.getSelectionModel().select(CustomFieldType.SHORT_TEXT);
        listOptionsField.clear();
        listOptionsField.setDisable(true);
        sortOrderField.setText("0");
        categoryActiveCheck.setSelected(true);
        scopePersonCheck.setSelected(true);
        scopeEventCheck.setSelected(false);
        scopeMeetingCheck.setSelected(false);
        scopeTaskCheck.setSelected(false);
        customCategoriesTable.getSelectionModel().clearSelection();
    }

    private List<CategoryScope> selectedScopes() {
        List<CategoryScope> scopes = new ArrayList<>();
        if (scopePersonCheck.isSelected()) {
            scopes.add(CategoryScope.PERSON);
        }
        if (scopeEventCheck.isSelected()) {
            scopes.add(CategoryScope.EVENT);
        }
        if (scopeMeetingCheck.isSelected()) {
            scopes.add(CategoryScope.MEETING);
        }
        if (scopeTaskCheck.isSelected()) {
            scopes.add(CategoryScope.TASK);
        }
        return scopes;
    }

    private int parseSortOrder(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ordre de categorie invalide.");
        }
    }

    private Path pathFromField(TextField field) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim());
    }
}
