package com.monasso.app.repository;

import com.monasso.app.model.AppSetting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppSettingsRepository {

    private final DatabaseManager databaseManager;

    public AppSettingsRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<AppSetting> findAll() {
        String sql = """
                SELECT setting_key, setting_value, updated_at
                FROM app_settings
                ORDER BY setting_key
                """;
        List<AppSetting> settings = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                settings.add(new AppSetting(
                        rs.getString("setting_key"),
                        rs.getString("setting_value"),
                        rs.getString("updated_at")
                ));
            }
            return settings;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les parametres applicatifs.", e);
        }
    }

    public Optional<String> findValueByKey(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le parametre " + key, e);
        }
    }

    public void save(String key, String value) {
        String sql = """
                INSERT INTO app_settings(setting_key, setting_value, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(setting_key) DO UPDATE SET
                    setting_value = excluded.setting_value,
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'enregistrer le parametre " + key, e);
        }
    }
}
