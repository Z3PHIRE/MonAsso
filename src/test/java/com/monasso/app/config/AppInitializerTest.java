package com.monasso.app.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppInitializerTest {

    @Test
    void shouldCreateLocalDatabaseAndBrandingFiles() {
        AppContext context = new AppInitializer().initialize();
        try {
            assertTrue(Files.exists(AppPaths.databasePath()), "La base SQLite doit etre creee automatiquement.");
            assertTrue(Files.exists(AppPaths.brandingJsonPath()), "Le branding.json doit exister.");
        } finally {
            context.close();
        }
    }
}
