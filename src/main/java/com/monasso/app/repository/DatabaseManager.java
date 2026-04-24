package com.monasso.app.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private final String jdbcUrl;

    public DatabaseManager(Path databasePath) {
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        try {
            Files.createDirectories(absolutePath.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer le dossier de base de donnees.", e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + absolutePath;
        LOGGER.info("SQLite utilise: {}", absolutePath);
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    @Override
    public void close() {
        // Les connexions sont gerees par operation. Aucun pool a fermer ici.
    }
}
