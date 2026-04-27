package com.monasso.app.model;

public enum ArchiveFilter {
    ACTIVE("Actifs"),
    ARCHIVED("Archives"),
    ALL("Tous");

    private final String label;

    ArchiveFilter(String label) {
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
