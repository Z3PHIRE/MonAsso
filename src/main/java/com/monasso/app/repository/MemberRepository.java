package com.monasso.app.repository;

import com.monasso.app.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberRepository {

    private final DatabaseManager databaseManager;

    public MemberRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Member> findByCriteria(String query, Boolean activeFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, first_name, last_name, email, phone, address, join_date, is_active, notes
                FROM members
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();

        if (activeFilter != null) {
            sql.append(" AND is_active = ?");
            parameters.add(activeFilter ? 1 : 0);
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(first_name) LIKE ?
                        OR LOWER(last_name) LIKE ?
                        OR LOWER(COALESCE(email, '')) LIKE ?
                        OR LOWER(COALESCE(phone, '')) LIKE ?
                        OR LOWER(COALESCE(address, '')) LIKE ?
                     )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY last_name ASC, first_name ASC, id ASC");

        List<Member> members = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
            return members;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les membres.", e);
        }
    }

    public Optional<Member> findById(long memberId) {
        String sql = """
                SELECT id, first_name, last_name, email, phone, address, join_date, is_active, notes
                FROM members
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le membre " + memberId, e);
        }
    }

    public Member create(Member member) {
        String sql = """
                INSERT INTO members(first_name, last_name, email, phone, address, join_date, is_active, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMember(statement, member);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Member(
                            keys.getLong(1),
                            member.firstName(),
                            member.lastName(),
                            member.email(),
                            member.phone(),
                            member.address(),
                            member.joinDate(),
                            member.active(),
                            member.notes()
                    );
                }
            }
            throw new IllegalStateException("Creation membre impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer un membre.", e);
        }
    }

    public Member update(Member member) {
        String sql = """
                UPDATE members
                SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?, join_date = ?, is_active = ?, notes = ?
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMember(statement, member);
            statement.setLong(9, member.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Le membre n'existe plus.");
            }
            return member;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour le membre " + member.id(), e);
        }
    }

    public boolean deleteById(long memberId) {
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le membre " + memberId, e);
        }
    }

    public long countAll() {
        return countWithCondition(null);
    }

    public long countActive() {
        return countWithCondition("is_active = 1");
    }

    private long countWithCondition(String condition) {
        String sql = "SELECT COUNT(*) FROM members" + (condition == null ? "" : " WHERE " + condition);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les membres.", e);
        }
    }

    private void bindMember(PreparedStatement statement, Member member) throws SQLException {
        statement.setString(1, member.firstName());
        statement.setString(2, member.lastName());
        statement.setString(3, member.email());
        statement.setString(4, member.phone());
        statement.setString(5, member.address());
        statement.setString(6, member.joinDate().toString());
        statement.setInt(7, member.active() ? 1 : 0);
        statement.setString(8, member.notes());
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
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
