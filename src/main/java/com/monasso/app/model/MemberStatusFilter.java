package com.monasso.app.model;

public enum MemberStatusFilter {
    ALL("Tous"),
    ACTIVE("Actifs"),
    INACTIVE("Inactifs");

    private final String label;

    MemberStatusFilter(String label) {
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
