package com.monasso.app.model;

import java.util.Locale;

public enum CustomFieldType {
    CHECKBOX("Case a cocher"),
    SHORT_TEXT("Texte court"),
    NUMBER("Nombre"),
    DATE("Date"),
    LIST("Liste");

    private final String label;

    CustomFieldType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CustomFieldType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return SHORT_TEXT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (CustomFieldType fieldType : values()) {
            if (fieldType.name().equals(normalized)) {
                return fieldType;
            }
        }
        return SHORT_TEXT;
    }

    @Override
    public String toString() {
        return label;
    }
}
