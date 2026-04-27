package com.monasso.app.model;

public enum GlobalSearchType {
    PERSON("Personne"),
    EVENT("Evenement"),
    MEETING("Reunion"),
    TASK("Tache");

    private final String label;

    GlobalSearchType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
