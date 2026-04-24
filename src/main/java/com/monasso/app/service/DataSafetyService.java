package com.monasso.app.service;

import com.monasso.app.repository.SchemaInitializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataSafetyService {

    private static final DateTimeFormatter FILE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SettingsService settingsService;
    private final SchemaInitializer schemaInitializer;

    public DataSafetyService(SettingsService settingsService, SchemaInitializer schemaInitializer) {
        this.settingsService = settingsService;
        this.schemaInitializer = schemaInitializer;
    }

    public Path createBackup() {
        return createBackup(settingsService.getBackupDirectory());
    }

    public Path createBackup(Path targetDirectory) {
        if (targetDirectory == null) {
            throw new IllegalArgumentException("Le dossier de sauvegarde est obligatoire.");
        }
        Path source = settingsService.getDatabasePath();
        if (!Files.exists(source)) {
            throw new IllegalStateException("Base SQLite introuvable: " + source);
        }

        try {
            Files.createDirectories(targetDirectory);
            Path destination = targetDirectory.resolve("monasso_backup_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".db");
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer la sauvegarde de la base.", e);
        }
    }

    public List<Path> listBackups() {
        return listBackups(settingsService.getBackupDirectory());
    }

    public List<Path> listBackups(Path backupDirectory) {
        if (backupDirectory == null) {
            throw new IllegalArgumentException("Le dossier de sauvegarde est obligatoire.");
        }
        try {
            Files.createDirectories(backupDirectory);
            try (Stream<Path> pathStream = Files.list(backupDirectory)) {
                return pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".db"))
                        .sorted(Comparator.comparing(this::lastModifiedMillis).reversed())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lister les sauvegardes disponibles.", e);
        }
    }

    public Path restoreBackup(Path backupFile) {
        if (backupFile == null) {
            throw new IllegalArgumentException("Le fichier de sauvegarde est obligatoire.");
        }
        Path backupPath = backupFile.toAbsolutePath().normalize();
        if (!Files.exists(backupPath) || !Files.isRegularFile(backupPath)) {
            throw new IllegalArgumentException("Sauvegarde introuvable: " + backupPath);
        }

        Path databasePath = settingsService.getDatabasePath();
        if (backupPath.equals(databasePath)) {
            throw new IllegalArgumentException("Le fichier source et la base cible sont identiques.");
        }

        try {
            Files.createDirectories(databasePath.getParent());
            Path safetyCopy = settingsService.getBackupDirectory()
                    .resolve("monasso_before_restore_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".db");

            if (Files.exists(databasePath)) {
                Files.copy(databasePath, safetyCopy, StandardCopyOption.REPLACE_EXISTING);
            }

            Path temporaryTarget = databasePath.resolveSibling("monasso_restoring_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".db");
            Files.copy(backupPath, temporaryTarget, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporaryTarget, databasePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveError) {
                Files.move(temporaryTarget, databasePath, StandardCopyOption.REPLACE_EXISTING);
            }

            schemaInitializer.initialize();
            return databasePath;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de restaurer la sauvegarde selectionnee.", e);
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
