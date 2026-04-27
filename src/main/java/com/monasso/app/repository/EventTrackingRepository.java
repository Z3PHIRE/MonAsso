package com.monasso.app.repository;

import com.monasso.app.model.EventAttendanceStatus;
import com.monasso.app.model.EventBudgetLine;
import com.monasso.app.model.EventBudgetLineType;
import com.monasso.app.model.EventBudgetPhase;
import com.monasso.app.model.EventBudgetSummary;
import com.monasso.app.model.EventDocument;
import com.monasso.app.model.EventHistoryEntry;
import com.monasso.app.model.EventParticipantAttendance;
import com.monasso.app.model.EventTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventTrackingRepository {

    private final DatabaseManager databaseManager;

    public EventTrackingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<EventParticipantAttendance> findParticipantsWithAttendance(long eventId, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.id,
                    m.first_name,
                    m.last_name,
                    m.email,
                    m.is_active,
                    ep.attendance_status
                FROM event_participants ep
                JOIN members m ON m.id = ep.member_id
                WHERE ep.event_id = ?
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(eventId);

        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(m.first_name) LIKE ?
                        OR LOWER(m.last_name) LIKE ?
                        OR LOWER(COALESCE(m.email, '')) LIKE ?
                     )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY m.last_name ASC, m.first_name ASC");

        List<EventParticipantAttendance> participants = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String fullName = (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
                    participants.add(new EventParticipantAttendance(
                            rs.getLong("id"),
                            fullName,
                            rs.getString("email"),
                            rs.getInt("is_active") == 1,
                            EventAttendanceStatus.fromDatabase(rs.getString("attendance_status"))
                    ));
                }
            }
            return participants;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la presence des participants de l'evenement.", e);
        }
    }

    public void updateAttendanceStatus(long eventId, long memberId, EventAttendanceStatus status) {
        String sql = """
                UPDATE event_participants
                SET attendance_status = ?
                WHERE event_id = ?
                  AND member_id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, eventId);
            statement.setLong(3, memberId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Participant introuvable pour cet evenement.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour la presence du participant.", e);
        }
    }

    public List<EventBudgetLine> findBudgetLines(long eventId) {
        String sql = """
                SELECT
                    id,
                    event_id,
                    line_type,
                    budget_phase,
                    category,
                    label,
                    amount,
                    notes
                FROM event_budget_lines
                WHERE event_id = ?
                ORDER BY budget_phase ASC, line_type ASC, id ASC
                """;
        List<EventBudgetLine> lines = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    lines.add(mapBudgetLine(rs));
                }
            }
            return lines;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le budget evenement.", e);
        }
    }

    public EventBudgetLine addBudgetLine(
            long eventId,
            EventBudgetLineType lineType,
            EventBudgetPhase budgetPhase,
            String category,
            String label,
            double amount,
            String notes
    ) {
        String sql = """
                INSERT INTO event_budget_lines(
                    event_id,
                    line_type,
                    budget_phase,
                    category,
                    label,
                    amount,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, lineType.name());
            statement.setString(3, budgetPhase.name());
            statement.setString(4, category);
            statement.setString(5, label);
            statement.setDouble(6, amount);
            statement.setString(7, notes);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Creation ligne budget impossible.");
                }
                return findBudgetLineById(connection, keys.getLong(1))
                        .orElseThrow(() -> new IllegalStateException("Ligne budget creee mais introuvable."));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter une ligne budget evenement.", e);
        }
    }

    public boolean deleteBudgetLine(long budgetLineId) {
        String sql = "DELETE FROM event_budget_lines WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, budgetLineId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la ligne budget.", e);
        }
    }

    public EventBudgetSummary summarizeBudget(long eventId) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN line_type = 'REVENUE' AND budget_phase = 'PLANNED' THEN amount ELSE 0 END), 0) AS planned_revenue,
                    COALESCE(SUM(CASE WHEN line_type = 'EXPENSE' AND budget_phase = 'PLANNED' THEN amount ELSE 0 END), 0) AS planned_expense,
                    COALESCE(SUM(CASE WHEN line_type = 'REVENUE' AND budget_phase = 'ACTUAL' THEN amount ELSE 0 END), 0) AS actual_revenue,
                    COALESCE(SUM(CASE WHEN line_type = 'EXPENSE' AND budget_phase = 'ACTUAL' THEN amount ELSE 0 END), 0) AS actual_expense
                FROM event_budget_lines
                WHERE event_id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new EventBudgetSummary(
                            rs.getDouble("planned_revenue"),
                            rs.getDouble("planned_expense"),
                            rs.getDouble("actual_revenue"),
                            rs.getDouble("actual_expense")
                    );
                }
                return new EventBudgetSummary(0.0, 0.0, 0.0, 0.0);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de calculer le budget evenement.", e);
        }
    }

    public List<EventTask> findTasks(long eventId, boolean openOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.id,
                    t.event_id,
                    t.title,
                    t.description,
                    t.due_date,
                    t.responsible_member_id,
                    (m.first_name || ' ' || m.last_name) AS responsible_name,
                    t.is_completed
                FROM event_tasks t
                LEFT JOIN members m ON m.id = t.responsible_member_id
                WHERE t.event_id = ?
                """);
        if (openOnly) {
            sql.append(" AND t.is_completed = 0");
        }
        sql.append(" ORDER BY t.is_completed ASC, t.due_date ASC, t.id ASC");

        List<EventTask> tasks = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les taches de suivi evenement.", e);
        }
    }

    public EventTask addTask(
            long eventId,
            String title,
            String description,
            LocalDate dueDate,
            Long responsibleMemberId
    ) {
        String sql = """
                INSERT INTO event_tasks(
                    event_id,
                    title,
                    description,
                    due_date,
                    responsible_member_id,
                    is_completed,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setString(4, dueDate == null ? null : dueDate.toString());
            if (responsibleMemberId == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, responsibleMemberId);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Creation tache evenement impossible.");
                }
                return findTaskById(connection, keys.getLong(1))
                        .orElseThrow(() -> new IllegalStateException("Tache creee mais introuvable."));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter une tache de suivi evenement.", e);
        }
    }

    public EventTask setTaskCompleted(long taskId, boolean completed) {
        String sql = """
                UPDATE event_tasks
                SET is_completed = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, completed ? 1 : 0);
            statement.setLong(2, taskId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("La tache n'existe plus.");
            }
            return findTaskById(connection, taskId)
                    .orElseThrow(() -> new IllegalStateException("Tache introuvable apres mise a jour."));
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de modifier l'etat de la tache evenement.", e);
        }
    }

    public boolean deleteTask(long taskId) {
        String sql = "DELETE FROM event_tasks WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la tache evenement.", e);
        }
    }

    public List<EventDocument> findDocuments(long eventId) {
        String sql = """
                SELECT
                    id,
                    event_id,
                    document_name,
                    document_ref,
                    notes
                FROM event_documents
                WHERE event_id = ?
                ORDER BY id DESC
                """;
        List<EventDocument> documents = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    documents.add(new EventDocument(
                            rs.getLong("id"),
                            rs.getLong("event_id"),
                            rs.getString("document_name"),
                            rs.getString("document_ref"),
                            rs.getString("notes")
                    ));
                }
            }
            return documents;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les documents evenement.", e);
        }
    }

    public EventDocument addDocument(long eventId, String documentName, String documentRef, String notes) {
        String sql = """
                INSERT INTO event_documents(event_id, document_name, document_ref, notes)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, documentName);
            statement.setString(3, documentRef);
            statement.setString(4, notes);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Creation document evenement impossible.");
                }
                long id = keys.getLong(1);
                return findDocumentById(connection, id)
                        .orElseThrow(() -> new IllegalStateException("Document cree mais introuvable."));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter un document evenement.", e);
        }
    }

    public boolean deleteDocument(long documentId) {
        String sql = "DELETE FROM event_documents WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, documentId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le document evenement.", e);
        }
    }

    public List<EventHistoryEntry> findHistory(long eventId, int limit) {
        int safeLimit = limit <= 0 ? 40 : limit;
        String sql = """
                SELECT
                    id,
                    event_id,
                    action_type,
                    details,
                    created_at
                FROM event_history
                WHERE event_id = ?
                ORDER BY id DESC
                LIMIT ?
                """;
        List<EventHistoryEntry> history = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setInt(2, safeLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    history.add(new EventHistoryEntry(
                            rs.getLong("id"),
                            rs.getLong("event_id"),
                            rs.getString("action_type"),
                            rs.getString("details"),
                            rs.getString("created_at")
                    ));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger l'historique evenement.", e);
        }
    }

    public void addHistory(long eventId, String actionType, String details) {
        String sql = """
                INSERT INTO event_history(event_id, action_type, details)
                VALUES (?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setString(2, actionType);
            statement.setString(3, details);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter une entree d'historique evenement.", e);
        }
    }

    public int countParticipants(long eventId) {
        return countSimpleInt("SELECT COUNT(*) FROM event_participants WHERE event_id = ?", eventId);
    }

    public int countParticipantsByStatus(long eventId, EventAttendanceStatus status) {
        String sql = """
                SELECT COUNT(*)
                FROM event_participants
                WHERE event_id = ?
                  AND attendance_status = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setString(2, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les statuts de presence participants.", e);
        }
    }

    public int countTasks(long eventId) {
        return countSimpleInt("SELECT COUNT(*) FROM event_tasks WHERE event_id = ?", eventId);
    }

    public int countCompletedTasks(long eventId) {
        return countSimpleInt("SELECT COUNT(*) FROM event_tasks WHERE event_id = ? AND is_completed = 1", eventId);
    }

    public int countChecklistItems(long eventId) {
        String sql = """
                SELECT COUNT(*)
                FROM checklist_items
                WHERE target_type = 'EVENT'
                  AND target_id = ?
                """;
        return countSimpleInt(sql, eventId);
    }

    public int countCompletedChecklistItems(long eventId) {
        String sql = """
                SELECT COUNT(*)
                FROM checklist_items
                WHERE target_type = 'EVENT'
                  AND target_id = ?
                  AND is_checked = 1
                """;
        return countSimpleInt(sql, eventId);
    }

    private java.util.Optional<EventBudgetLine> findBudgetLineById(Connection connection, long lineId) throws SQLException {
        String sql = """
                SELECT
                    id,
                    event_id,
                    line_type,
                    budget_phase,
                    category,
                    label,
                    amount,
                    notes
                FROM event_budget_lines
                WHERE id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lineId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(mapBudgetLine(rs));
            }
        }
    }

    private java.util.Optional<EventTask> findTaskById(Connection connection, long taskId) throws SQLException {
        String sql = """
                SELECT
                    t.id,
                    t.event_id,
                    t.title,
                    t.description,
                    t.due_date,
                    t.responsible_member_id,
                    (m.first_name || ' ' || m.last_name) AS responsible_name,
                    t.is_completed
                FROM event_tasks t
                LEFT JOIN members m ON m.id = t.responsible_member_id
                WHERE t.id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(mapTask(rs));
            }
        }
    }

    private java.util.Optional<EventDocument> findDocumentById(Connection connection, long documentId) throws SQLException {
        String sql = """
                SELECT id, event_id, document_name, document_ref, notes
                FROM event_documents
                WHERE id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, documentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new EventDocument(
                        rs.getLong("id"),
                        rs.getLong("event_id"),
                        rs.getString("document_name"),
                        rs.getString("document_ref"),
                        rs.getString("notes")
                ));
            }
        }
    }

    private EventBudgetLine mapBudgetLine(ResultSet rs) throws SQLException {
        return new EventBudgetLine(
                rs.getLong("id"),
                rs.getLong("event_id"),
                EventBudgetLineType.fromDatabase(rs.getString("line_type")),
                EventBudgetPhase.fromDatabase(rs.getString("budget_phase")),
                rs.getString("category"),
                rs.getString("label"),
                rs.getDouble("amount"),
                rs.getString("notes")
        );
    }

    private EventTask mapTask(ResultSet rs) throws SQLException {
        String dueDateRaw = rs.getString("due_date");
        return new EventTask(
                rs.getLong("id"),
                rs.getLong("event_id"),
                rs.getString("title"),
                rs.getString("description"),
                dueDateRaw == null || dueDateRaw.isBlank() ? null : LocalDate.parse(dueDateRaw),
                rs.getObject("responsible_member_id") == null ? null : rs.getLong("responsible_member_id"),
                rs.getString("responsible_name"),
                rs.getInt("is_completed") == 1
        );
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private int countSimpleInt(String sql, long eventId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de calculer les indicateurs de suivi evenement.", e);
        }
    }
}
