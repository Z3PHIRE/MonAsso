package com.monasso.app.model;

public enum CalendarEntryType {
    EVENT("Evenement"),
    MEETING("Reunion");

    private final String label;

    CalendarEntryType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
