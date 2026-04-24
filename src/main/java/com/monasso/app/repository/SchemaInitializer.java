package com.monasso.app.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SchemaInitializer {

    private final DatabaseManager databaseManager;

    public SchemaInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        List<String> createTableStatements = List.of(
                """
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    address TEXT,
                    join_date TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT,
                    event_date TEXT NOT NULL,
                    event_time TEXT,
                    location TEXT,
                    description TEXT,
                    capacity INTEGER,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS contributions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    contribution_date TEXT NOT NULL,
                    period_label TEXT,
                    status TEXT,
                    payment_method TEXT,
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS event_participants (
                    event_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    registration_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(event_id, member_id),
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );

        List<String> createIndexStatements = List.of(
                "CREATE INDEX IF NOT EXISTS idx_members_name ON members(last_name, first_name)",
                "CREATE INDEX IF NOT EXISTS idx_members_active ON members(is_active)",
                "CREATE INDEX IF NOT EXISTS idx_events_date ON events(event_date)",
                "CREATE INDEX IF NOT EXISTS idx_contributions_member_period ON contributions(member_id, period_label)",
                "CREATE INDEX IF NOT EXISTS idx_contributions_status ON contributions(status)"
        );

        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);

            for (String sql : createTableStatements) {
                statement.execute(sql);
            }
            migrateLegacySchema(connection);
            normalizeData(connection);
            for (String sql : createIndexStatements) {
                statement.execute(sql);
            }

            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser le schema SQLite.", e);
        }
    }

    private void migrateLegacySchema(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "members", "address", "TEXT");
        addColumnIfMissing(connection, "members", "is_active", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(connection, "members", "notes", "TEXT");

        addColumnIfMissing(connection, "events", "title", "TEXT");
        addColumnIfMissing(connection, "events", "event_time", "TEXT");
        addColumnIfMissing(connection, "events", "capacity", "INTEGER");

        addColumnIfMissing(connection, "contributions", "period_label", "TEXT");
        addColumnIfMissing(connection, "contributions", "status", "TEXT");
    }

    private void normalizeData(Connection connection) throws SQLException {
        if (columnExists(connection, "events", "name")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        UPDATE events
                        SET title = COALESCE(NULLIF(title, ''), name)
                        WHERE title IS NULL OR TRIM(title) = ''
                        """);
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE events SET event_time = '19:00' WHERE event_time IS NULL OR TRIM(event_time) = ''");
            statement.executeUpdate("UPDATE members SET is_active = 1 WHERE is_active IS NULL");
            statement.executeUpdate("UPDATE contributions SET period_label = strftime('%Y', contribution_date) WHERE period_label IS NULL OR TRIM(period_label) = ''");
            statement.executeUpdate("UPDATE contributions SET status = 'PAID' WHERE status IS NULL OR TRIM(status) = ''");
        }
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
