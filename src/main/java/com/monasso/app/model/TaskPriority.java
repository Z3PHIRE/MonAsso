package com.monasso.app.model;

import java.util.Locale;

public enum TaskPriority {
    LOW("Basse"),
    MEDIUM("Moyenne"),
    HIGH("Haute"),
    CRITICAL("Critique");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TaskPriority fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (TaskPriority priority : values()) {
            if (priority.name().equals(normalized)) {
                return priority;
            }
        }
        return MEDIUM;
    }

    @Override
    public String toString() {
        return label;
    }
}
