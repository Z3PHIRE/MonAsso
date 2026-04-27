package com.monasso.app.repository;

import com.monasso.app.model.Member;
import com.monasso.app.model.PersonType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeetingParticipantRepository {

    private final DatabaseManager databaseManager;

    public MeetingParticipantRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Member> findParticipants(long meetingId, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.id,
                    m.first_name,
                    m.last_name,
                    m.person_type,
                    m.email,
                    m.phone,
                    m.is_active,
                    m.address,
                    m.join_date,
                    m.association_role,
                    m.skills,
                    m.availability,
                    m.notes,
                    m.emergency_contact,
                    m.clothing_size,
                    m.certifications,
                    m.constraints_info,
                    m.linked_documents
                FROM meeting_participants mp
                JOIN members m ON m.id = mp.member_id
                WHERE mp.meeting_id = ?
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(meetingId);

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
            throw new IllegalStateException("Impossible de charger les participants de la reunion " + meetingId, e);
        }
    }

    public void addParticipant(long meetingId, long memberId) {
        String sql = """
                INSERT INTO meeting_participants(meeting_id, member_id, registration_date)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(meeting_id, member_id) DO NOTHING
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            statement.setLong(2, memberId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le participant a la reunion.", e);
        }
    }

    public boolean removeParticipant(long meetingId, long memberId) {
        String sql = "DELETE FROM meeting_participants WHERE meeting_id = ? AND member_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            statement.setLong(2, memberId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de retirer le participant de la reunion.", e);
        }
    }

    public long countParticipants(long meetingId) {
        String sql = "SELECT COUNT(*) FROM meeting_participants WHERE meeting_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les participants de reunion.", e);
        }
    }

    public Set<Long> findParticipantIds(long meetingId) {
        String sql = "SELECT member_id FROM meeting_participants WHERE meeting_id = ?";
        Set<Long> ids = new HashSet<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("member_id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les identifiants participants de reunion.", e);
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Member mapMember(ResultSet rs) throws SQLException {
        String joinDateRaw = rs.getString("join_date");
        return new Member(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                PersonType.fromDatabase(rs.getString("person_type")),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("is_active") == 1,
                rs.getString("address"),
                joinDateRaw == null || joinDateRaw.isBlank() ? LocalDate.now() : LocalDate.parse(joinDateRaw),
                rs.getString("association_role"),
                rs.getString("skills"),
                rs.getString("availability"),
                rs.getString("notes"),
                rs.getString("emergency_contact"),
                rs.getString("clothing_size"),
                rs.getString("certifications"),
                rs.getString("constraints_info"),
                rs.getString("linked_documents")
        );
    }
}
