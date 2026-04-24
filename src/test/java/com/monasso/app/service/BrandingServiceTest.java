package com.monasso.app.service;

import com.monasso.app.config.BrandingConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrandingServiceTest {

    @Test
    void shouldInitializeBrandingFilesAndRecoverMissingLogoReference() throws Exception {
        Path tempRoot = Files.createTempDirectory("monasso-branding-service");
        Path brandingDir = tempRoot.resolve("assets").resolve("branding");
        Path brandingFile = brandingDir.resolve("branding.json");

        BrandingService brandingService = new BrandingService(brandingDir, brandingFile);
        brandingService.initialize();

        assertTrue(Files.exists(brandingFile));
        assertTrue(Files.exists(brandingDir.resolve("logo.png")));
        assertTrue(Files.exists(brandingDir.resolve("icon.png")));

        brandingService.saveBranding(new BrandingConfig(
                "MonAsso Test",
                "#123456",
                "#ABCDEF",
                "#FF5500",
                "logo-introuvable.png"
        ));
        brandingService.reload();

        assertEquals("logo.png", brandingService.getCurrentBranding().logoPath());
    }
}
