package com.monasso.app.repository;

import com.monasso.app.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventParticipantRepository {

    private final DatabaseManager databaseManager;

    public EventParticipantRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Member> findParticipants(long eventId, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id, m.first_name, m.last_name, m.email, m.phone, m.address, m.join_date, m.is_active, m.notes
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

        List<Member> participants = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapMember(rs));
                }
            }
            return participants;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les participants de l'evenement " + eventId, e);
        }
    }

    public void addParticipant(long eventId, long memberId) {
        String sql = """
                INSERT INTO event_participants(event_id, member_id, registration_date)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(event_id, member_id) DO NOTHING
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setLong(2, memberId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le participant a l'evenement.", e);
        }
    }

    public boolean removeParticipant(long eventId, long memberId) {
        String sql = "DELETE FROM event_participants WHERE event_id = ? AND member_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setLong(2, memberId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de retirer le participant de l'evenement.", e);
        }
    }

    public long countParticipants(long eventId) {
        String sql = "SELECT COUNT(*) FROM event_participants WHERE event_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les participants.", e);
        }
    }

    public Set<Long> findParticipantIds(long eventId) {
        String sql = "SELECT member_id FROM event_participants WHERE event_id = ?";
        Set<Long> ids = new HashSet<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("member_id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les identifiants participants.", e);
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Member mapMember(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                LocalDate.parse(rs.getString("join_date")),
                rs.getInt("is_active") == 1,
                rs.getString("notes")
        );
    }
}
