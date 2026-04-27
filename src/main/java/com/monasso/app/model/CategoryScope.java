package com.monasso.app.model;

import java.util.Locale;

public enum CategoryScope {
    PERSON("Personne"),
    EVENT("Evenement"),
    MEETING("Reunion"),
    TASK("Tache");

    private final String label;

    CategoryScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CategoryScope fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return PERSON;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (CategoryScope scope : values()) {
            if (scope.name().equals(normalized)) {
                return scope;
            }
        }
        return PERSON;
    }

    @Override
    public String toString() {
        return label;
    }
}
