package com.monasso.app.model;

import java.util.Locale;

public enum DocumentTargetType {
    PERSON("Personne"),
    EVENT("Evenement"),
    MEETING("Reunion"),
    TASK("Tache");

    private final String label;

    DocumentTargetType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static DocumentTargetType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return PERSON;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DocumentTargetType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return PERSON;
    }

    @Override
    public String toString() {
        return label;
    }
}
