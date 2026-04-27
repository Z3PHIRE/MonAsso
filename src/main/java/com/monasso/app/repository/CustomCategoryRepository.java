package com.monasso.app.repository;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomCategoryValue;
import com.monasso.app.model.CustomFieldType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomCategoryRepository {

    private final DatabaseManager databaseManager;

    public CustomCategoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<CustomCategory> findAll() {
        String sql = """
                SELECT
                    id,
                    name,
                    parent_id,
                    field_type,
                    list_options,
                    is_active,
                    is_system,
                    sort_order
                FROM custom_categories
                ORDER BY sort_order ASC, name ASC, id ASC
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MutableCategoryRow> rows = new ArrayList<>();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) {
                MutableCategoryRow row = new MutableCategoryRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        CustomFieldType.fromDatabase(rs.getString("field_type")),
                        rs.getString("list_options"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("is_system") == 1,
                        rs.getInt("sort_order")
                );
                rows.add(row);
                ids.add(row.id);
            }
            Map<Long, List<CategoryScope>> scopesByCategory = loadScopes(connection, ids);
            List<CustomCategory> categories = new ArrayList<>();
            for (MutableCategoryRow row : rows) {
                categories.add(new CustomCategory(
                        row.id,
                        row.name,
                        row.parentId,
                        row.fieldType,
                        row.listOptions,
                        row.active,
                        row.systemCategory,
                        row.sortOrder,
                        scopesByCategory.getOrDefault(row.id, List.of())
                ));
            }
            return categories;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les categories personnalisees.", e);
        }
    }

    public List<CustomCategory> findByScope(CategoryScope scope) {
        String sql = """
                SELECT
                    c.id,
                    c.name,
                    c.parent_id,
                    c.field_type,
                    c.list_options,
                    c.is_active,
                    c.is_system,
                    c.sort_order
                FROM custom_categories c
                JOIN custom_category_scopes s ON s.category_id = c.id
                WHERE s.scope_type = ?
                  AND c.is_active = 1
                ORDER BY c.sort_order ASC, c.name ASC, c.id ASC
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scope.name());
            List<MutableCategoryRow> rows = new ArrayList<>();
            List<Long> ids = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    MutableCategoryRow row = new MutableCategoryRow(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                            CustomFieldType.fromDatabase(rs.getString("field_type")),
                            rs.getString("list_options"),
                            rs.getInt("is_active") == 1,
                            rs.getInt("is_system") == 1,
                            rs.getInt("sort_order")
                    );
                    rows.add(row);
                    ids.add(row.id);
                }
            }
            Map<Long, List<CategoryScope>> scopesByCategory = loadScopes(connection, ids);
            List<CustomCategory> categories = new ArrayList<>();
            for (MutableCategoryRow row : rows) {
                categories.add(new CustomCategory(
                        row.id,
                        row.name,
                        row.parentId,
                        row.fieldType,
                        row.listOptions,
                        row.active,
                        row.systemCategory,
                        row.sortOrder,
                        scopesByCategory.getOrDefault(row.id, List.of())
                ));
            }
            return categories;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les categories pour la cible " + scope.name(), e);
        }
    }

    public Optional<CustomCategory> findById(long categoryId) {
        String sql = """
                SELECT
                    id,
                    name,
                    parent_id,
                    field_type,
                    list_options,
                    is_active,
                    is_system,
                    sort_order
                FROM custom_categories
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Map<Long, List<CategoryScope>> scopes = loadScopes(connection, List.of(categoryId));
                return Optional.of(new CustomCategory(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        CustomFieldType.fromDatabase(rs.getString("field_type")),
                        rs.getString("list_options"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("is_system") == 1,
                        rs.getInt("sort_order"),
                        scopes.getOrDefault(categoryId, List.of())
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la categorie " + categoryId, e);
        }
    }

    public CustomCategory create(
            String name,
            Long parentId,
            CustomFieldType fieldType,
            String listOptions,
            boolean active,
            int sortOrder,
            List<CategoryScope> scopes
    ) {
        String sql = """
                INSERT INTO custom_categories(
                    name,
                    parent_id,
                    field_type,
                    list_options,
                    is_active,
                    is_system,
                    sort_order,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, 0, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            if (parentId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, parentId);
            }
            statement.setString(3, fieldType.name());
            statement.setString(4, listOptions);
            statement.setInt(5, active ? 1 : 0);
            statement.setInt(6, sortOrder);
            statement.executeUpdate();
            long categoryId;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Creation categorie impossible: identifiant absent.");
                }
                categoryId = keys.getLong(1);
            }
            replaceScopes(connection, categoryId, scopes);
            return findById(categoryId).orElseThrow(() ->
                    new IllegalStateException("Categorie creee mais introuvable apres insertion.")
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer la categorie personnalisee.", e);
        }
    }

    public CustomCategory update(
            long categoryId,
            String name,
            Long parentId,
            CustomFieldType fieldType,
            String listOptions,
            boolean active,
            int sortOrder,
            List<CategoryScope> scopes
    ) {
        String sql = """
                UPDATE custom_categories
                SET name = ?,
                    parent_id = ?,
                    field_type = ?,
                    list_options = ?,
                    is_active = ?,
                    sort_order = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (parentId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, parentId);
            }
            statement.setString(3, fieldType.name());
            statement.setString(4, listOptions);
            statement.setInt(5, active ? 1 : 0);
            statement.setInt(6, sortOrder);
            statement.setLong(7, categoryId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("La categorie n'existe plus.");
            }
            replaceScopes(connection, categoryId, scopes);
            return findById(categoryId).orElseThrow(() ->
                    new IllegalStateException("Categorie mise a jour mais introuvable.")
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour la categorie " + categoryId, e);
        }
    }

    public List<CustomCategoryValue> findValues(CategoryScope scope, long targetId) {
        String sql = """
                SELECT
                    id,
                    category_id,
                    scope_type,
                    target_id,
                    value_type,
                    text_value,
                    number_value,
                    date_value,
                    bool_value
                FROM custom_category_values
                WHERE scope_type = ?
                  AND target_id = ?
                ORDER BY category_id ASC
                """;
        List<CustomCategoryValue> values = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scope.name());
            statement.setLong(2, targetId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.add(new CustomCategoryValue(
                            rs.getLong("id"),
                            rs.getLong("category_id"),
                            CategoryScope.fromDatabase(rs.getString("scope_type")),
                            rs.getLong("target_id"),
                            rs.getString("value_type"),
                            rs.getString("text_value"),
                            rs.getObject("number_value") == null ? null : rs.getDouble("number_value"),
                            rs.getString("date_value"),
                            rs.getObject("bool_value") == null ? null : rs.getInt("bool_value") == 1
                    ));
                }
            }
            return values;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les valeurs de categories personnalisees.", e);
        }
    }

    public void saveValue(
            long categoryId,
            CategoryScope scope,
            long targetId,
            String valueType,
            String textValue,
            Double numberValue,
            String dateValue,
            Boolean boolValue
    ) {
        String sql = """
                INSERT INTO custom_category_values(
                    category_id,
                    scope_type,
                    target_id,
                    value_type,
                    text_value,
                    number_value,
                    date_value,
                    bool_value,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(category_id, scope_type, target_id) DO UPDATE SET
                    value_type = excluded.value_type,
                    text_value = excluded.text_value,
                    number_value = excluded.number_value,
                    date_value = excluded.date_value,
                    bool_value = excluded.bool_value,
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setString(2, scope.name());
            statement.setLong(3, targetId);
            statement.setString(4, valueType);
            statement.setString(5, textValue);
            if (numberValue == null) {
                statement.setNull(6, java.sql.Types.DOUBLE);
            } else {
                statement.setDouble(6, numberValue);
            }
            statement.setString(7, dateValue);
            if (boolValue == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, boolValue ? 1 : 0);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'enregistrer la valeur personnalisee.", e);
        }
    }

    public void deleteValue(long categoryId, CategoryScope scope, long targetId) {
        String sql = """
                DELETE FROM custom_category_values
                WHERE category_id = ?
                  AND scope_type = ?
                  AND target_id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setString(2, scope.name());
            statement.setLong(3, targetId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer la valeur personnalisee.", e);
        }
    }

    private void replaceScopes(Connection connection, long categoryId, List<CategoryScope> scopes) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM custom_category_scopes WHERE category_id = ?")) {
            deleteStatement.setLong(1, categoryId);
            deleteStatement.executeUpdate();
        }
        if (scopes == null || scopes.isEmpty()) {
            return;
        }
        String insertSql = """
                INSERT INTO custom_category_scopes(category_id, scope_type)
                VALUES (?, ?)
                ON CONFLICT(category_id, scope_type) DO NOTHING
                """;
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            for (CategoryScope scope : scopes) {
                insertStatement.setLong(1, categoryId);
                insertStatement.setString(2, scope.name());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private Map<Long, List<CategoryScope>> loadScopes(Connection connection, List<Long> categoryIds) throws SQLException {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(categoryIds.size(), "?"));
        String sql = "SELECT category_id, scope_type FROM custom_category_scopes WHERE category_id IN (" + placeholders + ")";
        Map<Long, List<CategoryScope>> scopesByCategory = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < categoryIds.size(); i++) {
                statement.setLong(i + 1, categoryIds.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long categoryId = rs.getLong("category_id");
                    scopesByCategory
                            .computeIfAbsent(categoryId, ignored -> new ArrayList<>())
                            .add(CategoryScope.fromDatabase(rs.getString("scope_type")));
                }
            }
        }
        return scopesByCategory;
    }

    private static final class MutableCategoryRow {
        private final long id;
        private final String name;
        private final Long parentId;
        private final CustomFieldType fieldType;
        private final String listOptions;
        private final boolean active;
        private final boolean systemCategory;
        private final int sortOrder;

        private MutableCategoryRow(
                long id,
                String name,
                Long parentId,
                CustomFieldType fieldType,
                String listOptions,
                boolean active,
                boolean systemCategory,
                int sortOrder
        ) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.fieldType = fieldType;
            this.listOptions = listOptions;
            this.active = active;
            this.systemCategory = systemCategory;
            this.sortOrder = sortOrder;
        }
    }
}
