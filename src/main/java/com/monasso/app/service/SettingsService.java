package com.monasso.app.service;

import com.monasso.app.model.AppSetting;
import com.monasso.app.repository.AppSettingsRepository;

import java.util.List;
import java.util.Optional;

public class SettingsService {

    private final AppSettingsRepository appSettingsRepository;

    public SettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public List<AppSetting> getAllSettings() {
        return appSettingsRepository.findAll();
    }

    public Optional<String> getValue(String key) {
        return appSettingsRepository.findValueByKey(key);
    }

    public void saveSetting(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La cle de parametre est obligatoire.");
        }
        if (value == null) {
            throw new IllegalArgumentException("La valeur ne peut pas etre nulle.");
        }
        appSettingsRepository.save(key.trim(), value);
    }
}
