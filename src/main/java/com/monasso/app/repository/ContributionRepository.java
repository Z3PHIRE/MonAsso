package com.monasso.app.repository;

import com.monasso.app.model.Contribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ContributionRepository {

    private final DatabaseManager databaseManager;

    public ContributionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Contribution> findAll() {
        String sql = """
                SELECT c.id, c.member_id, (m.first_name || ' ' || m.last_name) AS member_name, c.amount, c.contribution_date, c.payment_method, c.notes
                FROM contributions c
                JOIN members m ON m.id = c.member_id
                ORDER BY c.contribution_date DESC, c.id DESC
                """;
        List<Contribution> contributions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                contributions.add(mapRow(rs));
            }
            return contributions;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les cotisations.", e);
        }
    }

    public Contribution create(Contribution contribution) {
        String sql = """
                INSERT INTO contributions(member_id, amount, contribution_date, payment_method, notes)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, contribution.memberId());
            statement.setDouble(2, contribution.amount());
            statement.setString(3, contribution.contributionDate().toString());
            statement.setString(4, contribution.paymentMethod());
            statement.setString(5, contribution.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Contribution(
                            keys.getLong(1),
                            contribution.memberId(),
                            contribution.memberName(),
                            contribution.amount(),
                            contribution.contributionDate(),
                            contribution.paymentMethod(),
                            contribution.notes()
                    );
                }
            }
            throw new IllegalStateException("Creation cotisation impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer une cotisation.", e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM contributions";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les cotisations.", e);
        }
    }

    public double totalAmount() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM contributions";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0D;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de calculer le total des cotisations.", e);
        }
    }

    public long countDistinctMembersForYear(int year) {
        String sql = """
                SELECT COUNT(DISTINCT member_id)
                FROM contributions
                WHERE strftime('%Y', contribution_date) = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(year));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de compter les cotisations payees.", e);
        }
    }

    public boolean deleteById(long contributionId) {
        String sql = "DELETE FROM contributions WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contributionId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la cotisation " + contributionId, e);
        }
    }

    private Contribution mapRow(ResultSet rs) throws SQLException {
        return new Contribution(
                rs.getLong("id"),
                rs.getLong("member_id"),
                rs.getString("member_name"),
                rs.getDouble("amount"),
                LocalDate.parse(rs.getString("contribution_date")),
                rs.getString("payment_method"),
                rs.getString("notes")
        );
    }
}
