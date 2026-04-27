package com.monasso.app.ui.screen;

import com.monasso.app.config.BrandingConfig;
import com.monasso.app.config.ThemeManager;
import com.monasso.app.service.BrandingService;
import com.monasso.app.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class PersonalizationScreen extends VBox {

    private final BrandingService brandingService;
    private final ThemeManager themeManager;

    private final TextField appNameField = new TextField();
    private final ColorPicker primaryColorPicker = new ColorPicker();
    private final ColorPicker secondaryColorPicker = new ColorPicker();
    private final ColorPicker accentColorPicker = new ColorPicker();

    private final ImageView logoPreview = new ImageView();
    private final Label logoPathLabel = new Label();

    private final VBox previewRoot = new VBox(10);
    private final Label previewAppNameLabel = new Label();

    private Path selectedLogoPath;

    public PersonalizationScreen(BrandingService brandingService, ThemeManager themeManager) {
        this.brandingService = brandingService;
        this.themeManager = themeManager;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Personnalisation");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Modifiez le logo, le nom affiche et les couleurs principales. Le theme est recharge sans redemarrage.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        HBox contentRow = new HBox(14, createEditorPanel(), createPreviewPanel());
        HBox.setHgrow(contentRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(contentRow.getChildren().get(1), Priority.ALWAYS);

        getChildren().addAll(title, subtitle, contentRow);
        reloadFromConfig();
        registerPreviewListeners();
    }

    private VBox createEditorPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label sectionTitle = new Label("Configuration visuelle");
        sectionTitle.getStyleClass().add("section-label");

        logoPreview.setFitWidth(140);
        logoPreview.setFitHeight(140);
        logoPreview.setPreserveRatio(true);
        logoPreview.getStyleClass().add("logo-preview");

        logoPathLabel.getStyleClass().add("muted-text");
        logoPathLabel.setWrapText(true);

        Button chooseLogoButton = new Button("Choisir un logo");
        chooseLogoButton.getStyleClass().add("primary-button");
        chooseLogoButton.setOnAction(event -> chooseLogo());

        Button resetLogoSelectionButton = new Button("Annuler selection");
        resetLogoSelectionButton.getStyleClass().add("ghost-button");
        resetLogoSelectionButton.setOnAction(event -> clearLogoSelection());

        HBox logoActions = new HBox(10, chooseLogoButton, resetLogoSelectionButton);

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.add(new Label("Nom application"), 0, 0);
        form.add(appNameField, 1, 0);
        form.add(new Label("Couleur principale"), 0, 1);
        form.add(primaryColorPicker, 1, 1);
        form.add(new Label("Couleur secondaire"), 0, 2);
        form.add(secondaryColorPicker, 1, 2);
        form.add(new Label("Couleur accent"), 0, 3);
        form.add(accentColorPicker, 1, 3);

        Button previewButton = new Button("Apercu instantane");
        previewButton.getStyleClass().add("ghost-button");
        previewButton.setOnAction(event -> applyInstantPreview());

        Button saveButton = new Button("Enregistrer");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(event -> saveBranding());

        Button reloadButton = new Button("Recharger config");
        reloadButton.getStyleClass().add("primary-button");
        reloadButton.setOnAction(event -> reloadFromConfig());

        HBox actions = new HBox(10, previewButton, saveButton, reloadButton);

        panel.getChildren().addAll(sectionTitle, logoPreview, logoPathLabel, logoActions, form, actions);
        return panel;
    }

    private VBox createPreviewPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label sectionTitle = new Label("Apercu du theme");
        sectionTitle.getStyleClass().add("section-label");

        previewRoot.getStyleClass().add("theme-preview-root");
        previewRoot.setPadding(new Insets(12));

        previewAppNameLabel.getStyleClass().add("theme-preview-title");

        Label previewSubtitle = new Label("Exemple de rendu global apres application.");
        previewSubtitle.getStyleClass().add("screen-subtitle");

        VBox sampleCard = new VBox(8);
        sampleCard.getStyleClass().add("theme-preview-card");
        Label sampleTitle = new Label("Carte exemple");
        sampleTitle.getStyleClass().add("section-label");
        Label sampleText = new Label("Boutons, fond et contrastes reactifs aux couleurs choisies.");
        sampleText.getStyleClass().add("muted-text");
        HBox sampleButtons = new HBox(8);
        Button primary = new Button("Action principale");
        primary.getStyleClass().add("primary-button");
        Button accent = new Button("Action accent");
        accent.getStyleClass().add("accent-button");
        sampleButtons.getChildren().addAll(primary, accent);
        sampleCard.getChildren().addAll(sampleTitle, sampleText, sampleButtons);

        Label note = new Label("Fichier branding actif : assets/branding/branding.json");
        note.getStyleClass().add("muted-text");

        previewRoot.getChildren().addAll(previewAppNameLabel, previewSubtitle, sampleCard, note);
        panel.getChildren().add(previewRoot);
        return panel;
    }

    private void registerPreviewListeners() {
        appNameField.textProperty().addListener((obs, oldValue, newValue) -> updatePreviewPanel());
        primaryColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> updatePreviewPanel());
        secondaryColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> updatePreviewPanel());
        accentColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> updatePreviewPanel());
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selectionner un logo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File selected = chooser.showOpenDialog(getScene().getWindow());
        if (selected == null) {
            return;
        }
        selectedLogoPath = selected.toPath();
        logoPreview.setImage(new Image(selected.toURI().toString(), 140, 140, true, true));
        logoPathLabel.setText("Nouveau logo selectionne : " + selectedLogoPath);
    }

    private void clearLogoSelection() {
        selectedLogoPath = null;
        logoPathLabel.setText("Logo courant conserve.");
        logoPreview.setImage(brandingService.loadLogoImage(140, 140));
    }

    private void applyInstantPreview() {
        themeManager.applyPreview(
                toHex(primaryColorPicker.getValue()),
                toHex(secondaryColorPicker.getValue()),
                toHex(accentColorPicker.getValue())
        );
        AlertUtils.info(getScene().getWindow(), "Personnalisation", "Apercu applique. Enregistrez pour conserver les changements.");
    }

    private void saveBranding() {
        try {
            brandingService.updateBranding(
                    appNameField.getText(),
                    toHex(primaryColorPicker.getValue()),
                    toHex(secondaryColorPicker.getValue()),
                    toHex(accentColorPicker.getValue()),
                    selectedLogoPath
            );
            selectedLogoPath = null;
            themeManager.refreshNow();
            reloadFromConfig();
            AlertUtils.info(getScene().getWindow(), "Personnalisation", "Branding mis a jour avec succes.");
        } catch (Exception e) {
            AlertUtils.error(getScene().getWindow(), "Personnalisation", e.getMessage());
        }
    }

    private void reloadFromConfig() {
        BrandingConfig config = brandingService.getCurrentBranding();
        appNameField.setText(config.appName());
        primaryColorPicker.setValue(Color.web(config.primaryColor()));
        secondaryColorPicker.setValue(Color.web(config.secondaryColor()));
        accentColorPicker.setValue(Color.web(config.accentColor()));
        logoPreview.setImage(brandingService.loadLogoImage(140, 140));
        logoPathLabel.setText("Logo courant : " + config.logoPath());
        selectedLogoPath = null;
        themeManager.refreshNow();
        updatePreviewPanel();
    }

    private void updatePreviewPanel() {
        String appName = appNameField.getText() == null || appNameField.getText().isBlank() ? "MonAsso" : appNameField.getText().trim();
        previewAppNameLabel.setText(appName);

        previewRoot.setStyle(String.format(
                "-app-primary: %s; -app-secondary: %s; -app-accent: %s;",
                toHex(primaryColorPicker.getValue()),
                toHex(secondaryColorPicker.getValue()),
                toHex(accentColorPicker.getValue())
        ));
    }

    private String toHex(Color color) {
        Color effective = color == null ? Color.web("#1F4A7D") : color;
        int red = (int) Math.round(effective.getRed() * 255);
        int green = (int) Math.round(effective.getGreen() * 255);
        int blue = (int) Math.round(effective.getBlue() * 255);
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
