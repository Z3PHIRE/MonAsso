package com.monasso.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.monasso.app.config.AppPaths;
import com.monasso.app.config.BrandingConfig;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class BrandingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandingService.class);
    private static final String LOGO_TARGET_NAME = "logo";
    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg"};

    private final Path brandingDirectory;
    private final Path brandingFile;
    private final ObjectMapper objectMapper;
    private final ObjectProperty<BrandingConfig> brandingProperty;

    public BrandingService() {
        this(AppPaths.assetsBrandingDir(), AppPaths.brandingJsonPath());
    }

    public BrandingService(Path brandingDirectory, Path brandingFile) {
        this.brandingDirectory = brandingDirectory;
        this.brandingFile = brandingFile;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.brandingProperty = new SimpleObjectProperty<>(BrandingConfig.defaults());
    }

    public void initialize() {
        try {
            Files.createDirectories(brandingDirectory);
            ensureBrandingConfigExists();
            ensureBrandingAssetExists("logo.png");
            ensureBrandingAssetExists("icon.png");
            reload();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'initialiser la configuration de branding.", e);
        }
    }

    public void reload() {
        try {
            BrandingConfig loaded = objectMapper.readValue(brandingFile.toFile(), BrandingConfig.class).sanitized();
            brandingProperty.set(loaded);
            if (!Files.exists(resolveLogoPath(loaded))) {
                LOGGER.warn("Logo introuvable: {}. Repli sur le logo par defaut.", resolveLogoPath(loaded));
                saveBranding(new BrandingConfig(
                        loaded.appName(),
                        loaded.primaryColor(),
                        loaded.secondaryColor(),
                        loaded.accentColor(),
                        "logo.png"
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger branding.json", e);
        }
    }

    public BrandingConfig getCurrentBranding() {
        return brandingProperty.get();
    }

    public ReadOnlyObjectProperty<BrandingConfig> brandingProperty() {
        return brandingProperty;
    }

    public void saveBranding(BrandingConfig brandingConfig) {
        BrandingConfig sanitized = brandingConfig.sanitized();
        try {
            Files.createDirectories(brandingDirectory);
            objectMapper.writeValue(brandingFile.toFile(), sanitized);
            brandingProperty.set(sanitized);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer branding.json", e);
        }
    }

    public void updateBranding(String appName, String primaryColor, String secondaryColor, String accentColor, Path selectedLogoPath) {
        BrandingConfig current = getCurrentBranding();
        String logoPath = current.logoPath();
        if (selectedLogoPath != null) {
            logoPath = copyLogoToBrandingDirectory(selectedLogoPath);
        }
        saveBranding(new BrandingConfig(appName, primaryColor, secondaryColor, accentColor, logoPath));
    }

    public Image loadLogoImage(double requestedWidth, double requestedHeight) {
        Path logo = getResolvedLogoPath();
        if (!Files.exists(logo)) {
            logo = AppPaths.logoPath();
        }
        return new Image(logo.toUri().toString(), requestedWidth, requestedHeight, true, true);
    }

    public Path getResolvedLogoPath() {
        return resolveLogoPath(getCurrentBranding());
    }

    public Image loadAppIcon() {
        Path icon = AppPaths.iconPath();
        return new Image(icon.toUri().toString());
    }

    private void ensureBrandingConfigExists() throws IOException {
        if (!Files.exists(brandingFile)) {
            objectMapper.writeValue(brandingFile.toFile(), BrandingConfig.defaults());
        }
    }

    private void ensureBrandingAssetExists(String assetFileName) throws IOException {
        Path target = brandingDirectory.resolve(assetFileName);
        if (Files.exists(target)) {
            return;
        }
        try (InputStream input = BrandingService.class.getResourceAsStream("/default-branding/" + assetFileName)) {
            if (input == null) {
                LOGGER.warn("Ressource par defaut introuvable: {}", assetFileName);
                return;
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String copyLogoToBrandingDirectory(Path selectedLogoPath) {
        String extension = extractExtension(selectedLogoPath);
        Path target = brandingDirectory.resolve(LOGO_TARGET_NAME + extension);
        try {
            Files.copy(selectedLogoPath, target, StandardCopyOption.REPLACE_EXISTING);
            return target.getFileName().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de copier le logo dans assets/branding.", e);
        }
    }

    private String extractExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (filename.endsWith(extension)) {
                return extension;
            }
        }
        throw new IllegalArgumentException("Format de logo non supporte. Utiliser PNG ou JPG.");
    }

    private Path resolveLogoPath(BrandingConfig brandingConfig) {
        Path configuredPath = Path.of(brandingConfig.logoPath());
        if (configuredPath.isAbsolute()) {
            return configuredPath;
        }
        return brandingDirectory.resolve(configuredPath).normalize();
    }
}
