package com.monasso.app.repository;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomFieldType;

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
                    person_type TEXT NOT NULL DEFAULT 'MEMBER',
                    email TEXT,
                    phone TEXT,
                    association_role TEXT,
                    skills TEXT,
                    availability TEXT,
                    emergency_contact TEXT,
                    clothing_size TEXT,
                    certifications TEXT,
                    constraints_info TEXT,
                    linked_documents TEXT,
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
                    end_time TEXT,
                    location TEXT,
                    description TEXT,
                    capacity INTEGER,
                    responsible_member_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'CONFIRMED',
                    category TEXT,
                    materials TEXT,
                    logistics_needs TEXT,
                    partners TEXT,
                    internal_notes TEXT,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(responsible_member_id) REFERENCES members(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS meetings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    meeting_date TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    location TEXT,
                    organizer TEXT,
                    responsible_member_id INTEGER,
                    agenda TEXT,
                    notes TEXT,
                    status TEXT NOT NULL DEFAULT 'PLANNED',
                    category TEXT,
                    linked_documents TEXT,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(responsible_member_id) REFERENCES members(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS meeting_participants (
                    meeting_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    registration_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(meeting_id, member_id),
                    FOREIGN KEY(meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
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
                    attendance_status TEXT NOT NULL DEFAULT 'REGISTERED',
                    registration_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(event_id, member_id),
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS event_budget_lines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    line_type TEXT NOT NULL,
                    budget_phase TEXT NOT NULL,
                    category TEXT,
                    label TEXT NOT NULL,
                    amount REAL NOT NULL,
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS event_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    due_date TEXT,
                    responsible_member_id INTEGER,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
                    FOREIGN KEY(responsible_member_id) REFERENCES members(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS event_documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    document_name TEXT NOT NULL,
                    document_ref TEXT,
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS event_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    details TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    link_type TEXT NOT NULL DEFAULT 'NONE',
                    linked_event_id INTEGER,
                    linked_meeting_id INTEGER,
                    assignee_member_id INTEGER,
                    due_date TEXT,
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    status TEXT NOT NULL DEFAULT 'TODO',
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(linked_event_id) REFERENCES events(id) ON DELETE SET NULL,
                    FOREIGN KEY(linked_meeting_id) REFERENCES meetings(id) ON DELETE SET NULL,
                    FOREIGN KEY(assignee_member_id) REFERENCES members(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_type TEXT NOT NULL,
                    target_id INTEGER NOT NULL,
                    file_path TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    file_type TEXT,
                    notes TEXT,
                    added_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS custom_categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    parent_id INTEGER,
                    field_type TEXT NOT NULL,
                    list_options TEXT,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    is_system INTEGER NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(parent_id) REFERENCES custom_categories(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS custom_category_scopes (
                    category_id INTEGER NOT NULL,
                    scope_type TEXT NOT NULL,
                    PRIMARY KEY(category_id, scope_type),
                    FOREIGN KEY(category_id) REFERENCES custom_categories(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS custom_category_values (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER NOT NULL,
                    scope_type TEXT NOT NULL,
                    target_id INTEGER NOT NULL,
                    value_type TEXT NOT NULL,
                    text_value TEXT,
                    number_value REAL,
                    date_value TEXT,
                    bool_value INTEGER,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(category_id, scope_type, target_id),
                    FOREIGN KEY(category_id) REFERENCES custom_categories(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS checklist_categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS checklist_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_type TEXT NOT NULL,
                    target_id INTEGER NOT NULL,
                    category_id INTEGER,
                    label TEXT NOT NULL,
                    is_checked INTEGER NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(category_id) REFERENCES checklist_categories(id) ON DELETE SET NULL
                )
                """
        );

        List<String> createIndexStatements = List.of(
                "CREATE INDEX IF NOT EXISTS idx_members_name ON members(last_name, first_name)",
                "CREATE INDEX IF NOT EXISTS idx_members_active ON members(is_active)",
                "CREATE INDEX IF NOT EXISTS idx_events_date ON events(event_date)",
                "CREATE INDEX IF NOT EXISTS idx_events_status ON events(status)",
                "CREATE INDEX IF NOT EXISTS idx_events_responsible ON events(responsible_member_id)",
                "CREATE INDEX IF NOT EXISTS idx_events_category ON events(category)",
                "CREATE INDEX IF NOT EXISTS idx_events_archived ON events(is_archived)",
                "CREATE INDEX IF NOT EXISTS idx_event_participants_event ON event_participants(event_id)",
                "CREATE INDEX IF NOT EXISTS idx_event_participants_member ON event_participants(member_id)",
                "CREATE INDEX IF NOT EXISTS idx_event_participants_attendance ON event_participants(event_id, attendance_status)",
                "CREATE INDEX IF NOT EXISTS idx_event_budget_lines_event ON event_budget_lines(event_id)",
                "CREATE INDEX IF NOT EXISTS idx_event_budget_lines_phase ON event_budget_lines(event_id, budget_phase, line_type)",
                "CREATE INDEX IF NOT EXISTS idx_event_tasks_event ON event_tasks(event_id)",
                "CREATE INDEX IF NOT EXISTS idx_event_tasks_status ON event_tasks(event_id, is_completed)",
                "CREATE INDEX IF NOT EXISTS idx_event_documents_event ON event_documents(event_id)",
                "CREATE INDEX IF NOT EXISTS idx_event_history_event ON event_history(event_id)",
                "CREATE INDEX IF NOT EXISTS idx_meetings_date ON meetings(meeting_date)",
                "CREATE INDEX IF NOT EXISTS idx_meetings_status ON meetings(status)",
                "CREATE INDEX IF NOT EXISTS idx_meetings_responsible ON meetings(responsible_member_id)",
                "CREATE INDEX IF NOT EXISTS idx_meetings_category ON meetings(category)",
                "CREATE INDEX IF NOT EXISTS idx_meetings_archived ON meetings(is_archived)",
                "CREATE INDEX IF NOT EXISTS idx_meeting_participants_meeting ON meeting_participants(meeting_id)",
                "CREATE INDEX IF NOT EXISTS idx_meeting_participants_member ON meeting_participants(member_id)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_assignee ON tasks(assignee_member_id)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_linked_event ON tasks(linked_event_id)",
                "CREATE INDEX IF NOT EXISTS idx_tasks_linked_meeting ON tasks(linked_meeting_id)",
                "CREATE INDEX IF NOT EXISTS idx_documents_target ON documents(target_type, target_id)",
                "CREATE INDEX IF NOT EXISTS idx_contributions_member_period ON contributions(member_id, period_label)",
                "CREATE INDEX IF NOT EXISTS idx_contributions_status ON contributions(status)",
                "CREATE INDEX IF NOT EXISTS idx_custom_categories_parent ON custom_categories(parent_id)",
                "CREATE INDEX IF NOT EXISTS idx_custom_category_scopes_scope ON custom_category_scopes(scope_type)",
                "CREATE INDEX IF NOT EXISTS idx_custom_category_values_target ON custom_category_values(scope_type, target_id)",
                "CREATE INDEX IF NOT EXISTS idx_checklist_items_target ON checklist_items(target_type, target_id)",
                "CREATE INDEX IF NOT EXISTS idx_checklist_items_category ON checklist_items(category_id)"
        );

        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);

            for (String sql : createTableStatements) {
                statement.execute(sql);
            }
            migrateLegacySchema(connection);
            normalizeData(connection);
            seedDefaultCategories(connection);
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
        addColumnIfMissing(connection, "members", "person_type", "TEXT NOT NULL DEFAULT 'MEMBER'");
        addColumnIfMissing(connection, "members", "association_role", "TEXT");
        addColumnIfMissing(connection, "members", "skills", "TEXT");
        addColumnIfMissing(connection, "members", "availability", "TEXT");
        addColumnIfMissing(connection, "members", "emergency_contact", "TEXT");
        addColumnIfMissing(connection, "members", "clothing_size", "TEXT");
        addColumnIfMissing(connection, "members", "certifications", "TEXT");
        addColumnIfMissing(connection, "members", "constraints_info", "TEXT");
        addColumnIfMissing(connection, "members", "linked_documents", "TEXT");
        addColumnIfMissing(connection, "members", "is_active", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(connection, "members", "notes", "TEXT");

        addColumnIfMissing(connection, "events", "title", "TEXT");
        addColumnIfMissing(connection, "events", "event_time", "TEXT");
        addColumnIfMissing(connection, "events", "end_time", "TEXT");
        addColumnIfMissing(connection, "events", "capacity", "INTEGER");
        addColumnIfMissing(connection, "events", "responsible_member_id", "INTEGER");
        addColumnIfMissing(connection, "events", "status", "TEXT NOT NULL DEFAULT 'CONFIRMED'");
        addColumnIfMissing(connection, "events", "category", "TEXT");
        addColumnIfMissing(connection, "events", "materials", "TEXT");
        addColumnIfMissing(connection, "events", "logistics_needs", "TEXT");
        addColumnIfMissing(connection, "events", "partners", "TEXT");
        addColumnIfMissing(connection, "events", "internal_notes", "TEXT");
        addColumnIfMissing(connection, "events", "is_archived", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "event_participants", "attendance_status", "TEXT NOT NULL DEFAULT 'REGISTERED'");
        addColumnIfMissing(connection, "meetings", "is_archived", "INTEGER NOT NULL DEFAULT 0");

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
            statement.executeUpdate("UPDATE events SET end_time = time(COALESCE(NULLIF(event_time, ''), '19:00'), '+02:00') WHERE end_time IS NULL OR TRIM(end_time) = ''");
            statement.executeUpdate("UPDATE events SET status = 'CONFIRMED' WHERE status IS NULL OR TRIM(status) = ''");
            statement.executeUpdate("UPDATE events SET is_archived = 0 WHERE is_archived IS NULL");
            statement.executeUpdate("UPDATE meetings SET is_archived = 0 WHERE is_archived IS NULL");
            statement.executeUpdate("UPDATE event_participants SET attendance_status = 'REGISTERED' WHERE attendance_status IS NULL OR TRIM(attendance_status) = ''");
            statement.executeUpdate("UPDATE members SET is_active = 1 WHERE is_active IS NULL");
            statement.executeUpdate("UPDATE members SET person_type = 'MEMBER' WHERE person_type IS NULL OR TRIM(person_type) = ''");
            statement.executeUpdate("UPDATE contributions SET period_label = strftime('%Y', contribution_date) WHERE period_label IS NULL OR TRIM(period_label) = ''");
            statement.executeUpdate("UPDATE contributions SET status = 'PAID' WHERE status IS NULL OR TRIM(status) = ''");
        }
    }

    private void seedDefaultCategories(Connection connection) throws SQLException {
        List<String> defaults = List.of(
                "Vetements",
                "Materiel",
                "Budget",
                "Restauration",
                "Transport",
                "Communication",
                "Securite",
                "Administratif"
        );

        int sortOrder = 10;
        for (String categoryName : defaults) {
            long categoryId = ensureTopLevelCategory(connection, categoryName, CustomFieldType.SHORT_TEXT, sortOrder++);
            for (CategoryScope scope : CategoryScope.values()) {
                ensureCategoryScope(connection, categoryId, scope);
            }
            ensureChecklistCategory(connection, categoryName, sortOrder);
        }
    }

    private long ensureTopLevelCategory(
            Connection connection,
            String name,
            CustomFieldType fieldType,
            int sortOrder
    ) throws SQLException {
        String selectSql = """
                SELECT id
                FROM custom_categories
                WHERE parent_id IS NULL
                  AND LOWER(name) = LOWER(?)
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        String insertSql = """
                INSERT INTO custom_categories(name, parent_id, field_type, list_options, is_active, is_system, sort_order, updated_at)
                VALUES (?, NULL, ?, NULL, 1, 1, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, fieldType.name());
            statement.setInt(3, sortOrder);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new IllegalStateException("Impossible d'inserer la categorie systeme: " + name);
    }

    private void ensureCategoryScope(Connection connection, long categoryId, CategoryScope scope) throws SQLException {
        String insertSql = """
                INSERT INTO custom_category_scopes(category_id, scope_type)
                VALUES (?, ?)
                ON CONFLICT(category_id, scope_type) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setLong(1, categoryId);
            statement.setString(2, scope.name());
            statement.executeUpdate();
        }
    }

    private void ensureChecklistCategory(Connection connection, String name, int sortOrder) throws SQLException {
        String insertSql = """
                INSERT INTO checklist_categories(name, is_active, sort_order)
                VALUES (?, 1, ?)
                ON CONFLICT(name) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, name);
            statement.setInt(2, sortOrder);
            statement.executeUpdate();
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
