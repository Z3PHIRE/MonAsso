package com.monasso.app.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppInitializerTest {

    @Test
    void shouldCreateLocalDatabaseAndBrandingFiles() throws Exception {
        Path tempHome = Files.createTempDirectory("monasso-appinit-test");
        String previousHome = System.getProperty("monasso.home");
        System.setProperty("monasso.home", tempHome.toString());
        AppContext context = null;
        try {
            context = new AppInitializer().initialize();
            assertTrue(Files.exists(AppPaths.databasePath()), "La base SQLite doit etre creee automatiquement.");
            assertTrue(Files.exists(AppPaths.brandingJsonPath()), "Le branding.json doit exister.");
            assertTrue(Files.exists(AppPaths.logoPath()), "Le logo par defaut doit exister.");
            assertTrue(Files.exists(AppPaths.iconPath()), "L'icone par defaut doit exister.");
        } finally {
            if (context != null) {
                context.close();
            }
            if (previousHome == null) {
                System.clearProperty("monasso.home");
            } else {
                System.setProperty("monasso.home", previousHome);
            }
        }
    }
}
