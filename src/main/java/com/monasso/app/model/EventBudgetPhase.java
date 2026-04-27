package com.monasso.app.model;

import java.util.Locale;

public enum EventBudgetPhase {
    PLANNED("Previsionnel"),
    ACTUAL("Reel");

    private final String label;

    EventBudgetPhase(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EventBudgetPhase fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return PLANNED;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (EventBudgetPhase phase : values()) {
            if (phase.name().equals(normalized)) {
                return phase;
            }
        }
        return PLANNED;
    }

    @Override
    public String toString() {
        return label;
    }
}
