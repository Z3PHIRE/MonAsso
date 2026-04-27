package com.monasso.app.model;

import java.util.Locale;

public enum EventBudgetLineType {
    EXPENSE("Depense"),
    REVENUE("Recette");

    private final String label;

    EventBudgetLineType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EventBudgetLineType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return EXPENSE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (EventBudgetLineType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return EXPENSE;
    }

    @Override
    public String toString() {
        return label;
    }
}
