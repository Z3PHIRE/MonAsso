package com.monasso.app.config;

import com.monasso.app.service.BrandingService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);
    private static final String APP_STYLESHEET = "/styles/app.css";

    private final BrandingService brandingService;
    private Scene scene;

    public ThemeManager(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    public void attach(Scene scene) {
        this.scene = scene;
        if (!scene.getStylesheets().contains(resolveStylesheet())) {
            scene.getStylesheets().add(resolveStylesheet());
        }
        applyTheme(brandingService.getCurrentBranding());
        brandingService.brandingProperty().addListener((observable, oldValue, newValue) -> applyTheme(newValue));
    }

    public void refreshNow() {
        if (scene != null) {
            applyTheme(brandingService.getCurrentBranding());
        }
    }

    public void applyPreview(String primaryColor, String secondaryColor, String accentColor) {
        if (scene == null) {
            return;
        }
        applyStyleVariables(primaryColor, secondaryColor, accentColor);
    }

    private void applyTheme(BrandingConfig brandingConfig) {
        if (scene == null) {
            return;
        }
        applyStyleVariables(brandingConfig.primaryColor(), brandingConfig.secondaryColor(), brandingConfig.accentColor());
    }

    private void applyStyleVariables(String primaryColor, String secondaryColor, String accentColor) {
        Parent root = scene.getRoot();
        root.setStyle(String.format(
                "-app-primary: %s; -app-secondary: %s; -app-accent: %s;",
                primaryColor,
                secondaryColor,
                accentColor
        ));
    }

    private String resolveStylesheet() {
        var resource = getClass().getResource(APP_STYLESHEET);
        if (resource == null) {
            LOGGER.warn("Feuille de style introuvable: {}", APP_STYLESHEET);
            return "";
        }
        return resource.toExternalForm();
    }
}
