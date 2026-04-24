package com.monasso.app.model;

public enum ContributionStatus {
    PAID("Paye"),
    PENDING("En attente"),
    PARTIAL("Partiel");

    private final String label;

    ContributionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ContributionStatus fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return PAID;
        }
        try {
            return ContributionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PAID;
        }
    }
}
