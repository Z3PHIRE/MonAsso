package com.monasso.app.config;

import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.repository.SchemaInitializer;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaInitializerTest {

    @Test
    void shouldCreateExpectedTables() throws Exception {
        Path tempDir = Files.createTempDirectory("monasso-schema-test");
        Path dbPath = tempDir.resolve("monasso-test.db");

        DatabaseManager databaseManager = new DatabaseManager(dbPath);
        new SchemaInitializer(databaseManager).initialize();

        Set<String> tables = new HashSet<>();
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }

        assertTrue(tables.contains("members"));
        assertTrue(tables.contains("events"));
        assertTrue(tables.contains("contributions"));
        assertTrue(tables.contains("event_participants"));
        assertTrue(tables.contains("app_settings"));
    }
}
