package com.monasso.app.repository;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Meeting;
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

public class MeetingRepository {

    private static final String BASE_SELECT = """
            SELECT
                m.id,
                m.title,
                m.meeting_date,
                m.start_time,
                m.end_time,
                m.location,
                m.organizer,
                m.responsible_member_id,
                (rm.first_name || ' ' || rm.last_name) AS responsible_name,
                m.agenda,
                m.notes,
                COALESCE(NULLIF(m.status, ''), 'PLANNED') AS status,
                m.category,
                m.linked_documents,
                m.is_archived,
                (
                    SELECT COUNT(*)
                    FROM meeting_participants mp
                    WHERE mp.meeting_id = m.id
                ) AS participant_count
            FROM meetings m
            LEFT JOIN members rm ON rm.id = m.responsible_member_id
            """;

    private final DatabaseManager databaseManager;

    public MeetingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Meeting> findByCriteria(String query, boolean upcomingOnly) {
        return findByCriteria(query, upcomingOnly, ArchiveFilter.ACTIVE);
    }

    public List<Meeting> findByCriteria(String query, boolean upcomingOnly, ArchiveFilter archiveFilter) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();

        applyArchiveFilter(sql, archiveFilter);
        if (upcomingOnly) {
            sql.append(" AND m.meeting_date >= ? ");
            parameters.add(LocalDate.now().toString());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(COALESCE(m.title, '')) LIKE ?
                        OR LOWER(COALESCE(m.location, '')) LIKE ?
                        OR LOWER(COALESCE(m.organizer, '')) LIKE ?
                        OR LOWER(COALESCE(m.agenda, '')) LIKE ?
                        OR LOWER(COALESCE(m.notes, '')) LIKE ?
                        OR LOWER(COALESCE(m.category, '')) LIKE ?
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
        }
        sql.append(" ORDER BY m.meeting_date ASC, m.start_time ASC, m.id ASC");

        List<Meeting> meetings = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    meetings.add(mapRow(rs));
                }
            }
            return meetings;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les reunions.", e);
        }
    }

    public List<Meeting> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        return findByDateRange(fromDate, toDate, ArchiveFilter.ACTIVE);
    }

    public List<Meeting> findByDateRange(LocalDate fromDate, LocalDate toDate, ArchiveFilter archiveFilter) {
        String sql = BASE_SELECT + """
                WHERE m.meeting_date >= ?
                  AND m.meeting_date <= ?
                  AND (%s)
                ORDER BY m.meeting_date ASC, m.start_time ASC, m.id ASC
                """.formatted(buildArchiveSqlCondition(archiveFilter));
        List<Meeting> meetings = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            statement.setString(2, toDate.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    meetings.add(mapRow(rs));
                }
            }
            return meetings;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les reunions par periode.", e);
        }
    }

    public Optional<Meeting> findById(long meetingId) {
        String sql = BASE_SELECT + " WHERE m.id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la reunion " + meetingId, e);
        }
    }

    public Meeting create(Meeting meeting) {
        String sql = """
                INSERT INTO meetings(
                    title,
                    meeting_date,
                    start_time,
                    end_time,
                    location,
                    organizer,
                    responsible_member_id,
                    agenda,
                    notes,
                    status,
                    category,
                    linked_documents,
                    is_archived
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMeeting(statement, meeting);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1))
                            .orElseThrow(() -> new IllegalStateException("Reunion creee mais introuvable."));
                }
            }
            throw new IllegalStateException("Creation reunion impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer une reunion.", e);
        }
    }

    public Meeting update(Meeting meeting) {
        String sql = """
                UPDATE meetings
                SET title = ?,
                    meeting_date = ?,
                    start_time = ?,
                    end_time = ?,
                    location = ?,
                    organizer = ?,
                    responsible_member_id = ?,
                    agenda = ?,
                    notes = ?,
                    status = ?,
                    category = ?,
                    linked_documents = ?,
                    is_archived = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMeeting(statement, meeting);
            statement.setLong(14, meeting.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("La reunion n'existe plus.");
            }
            return findById(meeting.id()).orElse(meeting);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour la reunion " + meeting.id(), e);
        }
    }

    public boolean deleteById(long meetingId) {
        String sql = "DELETE FROM meetings WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, meetingId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la reunion " + meetingId, e);
        }
    }

    public long countAll() {
        return countWithCondition(null, null);
    }

    public long countUpcoming(LocalDate fromDate) {
        return countWithCondition("meeting_date >= ? AND is_archived = 0", fromDate.toString());
    }

    public List<Meeting> findUpcoming(LocalDate fromDate, int limit) {
        String sql = BASE_SELECT + """
                WHERE m.meeting_date >= ?
                  AND m.is_archived = 0
                ORDER BY m.meeting_date ASC, m.start_time ASC, m.id ASC
                LIMIT ?
                """;
        List<Meeting> meetings = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromDate.toString());
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    meetings.add(mapRow(rs));
                }
            }
            return meetings;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les prochaines reunions.", e);
        }
    }

    public boolean setArchived(long meetingId, boolean archived) {
        String sql = "UPDATE meetings SET is_archived = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, archived ? 1 : 0);
            statement.setLong(2, meetingId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour l'archive reunion " + meetingId, e);
        }
    }

    private long countWithCondition(String condition, String value) {
        String sql = "SELECT COUNT(*) FROM meetings" + (condition == null ? "" : " WHERE " + condition);
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
            throw new IllegalStateException("Impossible de compter les reunions.", e);
        }
    }

    private void bindMeeting(PreparedStatement statement, Meeting meeting) throws SQLException {
        statement.setString(1, meeting.title());
        statement.setString(2, meeting.meetingDate().toString());
        statement.setString(3, meeting.startTime().toString());
        statement.setString(4, meeting.endTime().toString());
        statement.setString(5, meeting.location());
        statement.setString(6, meeting.organizer());
        if (meeting.responsibleMemberId() == null) {
            statement.setNull(7, java.sql.Types.BIGINT);
        } else {
            statement.setLong(7, meeting.responsibleMemberId());
        }
        statement.setString(8, meeting.agenda());
        statement.setString(9, meeting.notes());
        statement.setString(10, (meeting.status() == null ? ScheduleStatus.PLANNED : meeting.status()).name());
        statement.setString(11, meeting.category());
        statement.setString(12, meeting.linkedDocuments());
        statement.setInt(13, meeting.archived() ? 1 : 0);
    }

    private void applyArchiveFilter(StringBuilder sql, ArchiveFilter archiveFilter) {
        ArchiveFilter safeFilter = archiveFilter == null ? ArchiveFilter.ACTIVE : archiveFilter;
        switch (safeFilter) {
            case ACTIVE -> sql.append(" AND m.is_archived = 0 ");
            case ARCHIVED -> sql.append(" AND m.is_archived = 1 ");
            case ALL -> {
                // no filter
            }
        }
    }

    private String buildArchiveSqlCondition(ArchiveFilter archiveFilter) {
        ArchiveFilter safeFilter = archiveFilter == null ? ArchiveFilter.ACTIVE : archiveFilter;
        return switch (safeFilter) {
            case ACTIVE -> "m.is_archived = 0";
            case ARCHIVED -> "m.is_archived = 1";
            case ALL -> "1 = 1";
        };
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Meeting mapRow(ResultSet rs) throws SQLException {
        return new Meeting(
                rs.getLong("id"),
                rs.getString("title"),
                LocalDate.parse(rs.getString("meeting_date")),
                LocalTime.parse(rs.getString("start_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getString("location"),
                rs.getString("organizer"),
                rs.getObject("responsible_member_id") == null ? null : rs.getLong("responsible_member_id"),
                rs.getString("responsible_name"),
                rs.getString("agenda"),
                rs.getString("notes"),
                ScheduleStatus.fromDatabase(rs.getString("status")),
                rs.getString("category"),
                rs.getString("linked_documents"),
                rs.getInt("is_archived") == 1,
                rs.getInt("participant_count")
        );
    }
}
