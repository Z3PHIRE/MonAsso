package com.monasso.app.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath().normalize();

    private AppPaths() {
    }

    public static Path projectRoot() {
        return PROJECT_ROOT;
    }

    public static Path assetsBrandingDir() {
        return PROJECT_ROOT.resolve("assets").resolve("branding");
    }

    public static Path dataDir() {
        return PROJECT_ROOT.resolve("data");
    }

    public static Path databasePath() {
        return dataDir().resolve("monasso.db");
    }

    public static Path exportsDir() {
        return PROJECT_ROOT.resolve("exports");
    }

    public static Path backupsDir() {
        return PROJECT_ROOT.resolve("backups");
    }

    public static Path brandingJsonPath() {
        return assetsBrandingDir().resolve("branding.json");
    }

    public static Path logoPath() {
        return assetsBrandingDir().resolve("logo.png");
    }

    public static Path iconPath() {
        return assetsBrandingDir().resolve("icon.png");
    }
}
