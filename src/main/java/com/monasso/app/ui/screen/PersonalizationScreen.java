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
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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

    private Path selectedLogoPath;

    public PersonalizationScreen(BrandingService brandingService, ThemeManager themeManager) {
        this.brandingService = brandingService;
        this.themeManager = themeManager;

        getStyleClass().add("content-root");
        setPadding(new Insets(20));
        setSpacing(16);

        Label title = new Label("Personnalisation");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Modifiez le logo, le nom affiche et les couleurs principales. Les changements sont sauvegardes dans assets/branding.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);

        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        logoPreview.setFitWidth(120);
        logoPreview.setFitHeight(120);
        logoPreview.setPreserveRatio(true);

        Button chooseLogoButton = new Button("Choisir un nouveau logo");
        chooseLogoButton.getStyleClass().add("primary-button");
        chooseLogoButton.setOnAction(event -> chooseLogo());

        Button saveButton = new Button("Enregistrer les changements");
        saveButton.getStyleClass().add("accent-button");
        saveButton.setOnAction(event -> saveBranding());

        Button reloadButton = new Button("Recharger la configuration");
        reloadButton.getStyleClass().add("ghost-button");
        reloadButton.setOnAction(event -> reloadFromConfig());

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

        Label info = new Label("Le fichier branding actif est assets/branding/branding.json.");
        info.getStyleClass().add("muted-text");

        HBox actions = new HBox(10, chooseLogoButton, saveButton, reloadButton);

        panel.getChildren().addAll(new Label("Logo courant"), logoPreview, form, actions, info);
        getChildren().addAll(title, subtitle, panel);

        reloadFromConfig();
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selectionner un logo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File selected = chooser.showOpenDialog(getScene().getWindow());
        if (selected != null) {
            selectedLogoPath = selected.toPath();
            logoPreview.setImage(new javafx.scene.image.Image(selected.toURI().toString(), 120, 120, true, true));
        }
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
            themeManager.refreshNow();
            selectedLogoPath = null;
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
        logoPreview.setImage(brandingService.loadLogoImage(120, 120));
    }

    private String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
