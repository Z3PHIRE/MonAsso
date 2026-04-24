package com.monasso.app.repository;

import com.monasso.app.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventRepository {

    private final DatabaseManager databaseManager;

    public EventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Event> findAll() {
        String sql = """
                SELECT id, name, event_date, location, description
                FROM events
                ORDER BY event_date DESC, id DESC
                """;
        List<Event> events = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                events.add(mapRow(rs));
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les evenements.", e);
        }
    }

    public Event create(Event event) {
        String sql = """
                INSERT INTO events(name, event_date, location, description)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, event.name());
            statement.setString(2, event.eventDate().toString());
            statement.setString(3, event.location());
            statement.setString(4, event.description());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Event(keys.getLong(1), event.name(), event.eventDate(), event.location(), event.description());
                }
            }
            throw new IllegalStateException("Creation evenement impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer un evenement.", e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les evenements.", e);
        }
    }

    public long countUpcoming(LocalDate fromDate) {
        String sql = "SELECT COUNT(*) FROM events WHERE event_date >= ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les prochains evenements.", e);
        }
    }

    public List<Event> findUpcoming(LocalDate fromDate, int limit) {
        String sql = """
                SELECT id, name, event_date, location, description
                FROM events
                WHERE event_date >= ?
                ORDER BY event_date ASC, id ASC
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

    private Event mapRow(ResultSet rs) throws SQLException {
        return new Event(
                rs.getLong("id"),
                rs.getString("name"),
                LocalDate.parse(rs.getString("event_date")),
                rs.getString("location"),
                rs.getString("description")
        );
    }
}
