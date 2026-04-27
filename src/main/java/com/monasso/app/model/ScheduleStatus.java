package com.monasso.app.model;

import java.util.Locale;

public enum ScheduleStatus {
    DRAFT("Brouillon"),
    PLANNED("Prevue"),
    CONFIRMED("Confirmee"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminee"),
    CANCELLED("Annulee");

    private final String label;

    ScheduleStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ScheduleStatus fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return CONFIRMED;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (ScheduleStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return CONFIRMED;
    }

    @Override
    public String toString() {
        return label;
    }
}
