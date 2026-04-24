package com.monasso.app.service;

import com.monasso.app.config.AppPaths;
import com.monasso.app.model.AppSetting;
import com.monasso.app.repository.AppSettingsRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SettingsService {

    private static final String EXPORT_PATH_KEY = "paths.exports";
    private static final String BACKUP_PATH_KEY = "paths.backups";

    private final AppSettingsRepository appSettingsRepository;

    public SettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public List<AppSetting> getAllSettings() {
        return appSettingsRepository.findAll();
    }

    public Optional<String> getValue(String key) {
        return appSettingsRepository.findValueByKey(key);
    }

    public void saveSetting(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La cle de parametre est obligatoire.");
        }
        if (value == null) {
            throw new IllegalArgumentException("La valeur ne peut pas etre nulle.");
        }
        appSettingsRepository.save(key.trim(), value);
    }

    public Path getDatabasePath() {
        return AppPaths.databasePath().toAbsolutePath().normalize();
    }

    public Path getExportDirectory() {
        return resolveDirectory(EXPORT_PATH_KEY, AppPaths.exportsDir());
    }

    public Path getBackupDirectory() {
        return resolveDirectory(BACKUP_PATH_KEY, AppPaths.backupsDir());
    }

    public void setExportDirectory(Path directory) {
        saveDirectory(EXPORT_PATH_KEY, directory);
    }

    public void setBackupDirectory(Path directory) {
        saveDirectory(BACKUP_PATH_KEY, directory);
    }

    private Path resolveDirectory(String key, Path fallback) {
        Path configured = getValue(key)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .orElse(fallback);
        Path normalized = configured.toAbsolutePath().normalize();
        ensureDirectoryExists(normalized);
        return normalized;
    }

    private void saveDirectory(String key, Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Le dossier est obligatoire.");
        }
        Path normalized = directory.toAbsolutePath().normalize();
        ensureDirectoryExists(normalized);
        saveSetting(key, normalized.toString());
    }

    private void ensureDirectoryExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer le dossier: " + directory, e);
        }
    }
}
