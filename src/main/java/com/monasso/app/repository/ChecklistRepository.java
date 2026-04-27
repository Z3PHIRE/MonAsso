package com.monasso.app.repository;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.ChecklistCategory;
import com.monasso.app.model.ChecklistItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChecklistRepository {

    private final DatabaseManager databaseManager;

    public ChecklistRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<ChecklistCategory> findCategories() {
        String sql = """
                SELECT id, name, is_active, sort_order
                FROM checklist_categories
                WHERE is_active = 1
                ORDER BY sort_order ASC, name ASC, id ASC
                """;
        List<ChecklistCategory> categories = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(new ChecklistCategory(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("sort_order")
                ));
            }
            return categories;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les categories de checklist.", e);
        }
    }

    public ChecklistCategory ensureCategory(String categoryName) {
        String normalized = categoryName == null ? "" : categoryName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Le nom de categorie de checklist est obligatoire.");
        }

        String selectSql = """
                SELECT id, name, is_active, sort_order
                FROM checklist_categories
                WHERE LOWER(name) = LOWER(?)
                LIMIT 1
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setString(1, normalized);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return new ChecklistCategory(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getInt("is_active") == 1,
                            rs.getInt("sort_order")
                    );
                }
            }

            int sortOrder = nextCategorySortOrder(connection);
            String insertSql = """
                    INSERT INTO checklist_categories(name, is_active, sort_order)
                    VALUES (?, 1, ?)
                    """;
            try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, normalized);
                insert.setInt(2, sortOrder);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new ChecklistCategory(keys.getLong(1), normalized, true, sortOrder);
                    }
                }
            }
            throw new IllegalStateException("Creation de categorie checklist impossible.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'assurer la categorie checklist \"" + normalized + "\".", e);
        }
    }

    public List<ChecklistItem> findItems(CategoryScope targetType, long targetId) {
        String sql = """
                SELECT
                    ci.id,
                    ci.target_type,
                    ci.target_id,
                    ci.category_id,
                    cc.name AS category_name,
                    ci.label,
                    ci.is_checked,
                    ci.sort_order
                FROM checklist_items ci
                LEFT JOIN checklist_categories cc ON cc.id = ci.category_id
                WHERE ci.target_type = ?
                  AND ci.target_id = ?
                ORDER BY ci.sort_order ASC, ci.id ASC
                """;
        List<ChecklistItem> items = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetType.name());
            statement.setLong(2, targetId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(new ChecklistItem(
                            rs.getLong("id"),
                            CategoryScope.fromDatabase(rs.getString("target_type")),
                            rs.getLong("target_id"),
                            rs.getObject("category_id") == null ? null : rs.getLong("category_id"),
                            rs.getString("category_name"),
                            rs.getString("label"),
                            rs.getInt("is_checked") == 1,
                            rs.getInt("sort_order")
                    ));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la checklist cible.", e);
        }
    }

    public ChecklistItem addItem(CategoryScope targetType, long targetId, long categoryId, String label) {
        String sql = """
                INSERT INTO checklist_items(target_type, target_id, category_id, label, is_checked, sort_order, updated_at)
                VALUES (?, ?, ?, ?, 0, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int sortOrder = nextItemSortOrder(connection, targetType, targetId);
            statement.setString(1, targetType.name());
            statement.setLong(2, targetId);
            statement.setLong(3, categoryId);
            statement.setString(4, label);
            statement.setInt(5, sortOrder);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Creation item checklist impossible: identifiant absent.");
                }
                long id = keys.getLong(1);
                return findItemById(connection, id).orElseThrow(() ->
                        new IllegalStateException("Item checklist cree mais introuvable.")
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter un item de checklist.", e);
        }
    }

    public void setChecked(long checklistItemId, boolean checked) {
        String sql = """
                UPDATE checklist_items
                SET is_checked = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, checked ? 1 : 0);
            statement.setLong(2, checklistItemId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("L'item checklist n'existe plus.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour l'etat de la checklist.", e);
        }
    }

    public boolean deleteItem(long checklistItemId) {
        String sql = "DELETE FROM checklist_items WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, checklistItemId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer l'item de checklist.", e);
        }
    }

    private java.util.Optional<ChecklistItem> findItemById(Connection connection, long checklistItemId) throws SQLException {
        String sql = """
                SELECT
                    ci.id,
                    ci.target_type,
                    ci.target_id,
                    ci.category_id,
                    cc.name AS category_name,
                    ci.label,
                    ci.is_checked,
                    ci.sort_order
                FROM checklist_items ci
                LEFT JOIN checklist_categories cc ON cc.id = ci.category_id
                WHERE ci.id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, checklistItemId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new ChecklistItem(
                        rs.getLong("id"),
                        CategoryScope.fromDatabase(rs.getString("target_type")),
                        rs.getLong("target_id"),
                        rs.getObject("category_id") == null ? null : rs.getLong("category_id"),
                        rs.getString("category_name"),
                        rs.getString("label"),
                        rs.getInt("is_checked") == 1,
                        rs.getInt("sort_order")
                ));
            }
        }
    }

    private int nextCategorySortOrder(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM checklist_categories");
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;
        }
    }

    private int nextItemSortOrder(Connection connection, CategoryScope targetType, long targetId) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(sort_order), 0) + 1
                FROM checklist_items
                WHERE target_type = ?
                  AND target_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetType.name());
            statement.setLong(2, targetId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 1;
            }
        }
    }
}
