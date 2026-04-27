package com.monasso.app.model;

import java.util.Locale;

public enum TaskStatus {
    TODO("A faire"),
    IN_PROGRESS("En cours"),
    BLOCKED("Bloquee"),
    DONE("Terminee");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TaskStatus fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return TODO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (TaskStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return TODO;
    }

    @Override
    public String toString() {
        return label;
    }
}
