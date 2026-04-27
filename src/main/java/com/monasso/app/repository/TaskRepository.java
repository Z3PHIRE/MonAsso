package com.monasso.app.repository;

import com.monasso.app.model.TaskItem;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {

    private static final String BASE_SELECT = """
            SELECT
                t.id,
                t.title,
                t.link_type,
                t.linked_event_id,
                t.linked_meeting_id,
                CASE
                    WHEN t.link_type = 'EVENT' THEN COALESCE(e.title, 'Evenement #' || t.linked_event_id)
                    WHEN t.link_type = 'MEETING' THEN COALESCE(me.title, 'Reunion #' || t.linked_meeting_id)
                    ELSE ''
                END AS linked_label,
                t.assignee_member_id,
                (m.first_name || ' ' || m.last_name) AS assignee_name,
                t.due_date,
                t.priority,
                t.status,
                t.notes,
                t.created_at
            FROM tasks t
            LEFT JOIN members m ON m.id = t.assignee_member_id
            LEFT JOIN events e ON e.id = t.linked_event_id
            LEFT JOIN meetings me ON me.id = t.linked_meeting_id
            """;

    private final DatabaseManager databaseManager;

    public TaskRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<TaskItem> findByCriteria(
            String query,
            Long assigneeMemberId,
            LocalDate dueDateTo,
            TaskStatus status
    ) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();

        if (assigneeMemberId != null) {
            sql.append(" AND t.assignee_member_id = ? ");
            parameters.add(assigneeMemberId);
        }
        if (dueDateTo != null) {
            sql.append(" AND t.due_date IS NOT NULL AND t.due_date <= ? ");
            parameters.add(dueDateTo.toString());
        }
        if (status != null) {
            sql.append(" AND t.status = ? ");
            parameters.add(status.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                    AND (
                        LOWER(COALESCE(t.title, '')) LIKE ?
                        OR LOWER(COALESCE(t.notes, '')) LIKE ?
                        OR LOWER(COALESCE((m.first_name || ' ' || m.last_name), '')) LIKE ?
                        OR LOWER(COALESCE(e.title, '')) LIKE ?
                        OR LOWER(COALESCE(me.title, '')) LIKE ?
                    )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY ");
        sql.append("""
                CASE t.priority
                    WHEN 'CRITICAL' THEN 4
                    WHEN 'HIGH' THEN 3
                    WHEN 'MEDIUM' THEN 2
                    ELSE 1
                END DESC,
                CASE WHEN t.due_date IS NULL THEN 1 ELSE 0 END ASC,
                t.due_date ASC,
                t.id DESC
                """);

        List<TaskItem> tasks = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les taches.", e);
        }
    }

    public List<TaskItem> findUrgent(LocalDate fromDate, LocalDate toDate, int limit) {
        int safeLimit = limit <= 0 ? 10 : limit;
        String sql = BASE_SELECT + """
                WHERE t.status <> 'DONE'
                  AND t.due_date IS NOT NULL
                  AND t.due_date >= ?
                  AND t.due_date <= ?
                ORDER BY
                    CASE t.priority
                        WHEN 'CRITICAL' THEN 4
                        WHEN 'HIGH' THEN 3
                        WHEN 'MEDIUM' THEN 2
                        ELSE 1
                    END DESC,
                    t.due_date ASC,
                    t.id DESC
                LIMIT ?
                """;
        List<TaskItem> tasks = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            statement.setString(2, toDate.toString());
            statement.setInt(3, safeLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les taches urgentes.", e);
        }
    }

    public Optional<TaskItem> findById(long taskId) {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la tache " + taskId, e);
        }
    }

    public TaskItem create(TaskItem task) {
        String sql = """
                INSERT INTO tasks(
                    title,
                    link_type,
                    linked_event_id,
                    linked_meeting_id,
                    assignee_member_id,
                    due_date,
                    priority,
                    status,
                    notes,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTask(statement, task);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1))
                            .orElseThrow(() -> new IllegalStateException("Tache creee mais introuvable."));
                }
            }
            throw new IllegalStateException("Creation tache impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer une tache.", e);
        }
    }

    public TaskItem update(TaskItem task) {
        String sql = """
                UPDATE tasks
                SET title = ?,
                    link_type = ?,
                    linked_event_id = ?,
                    linked_meeting_id = ?,
                    assignee_member_id = ?,
                    due_date = ?,
                    priority = ?,
                    status = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTask(statement, task);
            statement.setLong(10, task.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("La tache n'existe plus.");
            }
            return findById(task.id()).orElse(task);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour la tache " + task.id(), e);
        }
    }

    public boolean deleteById(long taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la tache " + taskId, e);
        }
    }

    public long countOpenByEvent(long eventId) {
        return countOpenByCondition("linked_event_id = ?", eventId);
    }

    public long countOpenByMeeting(long meetingId) {
        return countOpenByCondition("linked_meeting_id = ?", meetingId);
    }

    private long countOpenByCondition(String condition, long linkedId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status <> 'DONE' AND " + condition;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, linkedId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les taches ouvertes.", e);
        }
    }

    private void bindTask(PreparedStatement statement, TaskItem task) throws SQLException {
        statement.setString(1, task.title());
        statement.setString(2, (task.linkType() == null ? TaskLinkType.NONE : task.linkType()).name());
        if (task.linkedEventId() == null) {
            statement.setNull(3, java.sql.Types.BIGINT);
        } else {
            statement.setLong(3, task.linkedEventId());
        }
        if (task.linkedMeetingId() == null) {
            statement.setNull(4, java.sql.Types.BIGINT);
        } else {
            statement.setLong(4, task.linkedMeetingId());
        }
        if (task.assigneeMemberId() == null) {
            statement.setNull(5, java.sql.Types.BIGINT);
        } else {
            statement.setLong(5, task.assigneeMemberId());
        }
        statement.setString(6, task.dueDate() == null ? null : task.dueDate().toString());
        statement.setString(7, (task.priority() == null ? TaskPriority.MEDIUM : task.priority()).name());
        statement.setString(8, (task.status() == null ? TaskStatus.TODO : task.status()).name());
        statement.setString(9, task.notes());
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private TaskItem mapRow(ResultSet rs) throws SQLException {
        String dueDateRaw = rs.getString("due_date");
        return new TaskItem(
                rs.getLong("id"),
                rs.getString("title"),
                TaskLinkType.fromDatabase(rs.getString("link_type")),
                rs.getObject("linked_event_id") == null ? null : rs.getLong("linked_event_id"),
                rs.getObject("linked_meeting_id") == null ? null : rs.getLong("linked_meeting_id"),
                rs.getString("linked_label"),
                rs.getObject("assignee_member_id") == null ? null : rs.getLong("assignee_member_id"),
                rs.getString("assignee_name"),
                dueDateRaw == null || dueDateRaw.isBlank() ? null : LocalDate.parse(dueDateRaw),
                TaskPriority.fromDatabase(rs.getString("priority")),
                TaskStatus.fromDatabase(rs.getString("status")),
                rs.getString("notes"),
                rs.getString("created_at")
        );
    }
}
