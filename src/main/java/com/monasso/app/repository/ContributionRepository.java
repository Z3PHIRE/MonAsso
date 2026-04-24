package com.monasso.app.repository;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.ContributionStatus;

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

    public List<Contribution> findByCriteria(String query, String periodLabel, ContributionStatus status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.id,
                    c.member_id,
                    (m.first_name || ' ' || m.last_name) AS member_name,
                    c.amount,
                    c.contribution_date,
                    c.period_label,
                    c.status,
                    c.payment_method,
                    c.notes
                FROM contributions c
                JOIN members m ON m.id = c.member_id
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();

        if (periodLabel != null && !periodLabel.isBlank()) {
            sql.append(" AND c.period_label = ?");
            parameters.add(periodLabel.trim());
        }
        if (status != null) {
            sql.append(" AND c.status = ?");
            parameters.add(status.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(m.first_name) LIKE ?
                        OR LOWER(m.last_name) LIKE ?
                        OR LOWER(COALESCE(c.notes, '')) LIKE ?
                        OR LOWER(COALESCE(c.period_label, '')) LIKE ?
                     )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY c.contribution_date DESC, c.id DESC");

        List<Contribution> contributions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    contributions.add(mapRow(rs));
                }
            }
            return contributions;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les cotisations.", e);
        }
    }

    public List<Contribution> findByMemberId(long memberId) {
        String sql = """
                SELECT
                    c.id,
                    c.member_id,
                    (m.first_name || ' ' || m.last_name) AS member_name,
                    c.amount,
                    c.contribution_date,
                    c.period_label,
                    c.status,
                    c.payment_method,
                    c.notes
                FROM contributions c
                JOIN members m ON m.id = c.member_id
                WHERE c.member_id = ?
                ORDER BY c.contribution_date DESC, c.id DESC
                """;
        List<Contribution> history = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    history.add(mapRow(rs));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger l'historique des cotisations.", e);
        }
    }

    public Contribution create(Contribution contribution) {
        String sql = """
                INSERT INTO contributions(member_id, amount, contribution_date, period_label, status, payment_method, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindContribution(statement, contribution);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Contribution(
                            keys.getLong(1),
                            contribution.memberId(),
                            contribution.memberName(),
                            contribution.amount(),
                            contribution.contributionDate(),
                            contribution.periodLabel(),
                            contribution.status(),
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

    public long countAll() {
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

    public long countPaidMembersForPeriod(String periodLabel) {
        String sql = """
                SELECT COUNT(DISTINCT member_id)
                FROM contributions
                WHERE period_label = ?
                  AND status = 'PAID'
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, periodLabel);
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

    public double totalAmountForPeriod(String periodLabel) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM contributions
                WHERE period_label = ?
                  AND status IN ('PAID', 'PARTIAL')
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, periodLabel);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
                return 0D;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de calculer le total des cotisations.", e);
        }
    }

    private void bindContribution(PreparedStatement statement, Contribution contribution) throws SQLException {
        statement.setLong(1, contribution.memberId());
        statement.setDouble(2, contribution.amount());
        statement.setString(3, contribution.contributionDate().toString());
        statement.setString(4, contribution.periodLabel());
        statement.setString(5, contribution.status().name());
        statement.setString(6, contribution.paymentMethod());
        statement.setString(7, contribution.notes());
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private Contribution mapRow(ResultSet rs) throws SQLException {
        return new Contribution(
                rs.getLong("id"),
                rs.getLong("member_id"),
                rs.getString("member_name"),
                rs.getDouble("amount"),
                LocalDate.parse(rs.getString("contribution_date")),
                rs.getString("period_label"),
                ContributionStatus.fromDatabase(rs.getString("status")),
                rs.getString("payment_method"),
                rs.getString("notes")
        );
    }
}
