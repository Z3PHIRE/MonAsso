package com.monasso.app.repository;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Event;
import com.monasso.app.model.ScheduleStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventRepository {

    private static final String BASE_SELECT = """
            SELECT
                e.id,
                COALESCE(NULLIF(e.title, ''), 'Evenement sans titre') AS title,
                e.event_date,
                COALESCE(NULLIF(e.event_time, ''), '19:00') AS event_time,
                COALESCE(NULLIF(e.end_time, ''), time(COALESCE(NULLIF(e.event_time, ''), '19:00'), '+02:00')) AS end_time,
                e.location,
                e.description,
                e.capacity,
                e.responsible_member_id,
                (rm.first_name || ' ' || rm.last_name) AS responsible_name,
                COALESCE(NULLIF(e.status, ''), 'CONFIRMED') AS status,
                e.category,
                e.materials,
                e.logistics_needs,
                e.partners,
                e.internal_notes,
                e.is_archived,
                (
                    SELECT COUNT(*)
                    FROM event_participants ep
                    WHERE ep.event_id = e.id
                ) AS participant_count
            FROM events e
            LEFT JOIN members rm ON rm.id = e.responsible_member_id
            """;

    private final DatabaseManager databaseManager;

    public EventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Event> findByCriteria(String query, boolean upcomingOnly) {
        return findByCriteria(query, upcomingOnly, ArchiveFilter.ACTIVE);
    }

    public List<Event> findByCriteria(String query, boolean upcomingOnly, ArchiveFilter archiveFilter) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();

        applyArchiveFilter(sql, parameters, archiveFilter);
        if (upcomingOnly) {
            sql.append(" AND e.event_date >= ? ");
            parameters.add(LocalDate.now().toString());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(COALESCE(e.title, '')) LIKE ?
                        OR LOWER(COALESCE(e.location, '')) LIKE ?
                        OR LOWER(COALESCE(e.description, '')) LIKE ?
                        OR LOWER(COALESCE(e.category, '')) LIKE ?
                        OR LOWER(COALESCE(e.materials, '')) LIKE ?
                        OR LOWER(COALESCE(e.logistics_needs, '')) LIKE ?
                        OR LOWER(COALESCE(e.partners, '')) LIKE ?
                        OR LOWER(COALESCE(e.internal_notes, '')) LIKE ?
                        OR LOWER(COALESCE((rm.first_name || ' ' || rm.last_name), '')) LIKE ?
                     )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY e.event_date ASC, e.event_time ASC, e.id ASC");

        List<Event> events = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les evenements.", e);
        }
    }

    public List<Event> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        return findByDateRange(fromDate, toDate, ArchiveFilter.ACTIVE);
    }

    public List<Event> findByDateRange(LocalDate fromDate, LocalDate toDate, ArchiveFilter archiveFilter) {
        String sql = BASE_SELECT + """
                WHERE e.event_date >= ?
                  AND e.event_date <= ?
                  AND (%s)
                ORDER BY e.event_date ASC, e.event_time ASC, e.id ASC
                """.formatted(buildArchiveSqlCondition(archiveFilter));
        List<Event> events = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            statement.setString(2, toDate.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les evenements par periode.", e);
        }
    }

    public Optional<Event> findById(long eventId) {
        String sql = BASE_SELECT + " WHERE e.id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger l'evenement " + eventId, e);
        }
    }

    public Event create(Event event) {
        String sql = """
                INSERT INTO events(
                    title,
                    event_date,
                    event_time,
                    end_time,
                    location,
                    description,
                    capacity,
                    responsible_member_id,
                    status,
                    category,
                    materials,
                    logistics_needs,
                    partners,
                    internal_notes,
                    is_archived
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEvent(statement, event);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1))
                            .orElseThrow(() -> new IllegalStateException("Evenement cree mais introuvable."));
                }
            }
            throw new IllegalStateException("Creation evenement impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer un evenement.", e);
        }
    }

    public Event update(Event event) {
        String sql = """
                UPDATE events
                SET title = ?,
                    event_date = ?,
                    event_time = ?,
                    end_time = ?,
                    location = ?,
                    description = ?,
                    capacity = ?,
                    responsible_member_id = ?,
                    status = ?,
                    category = ?,
                    materials = ?,
                    logistics_needs = ?,
                    partners = ?,
                    internal_notes = ?,
                    is_archived = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEvent(statement, event);
            statement.setLong(16, event.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("L'evenement n'existe plus.");
            }
            return findById(event.id()).orElse(event);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour l'evenement " + event.id(), e);
        }
    }

    public boolean deleteById(long eventId) {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer l'evenement " + eventId, e);
        }
    }

    public long countAll() {
        return countWithCondition(null, null);
    }

    public long countUpcoming(LocalDate fromDate) {
        return countWithCondition("event_date >= ? AND is_archived = 0", fromDate.toString());
    }

    public List<Event> findUpcoming(LocalDate fromDate, int limit) {
        String sql = BASE_SELECT + """
                WHERE e.event_date >= ?
                  AND e.is_archived = 0
                ORDER BY e.event_date ASC, e.event_time ASC, e.id ASC
                LIMIT ?
                """;
        List<Event> events = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les prochains evenements.", e);
        }
    }

    public boolean setArchived(long eventId, boolean archived) {
        String sql = "UPDATE events SET is_archived = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, archived ? 1 : 0);
            statement.setLong(2, eventId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour l'archive evenement " + eventId, e);
        }
    }

    private long countWithCondition(String condition, String value) {
        String sql = "SELECT COUNT(*) FROM events" + (condition == null ? "" : " WHERE " + condition);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value != null) {
                statement.setString(1, value);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les evenements.", e);
        }
    }

    private void bindEvent(PreparedStatement statement, Event event) throws SQLException {
        statement.setString(1, event.title());
        statement.setString(2, event.eventDate().toString());
        statement.setString(3, event.eventTime().toString());
        statement.setString(4, event.endTime() == null ? null : event.endTime().toString());
        statement.setString(5, event.location());
        statement.setString(6, event.description());
        if (event.capacity() == null) {
            statement.setNull(7, java.sql.Types.INTEGER);
        } else {
            statement.setInt(7, event.capacity());
        }
        if (event.responsibleMemberId() == null) {
            statement.setNull(8, java.sql.Types.BIGINT);
        } else {
            statement.setLong(8, event.responsibleMemberId());
        }
        statement.setString(9, (event.status() == null ? ScheduleStatus.CONFIRMED : event.status()).name());
        statement.setString(10, event.category());
        statement.setString(11, event.materials());
        statement.setString(12, event.logisticsNeeds());
        statement.setString(13, event.partners());
        statement.setString(14, event.internalNotes());
        statement.setInt(15, event.archived() ? 1 : 0);
    }

    private void applyArchiveFilter(StringBuilder sql, List<Object> parameters, ArchiveFilter archiveFilter) {
        ArchiveFilter safeFilter = archiveFilter == null ? ArchiveFilter.ACTIVE : archiveFilter;
        switch (safeFilter) {
            case ACTIVE -> sql.append(" AND e.is_archived = 0 ");
            case ARCHIVED -> sql.append(" AND e.is_archived = 1 ");
            case ALL -> {
                // no filter
            }
        }
    }

    private String buildArchiveSqlCondition(ArchiveFilter archiveFilter) {
        ArchiveFilter safeFilter = archiveFilter == null ? ArchiveFilter.ACTIVE : archiveFilter;
        return switch (safeFilter) {
            case ACTIVE -> "e.is_archived = 0";
            case ARCHIVED -> "e.is_archived = 1";
            case ALL -> "1 = 1";
        };
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Event mapRow(ResultSet rs) throws SQLException {
        String capacity = rs.getString("capacity");
        return new Event(
                rs.getLong("id"),
                rs.getString("title"),
                LocalDate.parse(rs.getString("event_date")),
                LocalTime.parse(rs.getString("event_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getString("location"),
                rs.getString("description"),
                capacity == null ? null : rs.getInt("capacity"),
                rs.getObject("responsible_member_id") == null ? null : rs.getLong("responsible_member_id"),
                rs.getString("responsible_name"),
                ScheduleStatus.fromDatabase(rs.getString("status")),
                rs.getString("category"),
                rs.getString("materials"),
                rs.getString("logistics_needs"),
                rs.getString("partners"),
                rs.getString("internal_notes"),
                rs.getInt("is_archived") == 1,
                rs.getInt("participant_count")
        );
    }
}
