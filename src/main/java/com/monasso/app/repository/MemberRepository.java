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

public class MemberRepository {

    private final DatabaseManager databaseManager;

    public MemberRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Member> findAll() {
        String sql = """
                SELECT id, first_name, last_name, email, phone, join_date
                FROM members
                ORDER BY id DESC
                """;
        List<Member> members = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
            return members;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les membres.", e);
        }
    }

    public Member create(Member member) {
        String sql = """
                INSERT INTO members(first_name, last_name, email, phone, join_date)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, member.firstName());
            statement.setString(2, member.lastName());
            statement.setString(3, member.email());
            statement.setString(4, member.phone());
            statement.setString(5, member.joinDate().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Member(
                            keys.getLong(1),
                            member.firstName(),
                            member.lastName(),
                            member.email(),
                            member.phone(),
                            member.joinDate()
                    );
                }
            }
            throw new IllegalStateException("Creation membre impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer un membre.", e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM members";
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

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                LocalDate.parse(rs.getString("join_date"))
        );
    }
}
