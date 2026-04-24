package com.monasso.app.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final String APP_NAME = "MonAsso";
    private static final Path WORKING_DIR = Paths.get("").toAbsolutePath().normalize();
    private static final Path APP_HOME = resolveAppHome();

    private AppPaths() {
    }

    public static Path projectRoot() {
        return WORKING_DIR;
    }

    public static Path appHome() {
        return APP_HOME;
    }

    public static Path assetsBrandingDir() {
        return APP_HOME.resolve("assets").resolve("branding");
    }

    public static Path dataDir() {
        return APP_HOME.resolve("data");
    }

    public static Path databasePath() {
        return dataDir().resolve("monasso.db");
    }

    public static Path exportsDir() {
        return APP_HOME.resolve("exports");
    }

    public static Path backupsDir() {
        return APP_HOME.resolve("backups");
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

    private static Path resolveAppHome() {
        String override = System.getProperty("monasso.home");
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim()).toAbsolutePath().normalize();
        }

        if (Files.exists(WORKING_DIR.resolve("build.gradle"))) {
            return WORKING_DIR;
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_NAME).toAbsolutePath().normalize();
        }

        return Path.of(System.getProperty("user.home"), "." + APP_NAME.toLowerCase()).toAbsolutePath().normalize();
    }
}
