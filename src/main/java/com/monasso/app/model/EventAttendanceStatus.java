package com.monasso.app.model;

import java.util.Locale;

public enum EventAttendanceStatus {
    REGISTERED("Inscrit"),
    PRESENT("Present"),
    ABSENT("Absent");

    private final String label;

    EventAttendanceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EventAttendanceStatus fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return REGISTERED;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (EventAttendanceStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return REGISTERED;
    }

    @Override
    public String toString() {
        return label;
    }
}
