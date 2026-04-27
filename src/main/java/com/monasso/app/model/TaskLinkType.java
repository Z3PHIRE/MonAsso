package com.monasso.app.model;

import java.util.Locale;

public enum TaskLinkType {
    NONE("Aucun lien"),
    EVENT("Evenement"),
    MEETING("Reunion");

    private final String label;

    TaskLinkType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TaskLinkType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (TaskLinkType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return NONE;
    }

    @Override
    public String toString() {
        return label;
    }
}
