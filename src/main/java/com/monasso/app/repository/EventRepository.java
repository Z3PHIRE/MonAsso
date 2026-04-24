package com.monasso.app.repository;

import com.monasso.app.model.Event;

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

    private final DatabaseManager databaseManager;

    public EventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Event> findByCriteria(String query, boolean upcomingOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.id,
                    COALESCE(NULLIF(e.title, ''), 'Evenement sans titre') AS title,
                    e.event_date,
                    COALESCE(NULLIF(e.event_time, ''), '19:00') AS event_time,
                    e.location,
                    e.description,
                    e.capacity,
                    (
                        SELECT COUNT(*)
                        FROM event_participants ep
                        WHERE ep.event_id = e.id
                    ) AS participant_count
                FROM events e
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();

        if (upcomingOnly) {
            sql.append(" AND e.event_date >= ?");
            parameters.add(LocalDate.now().toString());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(COALESCE(e.title, '')) LIKE ?
                        OR LOWER(COALESCE(e.location, '')) LIKE ?
                        OR LOWER(COALESCE(e.description, '')) LIKE ?
                     )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
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

    public Optional<Event> findById(long eventId) {
        String sql = """
                SELECT
                    e.id,
                    COALESCE(NULLIF(e.title, ''), 'Evenement sans titre') AS title,
                    e.event_date,
                    COALESCE(NULLIF(e.event_time, ''), '19:00') AS event_time,
                    e.location,
                    e.description,
                    e.capacity,
                    (
                        SELECT COUNT(*)
                        FROM event_participants ep
                        WHERE ep.event_id = e.id
                    ) AS participant_count
                FROM events e
                WHERE e.id = ?
                """;
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
                INSERT INTO events(title, event_date, event_time, location, description, capacity)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEvent(statement, event);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Event(
                            keys.getLong(1),
                            event.title(),
                            event.eventDate(),
                            event.eventTime(),
                            event.location(),
                            event.description(),
                            event.capacity(),
                            0
                    );
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
                SET title = ?, event_date = ?, event_time = ?, location = ?, description = ?, capacity = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEvent(statement, event);
            statement.setLong(7, event.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("L'evenement n'existe plus.");
            }
            return event;
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
        return countWithCondition("event_date >= ?", fromDate.toString());
    }

    public List<Event> findUpcoming(LocalDate fromDate, int limit) {
        String sql = """
                SELECT
                    e.id,
                    COALESCE(NULLIF(e.title, ''), 'Evenement sans titre') AS title,
                    e.event_date,
                    COALESCE(NULLIF(e.event_time, ''), '19:00') AS event_time,
                    e.location,
                    e.description,
                    e.capacity,
                    (
                        SELECT COUNT(*)
                        FROM event_participants ep
                        WHERE ep.event_id = e.id
                    ) AS participant_count
                FROM events e
                WHERE e.event_date >= ?
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
        statement.setString(4, event.location());
        statement.setString(5, event.description());
        if (event.capacity() == null) {
            statement.setNull(6, java.sql.Types.INTEGER);
        } else {
            statement.setInt(6, event.capacity());
        }
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
                rs.getString("location"),
                rs.getString("description"),
                capacity == null ? null : rs.getInt("capacity"),
                rs.getInt("participant_count")
        );
    }
}
