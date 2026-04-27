package com.monasso.app.repository;

import com.monasso.app.model.AppDocument;
import com.monasso.app.model.DocumentTargetType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentRepository {

    private static final String BASE_SELECT = """
            SELECT
                d.id,
                d.target_type,
                d.target_id,
                CASE d.target_type
                    WHEN 'PERSON' THEN COALESCE((SELECT m.first_name || ' ' || m.last_name FROM members m WHERE m.id = d.target_id), 'Personne #' || d.target_id)
                    WHEN 'EVENT' THEN COALESCE((SELECT e.title FROM events e WHERE e.id = d.target_id), 'Evenement #' || d.target_id)
                    WHEN 'MEETING' THEN COALESCE((SELECT me.title FROM meetings me WHERE me.id = d.target_id), 'Reunion #' || d.target_id)
                    WHEN 'TASK' THEN COALESCE((SELECT t.title FROM tasks t WHERE t.id = d.target_id), 'Tache #' || d.target_id)
                    ELSE 'Cible #' || d.target_id
                END AS target_label,
                d.file_path,
                d.file_name,
                d.file_type,
                d.notes,
                d.added_at
            FROM documents d
            """;

    private final DatabaseManager databaseManager;

    public DocumentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<AppDocument> findByCriteria(DocumentTargetType targetType, String query) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1 ");
        List<Object> parameters = new ArrayList<>();

        if (targetType != null) {
            sql.append(" AND d.target_type = ? ");
            parameters.add(targetType.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append("""
                    AND (
                        LOWER(COALESCE(d.file_name, '')) LIKE ?
                        OR LOWER(COALESCE(d.file_path, '')) LIKE ?
                        OR LOWER(COALESCE(d.file_type, '')) LIKE ?
                        OR LOWER(COALESCE(d.notes, '')) LIKE ?
                    )
                    """);
            String like = "%" + query.trim().toLowerCase() + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        sql.append(" ORDER BY d.added_at DESC, d.id DESC");

        List<AppDocument> documents = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapRow(rs));
                }
            }
            return documents;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les documents.", e);
        }
    }

    public Optional<AppDocument> findById(long documentId) {
        String sql = BASE_SELECT + " WHERE d.id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, documentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le document " + documentId, e);
        }
    }

    public AppDocument create(AppDocument document) {
        String sql = """
                INSERT INTO documents(
                    target_type,
                    target_id,
                    file_path,
                    file_name,
                    file_type,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, document.targetType().name());
            statement.setLong(2, document.targetId());
            statement.setString(3, document.filePath());
            statement.setString(4, document.fileName());
            statement.setString(5, document.fileType());
            statement.setString(6, document.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1))
                            .orElseThrow(() -> new IllegalStateException("Document cree mais introuvable."));
                }
            }
            throw new IllegalStateException("Creation document impossible: identifiant non retourne.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer un document.", e);
        }
    }

    public boolean deleteById(long documentId) {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, documentId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le document " + documentId, e);
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private AppDocument mapRow(ResultSet rs) throws SQLException {
        return new AppDocument(
                rs.getLong("id"),
                DocumentTargetType.fromDatabase(rs.getString("target_type")),
                rs.getLong("target_id"),
                rs.getString("target_label"),
                rs.getString("file_path"),
                rs.getString("file_name"),
                rs.getString("file_type"),
                rs.getString("notes"),
                rs.getString("added_at")
        );
    }
}
