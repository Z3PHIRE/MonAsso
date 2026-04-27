package com.monasso.app.model;

import java.util.Locale;

public enum PersonType {
    MEMBER("Membre"),
    VOLUNTEER("Benevole"),
    EMPLOYEE("Salarie"),
    SPEAKER("Intervenant"),
    PARTNER("Partenaire");

    private final String label;

    PersonType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PersonType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return MEMBER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (PersonType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return MEMBER;
    }

    @Override
    public String toString() {
        return label;
    }
}
